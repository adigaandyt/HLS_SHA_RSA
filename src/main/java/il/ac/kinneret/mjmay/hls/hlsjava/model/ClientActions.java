package il.ac.kinneret.mjmay.hls.hlsjava.model;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Class to perform changes on the client state, primarily things that have to do with communication with the father or other nodes.
 * @author Michael J. May
 * @version 1.0
 */
public class ClientActions {

    static Logger logger =  Logger.getLogger(ClientActions.class.getName());
    /**
     * Quick function for logging string into the FileLog.Log file
     *
     * @param msg String to be logged
     */
    public static void log (String msg){
        try{
            LoggerFile.getInstance().info(msg);
        }catch (Exception e){
            logger.severe("Logging error");
        }
    }

    /**
     * Add a new file locally.  Also notifies the father if needed
     * @param fileName The file name to add locally
     * @return Whether the addition was successfully done (and sent to the father if needed)
     */
    public static boolean localAdd(String fileName)
    {
        // create an add message to send to the parent if this is not the root
        String command = Common.ADD_COMMAND + " " + Common.ipRemoveSlash(Common.localIp.toString()) + ":" + Common.localPort + " " + fileName ;
        try {
            System.out.println(command);
            Common.fatherMessages.put(command);
            logger.info("Queued add message: " + command);
            log("Queued add message: " + command);
            return true;
        } catch (InterruptedException e) {
            System.err.println("Error sending a message to the father for add: " + e.getMessage());
            // this shouldn't happen
            return false;
        }
    }



    /**
     * Remove a file that is found locally.  Also notifies the father if necessary
     * @param fileName The name of the file to remove
     * @return Whether the removal was succesfully done and sent to the father if necessary
     */
    public static boolean localRemove (String fileName)
    {
        // create a delete message to send to the parent if this is not the root
        String command = Common.DELETE_COMMAND + " " + Common.ipRemoveSlash(Common.localIp.toString()) + ":" + Common.localPort + " " + fileName ;
        try {
            Common.fatherMessages.put(command);
            logger.info("Queued remove message: " + command);
            log("Queued remove message: " + command);
            return true;
        } catch (InterruptedException e) {
            System.err.println("Error sending a message to the father for delete: " + e.getMessage());
            // this shouldn't happen
            return false;
        }
    }

    /**
     * Search for a file at the node's father (iteratively)
     * @param searchFileName The file name to search for
     * @param results The list of results that will be shown on the screen (results are added here)
     * @return True if the file was found.  False otherwise.
     */
    public static boolean searchFather (String searchFileName, ArrayList<String> results)
    {
        return iterateSearchSearch(Common.fatherIp, Common.fatherPort, searchFileName, results);
    }

    /**
     * Search for a file at a given IP address and port (iteratively).
     * @param searchAddress The IP address to ask.
     * @param searchPort The port of the node to ask
     * @param searchFileName The file to ask for
     * @param results The list of results that will be shown on the screen (results are added here)
     * @return True if the file was found.  False otherwise.
     */
    public static boolean iterateSearchSearch(InetAddress searchAddress, int searchPort, String searchFileName, ArrayList<String> results) {
        // prepare the query string
        String searchCommand = Common.LOOKUP_COMMAND + " " + searchFileName;
        String nodeResponse;
        nodeResponse = queryNode(searchCommand, searchAddress, searchPort);
        // something is wrong with the query, so quit it
        if (nodeResponse == null) return false;

        logger.info("Response from node " + searchAddress + ":" + searchPort + " was : " + nodeResponse);
        log("Response from node " + searchAddress + ":" + searchPort + " was : " + nodeResponse);

        // see what the node responded
        if (nodeResponse.equals(Common.NOTFOUND))
        {
            // it's not found, we're done
            logger.info("Father said file " + searchFileName + " isn't found.");
            log("Father said file " + searchFileName + " isn't found.");
            return false;
        }
        else {
            // see what came back here
            String[] responseParts = nodeResponse.split(";");
            boolean cumulativeResponse = false;
            for (String location : responseParts) {
                // see if this is local
                if (location.equals(Common.LOCAL)) {
                    results.add(Common.ipRemoveSlash(searchAddress.toString()) + ":" + searchPort);
                    cumulativeResponse = true;
                } else {
                    // go over each part of the response and recurse
                    String[] parts = location.split(":");
                    try {
                        if (iterateSearchSearch(InetAddress.getByName(Common.ipRemoveSlash(parts[0])), Integer.parseInt(parts[1]), searchFileName, results)) {
                            cumulativeResponse = true;
                        }
                    } catch (Exception ex) {
                        logger.severe("Error communicating with other node " + parts[0] + ":" + parts[1] + ": " + ex.getMessage());
                        log("Error communicating with other node " + parts[0] + ":" + parts[1] + ": " + ex.getMessage());
                    }
                }
            }
            return cumulativeResponse;
        }
    }

