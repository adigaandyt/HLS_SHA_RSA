package il.ac.kinneret.mjmay.hls.hlsjava.model;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

//Singleton class for logging
public class LoggerFile {
    private static LoggerFile loggerFile = null;
    public Logger logger;

    //Constructor for logger
    public LoggerFile() {
        logger = Logger.getLogger("MyLog");
        FileHandler fileHandler;
        System.setProperty("java.util.logging.SimpleFormatter.format", "[%1$tF %1$tT] %5$s %n");
        try{
            fileHandler = new FileHandler("./FileLog.log");
            logger.addHandler(fileHandler);
            SimpleFormatter formatter = new SimpleFormatter();
            fileHandler.setFormatter(formatter);
            logger.setUseParentHandlers(false);
            logger.info("Log start");
        } catch(Exception e){
            e.printStackTrace();
            System.out.println("Error while setting log file");
        }
    }
    //Singleton Class
    public static Logger getInstance() {
        if (loggerFile == null)
            loggerFile = new LoggerFile();
        return loggerFile.logger;
    }
}