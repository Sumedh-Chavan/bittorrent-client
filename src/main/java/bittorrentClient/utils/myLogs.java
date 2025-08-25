package bittorrentClient.utils;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.*;

public class myLogs {
    private static Logger logger = Logger.getLogger(myLogs.class.getName());

    static {
        try {
            // Ensure "logs" directory exists
            java.nio.file.Path logDir = java.nio.file.Paths.get("logs");
            if (!java.nio.file.Files.exists(logDir)) {
                java.nio.file.Files.createDirectories(logDir);
            }

            String logFileName = "logs/run.log";

            // false = overwrite on each run
            FileHandler fileHandler = new FileHandler(logFileName, false);
            fileHandler.setFormatter(new CustomFormatter());

            logger.setUseParentHandlers(false); // disable console output
            logger.addHandler(fileHandler);
            logger.setLevel(Level.ALL); // allow all levels

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // Custom formatter: timestamp + level + message
    static class CustomFormatter extends Formatter {
        private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        @Override
        public String format(LogRecord record) {
            String time = sdf.format(new Date(record.getMillis()));
            String level = record.getLevel().getName();
            return String.format("%s [%s] %s%n", time, level, record.getMessage());
        }
    }

    // Convenience methods
    public static void info(String message) {
        logger.log(Level.INFO, message);
    }

    public static void warn(String message) {
        logger.log(Level.WARNING, message);
    }

    public static void error(String message) {
        logger.log(Level.SEVERE, message);
    }
}
