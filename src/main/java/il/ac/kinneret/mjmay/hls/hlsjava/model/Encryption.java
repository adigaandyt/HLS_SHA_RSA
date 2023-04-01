package il.ac.kinneret.mjmay.hls.hlsjava.model;


import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.logging.Logger;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import static il.ac.kinneret.mjmay.hls.hlsjava.model.ClientActions.logger;

/**
 * Class to preform encryption and decryption on messages and files
 * @author Andy Thaok
 * @version 1.0
 */
public class Encryption {
    /**
     * GCM for files CBC for messages
     * MAC Encrypt-then-MAC, IV + DATA + TIMESTAMP + RSA + NAME
     * RSA sign-then-encrypt, TIMESTAMP + NAME
     */

    public static final String pcName = myName();
    public static final int TIME_LENGTH = 19;
    public static final int RSA_SIGN_LENGTH = 512;
    public static final int GCM_IV_LENGTH = 16;
    public static final int MAC_LEN = 32;
    public static final int MSG_IV_LEN = 16;
    public static SecretKey macKey;
    public static SecretKey secretKey;
    public static ArrayList<Character> pcNames;

    static {
        try {
            secretKey = getKey();
            macKey = getMacKey();
            pcNames = getNames();
        } catch (Exception e) {
            logger.severe("Error reading config "+ e.getMessage());
            log("Error reading config "+ e.getMessage() );
        }
    }

    public static ArrayList<Character> getNames(){
        try{
            String keyString = Files.readAllLines(Paths.get("Config.txt")).get(3);
            ArrayList<Character> pcNames = new ArrayList<Character>();
            for(int i=0;i<keyString.length();i++){
                pcNames.add(keyString.charAt(i));
            }
            return pcNames;
        }catch (Exception e){
            logger.severe("Error reading config file");
            log("Error reading config file");
        }
        return null;
    }

    /**
     * Gets the second line from Config.txt and turns it into a  AES-256 Key
     * @return Returns the MAC result
     */
    public static SecretKeySpec getMacKey ()  {
        try{
            int n = 1; // The line number
            String macKeyString = Files.readAllLines(Paths.get("Config.txt")).get(n);
            //Converting the password to bytes using the UTF8 encoding
            byte[] macKeyByte = macKeyString.getBytes(StandardCharsets.UTF_8);
            //Hashing the resulting bytes with SHA256 hash algorithm
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            //Use the 256 output bits as the MAC key for HMAC-SHA256.
            return new SecretKeySpec(digest.digest(macKeyByte), 0, digest.digest(macKeyByte).length, "AES");
        }catch (Exception e){
            logger.severe("Error while getting MAC key: "+ e.getMessage() );
            log("Error while getting MAC key: "+ e.getMessage() );
        }
        return null;
    }

    /**
     * Gets the first line from Config.txt and turns it into a  AES-256 Key
     *
     * @return Returns SecretKey created from the Config.txt
     */
    public static SecretKey getKey () {
        try{
            int n = 0; // The line number
            String keyString = Files.readAllLines(Paths.get("Config.txt")).get(n);
            log("Key String: " + keyString);
            byte[] ketByte = keyString.getBytes(StandardCharsets.UTF_8);            //Converting the password to bytes using the UTF8 encoding
            MessageDigest digest = MessageDigest.getInstance("SHA-256");            //Hashing the resulting bytes with SHA2-256 hash algorithm

            byte[] key = digest.digest(ketByte);
            int offset = 0;
            int len = key.length;
            String algorithm = "AES";

            return new SecretKeySpec(key, offset, len, algorithm);
        }catch (Exception e){
            logger.severe("Error while retrieving key: " + e.getMessage());
            log("Error while retrieving key: " + e.getMessage());
        }
        return null;
    }

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
     * Generate a random IV
     * @param len Length of the IV byte array
     * @return randomly generated IV as an IvParameterSpec
     */
    public static IvParameterSpec getIVSecureRandom (int len) {
        try{
            SecureRandom random = SecureRandom.getInstanceStrong();
            byte[] iv = new byte[len];
            random.nextBytes(iv);
            return new IvParameterSpec(iv);
        }catch (NoSuchAlgorithmException e){
            logger.severe("Error while generating IV: " + e.getMessage());
            log("Error while generating IV: " + e.getMessage());
        }
        return null;
    }

