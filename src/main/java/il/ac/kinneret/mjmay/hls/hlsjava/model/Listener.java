package il.ac.kinneret.mjmay.hls.hlsjava.model;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Logger;

import static il.ac.kinneret.mjmay.hls.hlsjava.model.ClientActions.logger;

/**
 * Thread for listening for incoming conversations
 * @author Michael J. May
 * @version 1.0
 */
public class Listener extends Thread{

    ServerSocket serverSocket;
    Logger logger;
    /**
     * Quick function for logging string into the FileLog.Log file
     *
     * @param msg String to be logged
     */
    public static void log (String msg){
        try{
            LoggerFile.getInstance().info(msg);
        }catch (Exception e){

        }
    }

    /**
     * Builds the listening thread
     * @param socket The server socket to listen on
     */
    public Listener (ServerSocket socket)
    {
        serverSocket = socket;
        logger = Logger.getLogger(Listener.class.getName());

    }

    /**
     * Runs the listening logic.
     */
    @Override
    public void run() {
        while (!this.isInterrupted() && serverSocket != null && !serverSocket.isClosed())
        {
            try {
                // get an incoming conversation and handle it
                Socket incomingSocket = serverSocket.accept();
                logger.info("Received incoming connection from: " + incomingSocket.getRemoteSocketAddress().toString());
                log("Received incoming connection from: " + incomingSocket.getRemoteSocketAddress().toString());
                HandleClient handleClient = new HandleClient(incomingSocket);
                handleClient.start();
            }
            catch (IOException ex)
            {
                // something is wrong, let's leave
                logger.severe("Error listening: " + ex.getLocalizedMessage());
                log("Error listening: " + ex.getLocalizedMessage());
            }

        }
    }
}