    /**
     * Queries a node for the file.  Used internally by the search methods
     * @param searchCommand The search command to send to the remote host
     * @param address The IP address of the remote host
     * @param port The port of the remote host
     * @return The result returned by the remote host
     */
    private static String queryNode(String searchCommand, InetAddress address, int port) {
        String nodeResponse;
        String encNodeResponse;
        try {
            Socket clientSocket = new Socket(address, port);
            // send the query
            PrintWriter pwOut = new PrintWriter(clientSocket.getOutputStream());
            BufferedReader brIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            pwOut.println(Encryption.encrypt(searchCommand));
            pwOut.flush();
            //get the response
            encNodeResponse = brIn.readLine();
            nodeResponse = Encryption.decrypt(encNodeResponse);
            pwOut.close();
            brIn.close();
            clientSocket.close();
        } catch (IOException e) {
            logger.severe("Can't contact node " + address.toString() + ":" + port + ": " + e.getMessage());
            log("Can't contact node " + address.toString() + ":" + port + ": " + e.getMessage());
            return null;
        }
        return nodeResponse;
    }

    /**
     * Retrives a file from a remote node.
     * @param address The IP address of the remote node
     * @param port The port of the remote node
     * @param fileName The file name to retrieve from the remote node
     * @param destinationDirectory The directory to store the downloaded file
     * @return True if the file was downloaded successfully.  False otherwise.
     */
    public static boolean retrieveFile(InetAddress address, int port, String fileName, File destinationDirectory) {
        // try to retrieve the file from the remote site
        try (Socket clientSocket = new Socket(address, port)){
            PrintWriter pwOut = new PrintWriter(clientSocket.getOutputStream());
            BufferedReader brIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String command = Common.RETRIEVE_COMMAND + " " + fileName;
            pwOut.println(Encryption.encrypt(command));
            pwOut.flush();
            logger.info("Sent " + command + " to remote node " + address + ":" + port);
            log("Sent " + command + " to remote node " + address + ":" + port);
            // get the response
            String enResponse = brIn.readLine();
            String response = Encryption.decrypt(enResponse);
            logger.info("Received message from remote node " + address.toString() + ":" + port + ": " + response);
            log("Received message from remote node " + address.toString() + ":" + port + ": " + response);
            if (response.equals(Common.LOCAL))
            {
                // file is coming
                byte[] buffer = new byte[4096];
                int read = 0;
                InputStream is = clientSocket.getInputStream();
                FileOutputStream fos = new FileOutputStream(new File(destinationDirectory, fileName +".enc"));
                while ((read = is.read(buffer)) >0)
                {
                    fos.write(buffer, 0, read);
                }
                // close up
                fos.close();

                //Decrypt the enc file we just got and delete it once the decryption is done and keep it if it fails
                File encryptedFile = new File(destinationDirectory + "\\" + fileName+".enc");
                File decryptedFile = Encryption.decrypt(encryptedFile);
                if(decryptedFile.exists()){encryptedFile.delete();}

                logger.info("Finished downloading file " + fileName + " from node " + address.toString() + ":" + port + " and stored at " + destinationDirectory.toString());
                log("Finished downloading file " + fileName + " from node " + address.toString() + ":" + port + " and stored at " + destinationDirectory.toString());

                return true;
            }
            else {
                // if it's not local, something is wrong
                logger.info("The remote node doesn't actually have the file " + response + " was received");
                log("The remote node doesn't actually have the file " + response + " was received");
                return false;
            }
        } catch (IOException iox)
        {
            // error reading the file
            logger.severe("Error reading or writing the file from the remote node: " + iox.getMessage());
            log("Error reading or writing the file from the remote node: " + iox.getMessage());
            return false;
        }


    }
}