    /**
     * Encrypt file using AES-GCM and HMAC, create and save an .enc folder in the same directory as the input file
     * @param file The file to be encrypted
     */
    public static Boolean encrypt(File file) {
        logger.info("Attempting to encrypt file: " + file.getName());
        log("Attempting to encrypt file: " + file.getName());
        try{
            //Setting up the Cipher
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            IvParameterSpec iv = getIVSecureRandom(GCM_IV_LENGTH);
            byte[] ivBytes = iv.getIV();
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_IV_LENGTH*8, ivBytes);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec);

            //Get the data and create a [DATA|TIME|RSA|NAME] Byte array to encrypt
            byte[] data = Files.readAllBytes(file.toPath()); //[DATA]

            byte[] time = RsaFuncs.getTimeBytes(); //[TIME]
            //[data|time|name] for RSA signature
            byte[] toRSA = ArrayEditor.combine(data,time,pcName.getBytes(StandardCharsets.UTF_8));
            byte[] RSAsign = RsaFuncs.createRSASignature(toRSA); //RSA Signature

            //[DATA|TIME|RSA|NAME] => [Encrypted Data]
            byte[] nameByte = pcName.getBytes(StandardCharsets.UTF_8); //Name
            byte[] toEnc = ArrayEditor.combine(data,time,RSAsign,nameByte);
            byte[] encryptedData = cipher.doFinal(toEnc);

            //Get MAC of encrypted [IV|DATA|TIME|RSA|NAME]
            byte[] calcHmac = ArrayEditor.combine(iv.getIV(),encryptedData);
            byte[] macSign = computeHash(calcHmac);

            //Create output byte[] [IV | Encrypted Data | MAC]
            byte[] outputByte = ArrayEditor.combine(ivBytes,encryptedData,macSign);

            OutputStream os = new FileOutputStream(file.toPath() + ".enc");
            os.write(outputByte);
            os.close();

            log("IV During file encryption: " + bytesToHex(iv.getIV()));
            logger.info("IV During file encryption: " + bytesToHex(iv.getIV()));
            log("MAC During file encryption: " + bytesToHex(macSign));
            logger.info("MAC During file encryption: " + bytesToHex(macSign));
            log("RSA During file encryption: " + bytesToHex(RSAsign));
            logger.info("RSA During file encryption: " + bytesToHex(RSAsign));


            logger.info("Finished encrypting file: " + file);
            log("Finished encrypting file: " + file);

