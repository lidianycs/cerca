package com.cerca.service;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Save the logs of operations for audit purposes.
 *
 * @author Lidiany Cerqueira
 */
public class LogService {

    private static final Path LOG_FILE = Paths.get("cerca_audit.log");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public LogService() {
        
        log("SYSTEM", "=== New Session Started ===");
    }

    public void log(String category, String message) {
        String timestamp = LocalDateTime.now().format(TIME_FMT);
        
        String logLine = String.format("[%s] [%s] %s%n", timestamp, category, message);

        try {
            
            Files.writeString(LOG_FILE, logLine, 
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            
            
            System.out.print(logLine);
            
        } catch (IOException e) {
            System.err.println("Failed to write to log: " + e.getMessage());
        }
    }
}
