package il.ac.kinneret.mjmay.hls.hlsjava.model;

import il.ac.kinneret.mjmay.hls.hlsjava.model.LoggerFile;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import static il.ac.kinneret.mjmay.hls.hlsjava.model.ClientActions.logger;

/**
 * Class for RSA creation and verifaction and retriving keys from files
 * @author Andy Thaok
 * @version 1.0
 */
public class RsaFuncs {


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
     * Check to see if the given time is older than 5 seconds
     * @param byte[] the time we want to check
     * @return True or Flase based on the result
     */
    public static boolean onTime(byte[] timeBytes1){
        try{
            //Get our current time
            byte[] timeBytes2 = getTimeBytes();
            //Turn to strings
            String timeString1 = new String(timeBytes1);
            String timeString2 = new String(timeBytes2);
            //Get the HH:MM:SS Portion
            String HHMMSS1 = timeString1.substring(timeString1.length()-8,timeString1.length());
            String HHMMSS2 = timeString2.substring(timeString2.length()-8,timeString2.length());
            //Create date format for the comparison
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            Date d1 = sdf.parse(HHMMSS1);
            Date d2 = sdf.parse(HHMMSS2);
            //Check if it too longer than 5 seconds
            long elapsed = d2.getTime() - d1.getTime();
            if(elapsed > 5000){
                logger.info("Message is older than 5 seconds");
                log("Message is older than 5 seconds");
                return false;
            }else {
                logger.info("Message arrived on time");
                log("Message arrived on time");
                return true;
            }
        }catch (Exception e){
            logger.severe("Error while comparting message time: " + e.getMessage());
            log("Error while comparting message time: " + e.getMessage());
        }
        return true;
    }

    /**
     * Get the public key from rsaPublicKey file in the project directory
     * @return PublicKey object created from the file
     */
    public static PublicKey getPublicKey(){
        try{
            KeyFactory kf = KeyFactory.getInstance("RSA");
            byte[] publicKeyBytes = Files.readAllBytes(Paths.get("rsaPublicKey"));
            X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(publicKeyBytes);
            PublicKey pub = kf.generatePublic(pubKeySpec);

            return pub;
        }catch (Exception e){
            logger.severe("Error while retriving public key: " + e.getMessage());
            log("Error while retriving public key: " + e.getMessage());
        }
        return null;
    }

    /**
     * Get the public key of a specific PC using X_rsaPublicKey files
     * @param pcName the PC whose public key we want
     * @return PublicKey object created from the file
     */
    public static PublicKey getPublicKey(String pcName){
        try{
            KeyFactory kf = KeyFactory.getInstance("RSA");
            byte[] publicKeyBytes = Files.readAllBytes(Paths.get(".\\RSA_Keys\\"+pcName+"_rsaPublicKey"));
            X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(publicKeyBytes);
            PublicKey pub = kf.generatePublic(pubKeySpec);

            return pub;
        }catch (Exception e){
            logger.severe("Error while retriving public key: " + e.getMessage());
            log("Error while retriving public key: " + e.getMessage());
        }
        return null;
    }

    /**
     * Get the private key from rsaPrivateKey file in the project directory
     * @return PrivateKey object created from the file
     */
    public static PrivateKey getPrivateKey(){

        try{
            KeyFactory kf = KeyFactory.getInstance("RSA");
            byte[] privateKeyBytes = Files.readAllBytes(Paths.get(".\\RSA_Keys\\rsaPrivateKey"));
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(privateKeyBytes);
            PrivateKey priv = kf.generatePrivate(spec);

            return priv;
        }catch (Exception e ){
            logger.severe("Error while retreiving private key: " + e.getMessage());
            log("Error while retreiving private key: " + e.getMessage());
        }
        return null;
    }

    /**
     * Creates an SHA256withRSA signatrue for given byte[]
     * @param data the byte[] to create a signature for
     * @return byte[] of the signature
     */
    public static byte[] createRSASignature(byte[] data){

        try{
            Signature privateSignature = Signature.getInstance("SHA256withRSA");
            PrivateKey priv = getPrivateKey();
            //init signature and sign the message
            privateSignature.initSign(priv);
            privateSignature.update(data);
            //save it
            byte[] sign = privateSignature.sign();

            return sign;
        }catch (Exception e){
            logger.severe("Error while making an RSA signature: "+ e.getMessage());
            log("Error while making an RSA signature: "+ e.getMessage());
        }
        return null;
    }

    /**
     * Verify the RSA signature using public key and signed data
     * @param data Signed data that was used to create the signature
     * @param pcName The PC Name with the public key to use
     * @param signature The RSA to be verified
     * @return boolean True if verified, false if not
     */
    public static boolean verifyRSASignature(byte[] data, String pcName,byte[] signature){

        try{
            //Creating a Signature object
            Signature publicSignature = Signature.getInstance("SHA256withRSA");
            //Get public key using the PC name
            PublicKey pub = getPublicKey(pcName);
            //Initializing the signature
            publicSignature.initVerify(pub);
            //Update the data to be verified (This is the time stamp)
            publicSignature.update(data);
            //Verify the signature (This is the RSA signature that was attached)
            boolean isCorrect = publicSignature.verify(signature);

            return isCorrect;
        }catch (Exception e){
            logger.severe("Error during RSA Verifcation: " + e.getMessage());
            log("Error during RSA Verifcation: " + e.getMessage());
        }
        return false;
    }

    /**
     * Get current time in Israel formatted in ISO8601 date and time stamp as a byte[]
     * @return Byte[] containg current time
     */
    public static byte[] getTimeBytes(){
        String time = ZonedDateTime.now( ZoneId.of("Israel") ).truncatedTo(ChronoUnit.SECONDS).format( DateTimeFormatter.ISO_LOCAL_DATE_TIME );
        return time.getBytes(StandardCharsets.UTF_8);
    }
}