            return true;
        }catch (Exception e){
            logger.severe("Error during file encryption: " + e.getMessage());
            log("Error during file encryption: " + e.getMessage());
            return false;
        }
    }

    /**
     * Decrypt .enc file using AES-GCM encryption and HMAC and save it as the same directory as the input file
     * @param file .enc folder to be decrypted
     */
    public static File decrypt (File file) {
        logger.info("Attempting to decrypt file: " + file.getName());
        log("Attempting to decrypt file: " + file.getName());
        log("Arrived at: " + new String(RsaFuncs.getTimeBytes()));
        try{

            byte[] encryptedFileBytes = Files.readAllBytes(file.toPath());

            byte[] iv =  Arrays.copyOfRange(encryptedFileBytes,0,GCM_IV_LENGTH);
            byte[] enData = Arrays.copyOfRange(encryptedFileBytes,iv.length,encryptedFileBytes.length-MAC_LEN);
            byte[] macBytes =  Arrays.copyOfRange(encryptedFileBytes,encryptedFileBytes.length-MAC_LEN,encryptedFileBytes.length);
            byte[] toHmac = Arrays.copyOfRange(encryptedFileBytes,0,encryptedFileBytes.length-MAC_LEN);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_IV_LENGTH*8, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            //MAC Verification
            if(!verifyMac(toHmac,macBytes)) {
                logger.severe("File HMAC verifcation failed");
                log("File HMAC verifcation failed");
                return null;}

            //[ Data | TimeStamp | RSA| PC ]
            byte[] decryptedBytes = cipher.doFinal(enData);
            byte[] data = Arrays.copyOfRange(decryptedBytes,0,decryptedBytes.length-RSA_SIGN_LENGTH-TIME_LENGTH-1);
            byte[] timeByte = Arrays.copyOfRange(decryptedBytes,data.length,data.length+TIME_LENGTH);
            byte[] rsaSign = Arrays.copyOfRange(decryptedBytes,data.length+timeByte.length,decryptedBytes.length-1);
            byte[] pcName = Arrays.copyOfRange(decryptedBytes,decryptedBytes.length-1,decryptedBytes.length);


            log("File sender  ID: " + new String(pcName));
            logger.info("File sender ID: " + new String(pcName));
            log("Sender file Timestamp: " + new String(timeByte));
            logger.info("Sender file Timestamp: " + new String(timeByte));
            log("IV After file decryption: " + bytesToHex(iv));
            logger.info("IV After file decryption: " + bytesToHex(iv));
            log("MAC After file decryption: " + bytesToHex(macBytes));
            logger.info("MAC After file decryption: " + bytesToHex(macBytes));
            log("RSA of file received: " + bytesToHex(rsaSign));
            logger.info("RSA of file received: " + bytesToHex(rsaSign));

            //[data|time|name] for RSA signature
            byte[] toRSA = ArrayEditor.combine(data,timeByte,pcName);

            //RSA TIME checks
            if(!RsaFuncs.verifyRSASignature(toRSA,new String(pcName),rsaSign)){
                logger.severe("File RSA Signature is incorrect");
                log("File RSA Signature is incorrect");
                return null;}
            if(!RsaFuncs.onTime(timeByte)){
                logger.severe("File timestamp is too old");
                log("File timestamp is too old");
                return null;}
            //save file
            OutputStream os = new FileOutputStream(file.getAbsolutePath().substring(0,file.getAbsolutePath().length()-4));
            os.write(data);
            os.close();

            logger.info("Finished file decryption");
            log("Finished file decryption");

            return new File(file.getAbsolutePath().substring(0,file.getAbsolutePath().length()-4));
        }catch (Exception e){
            logger.severe("Error during file decryption: " + e.getMessage());
            log("Error during file decryption: " + e.getMessage());
        }
        return null;
    }

    /**
     * Encrypt messages using AES-CBC encryption and HMAC
     * @param msg The String to be encrypted
     * @return Return the encrypted message as String or ERROR if unsuccessful
     */
    public static String encrypt (String msg) {
        logger.info("Attempting to encrypt message: " + msg);
        log("Attempting to encrypt message: " + msg);
        logger.info("Arrived at: " + new String(RsaFuncs.getTimeBytes()));
        //log("Arrived at: " + new String(RsaFuncs.getTimeBytes())); Log has timestamps

        try {
            //Encryption
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            IvParameterSpec iv = getIVSecureRandom(MSG_IV_LEN);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);

            //command
            byte[] msgByte = msg.getBytes("UTF-8");
            byte[] time = RsaFuncs.getTimeBytes();
            byte[] nameByte = pcName.getBytes(StandardCharsets.UTF_8);
            //Command + Timestamp + Name to create RSA signature
            byte[] toRSA = ArrayEditor.combine(msgByte,time,nameByte);
            byte[] RSAsign = RsaFuncs.createRSASignature(toRSA);

            //data we want to encrypt before sending
            byte[] data = ArrayEditor.combine(msgByte,time,RSAsign,nameByte);
            //encrypted data we will send
            byte[] encrypted = cipher.doFinal(data);
            //IV+Encrypted data to create MAC
            byte[] toHmac = ArrayEditor.combine(iv.getIV(),encrypted);
            byte[] hmac = computeHash(toHmac);

            ByteArrayOutputStream finalOutput = new ByteArrayOutputStream();
            finalOutput.write(iv.getIV());
            finalOutput.write(encrypted);
            finalOutput.write(hmac);

            log("IV During message encryption: " + bytesToHex(iv.getIV()));
            logger.info("IV During message encryption: " + bytesToHex(iv.getIV()));
            log("MAC During message encryption: " + bytesToHex(hmac));
            logger.info("MAC During message encryption: " + bytesToHex(hmac));
            log("RSA During message encryption: " + bytesToHex(RSAsign));
            logger.info("RSA During message encryption: " + bytesToHex(RSAsign));

            logger.info("Finished encrypting");
            log("Finished encrypting");

            return Base64.getEncoder().encodeToString(finalOutput.toByteArray());
        }catch (Exception e){
            logger.severe("Error druing message encryption: "+ e.getMessage());
            log("Error druing message encryption: "+ e.getMessage());
        }
        return "ERROR";
    }

    /**
     * Decrypt messages using AES-CBC and verify HMAC
     * @param msg The String to be decrypted
     * @return Return the decrypted message as String or an ERROR if unsuccessful
     */
    public static String decrypt (String msg) {
        log("Attempting to decrypt message:  " + msg);
        logger.info("Attempting to decrypt message:  " + msg);
        try{
            //Splint input [IV | ENCRYPTED DATA | MAC] to [IV], [Encrpyted Data], [MAC]
            //[ENCRYPTED DATA] = Encrypted [Command|Time|RSA|Name]
            byte[] fullBytes = Base64.getDecoder().decode(msg);
            byte[] ivBytes = Arrays.copyOfRange(fullBytes, 0, 16); //first 16 byte = IV
            byte[] dataBytes = Arrays.copyOfRange(fullBytes, 16, fullBytes.length - MAC_LEN); // message bytes
            byte[] macBytes = Arrays.copyOfRange(fullBytes, fullBytes.length - MAC_LEN, fullBytes.length); //last 32 bytes = MAC
            //IV+Encrypted data for MAC verification
            byte[] toHmac = Arrays.copyOfRange(fullBytes, 0, fullBytes.length - MAC_LEN);

            //MAC check
            if(!verifyMac(toHmac,macBytes)){
                log("Message HMAC Verifcation failed");
                return "Message HMAC Verifcation failed";}

            IvParameterSpec iv = new IvParameterSpec(ivBytes);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            SecretKey secretKey = getKey();
            cipher.init(Cipher.DECRYPT_MODE, secretKey, iv);

            //Decrpyt the [Encrpyted Data] and split it into [Command] [Time] [RSA] [Name]
            byte[] data = cipher.doFinal(dataBytes);
            byte[] command = Arrays.copyOfRange(data,0,data.length-1-RSA_SIGN_LENGTH-TIME_LENGTH);
            byte[] timeByte = Arrays.copyOfRange(data,data.length-1-RSA_SIGN_LENGTH-TIME_LENGTH,data.length-1-RSA_SIGN_LENGTH);
            byte[] rsaSign = Arrays.copyOfRange(data,data.length-1-RSA_SIGN_LENGTH,data.length-1);
            byte[] nameByte = Arrays.copyOfRange(data,data.length-1,data.length);

            byte[] toRSA = ArrayEditor.combine(command,timeByte,nameByte);

            log("Message decrypted Command:  " + new String(command));
            logger.info("Message decryptedCommand:  " + new String(command));
            log("Message Sender ID: " + new String(nameByte));
            logger.info("Message Sender ID: " + new String(nameByte));
            log("Message Timestamp: " + new String(timeByte));
            logger.info("Message Timestamp: " + new String(timeByte));
            log("Message IV After decryption: " + bytesToHex(ivBytes));
            logger.info("Message IV After decryption: " + bytesToHex(ivBytes));
            log("Message MAC After decryption: " + bytesToHex(macBytes));
            logger.info("Message MAC After decryption: " + bytesToHex(macBytes));

            //RSA TIME checks
            if(!RsaFuncs.verifyRSASignature(toRSA,new String(nameByte),rsaSign)){
                log("Message RSA Signature is incorrect");
                return "Message RSA Signature is incorrect";}
            if(!RsaFuncs.onTime(timeByte)){
                log("Message timestamp is too old");
                return "Message timestamp is too old";}

            log("Finished message decryption ");
            logger.info("Finished message decryption");

            return new String(new String(command));
        }catch (Exception e){
            log("Error during message decryption: "+e.getMessage());
        }
        return "ERROR DURING MESSAGE DECRYPTION";
    }

    /**
     * Computes the MAC of a given byte array using the HmacSHA256 algorithm
     * @param data byte array to be computed
     * @return Returns the MAC result
     */
    public static byte[] computeHash ( byte[] data) {
        try{
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(macKey);
            return mac.doFinal(data);
        } catch (Exception e){
            log("Error while computing Hash: " + e.getMessage());
        }
        return null;
    }

    /**
     * Veirfy if the hash of the first array is the same as the MAC array
     * @param data byte array whose hash will be computed
     * @param mac byte array of the MAC to compare to
     * @return True if the hashes are equal, false otherwise
     */
    public static boolean verifyMac(byte[] data , byte[] mac){
        if(Arrays.equals(computeHash(data),mac)){
            return true;
        }else{return false;}
    }

    /**
     * Get PC name from config file line 3
     * @return PC Name
     */
    public static String myName(){
        try{
            String name = Files.readAllLines(Paths.get("Config.txt")).get(2);
            return name;
        }catch (Exception e){
            log("Error while getting name: " + e.getMessage());
        }
        return null;
    }

    /**
     * Turn a byte array into a HEX string
     * @return hex string
     * @param bytes the array to be converted to a hex string
     */
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

}
