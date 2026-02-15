package com.cerca.service;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class ConfigService {

	private static final Path CONFIG_FILE = Paths.get("config.properties");
    private final LogService logger;
    private Properties properties;
   
    public ConfigService(LogService logger) {
        this.logger = logger;
        this.properties = new Properties();
       // this.configFile = initConfigFile();
        loadProperties();
    }

   
    
    
    
    private void loadProperties() {
        if (Files.exists(CONFIG_FILE)) {
            try (InputStream input = Files.newInputStream(CONFIG_FILE)) {
                properties.load(input);
                logger.log("INFO", "Settings loaded from config.properties");
            } catch (Exception ex) {
                logger.log("WARNING", "Failed to read config file: " + ex.getMessage());
            }
        } else {
            logger.log("INFO", "No config.properties found. A new one will be created when settings are saved.");
        }
    }

    // --- GENERIC GETTERS AND SETTERS ---

    public String getProperty(String key) {
        return properties.getProperty(key, "");
    }

    public void setProperty(String key, String value) {
        properties.setProperty(key, value.trim());
        saveProperties();
    }

    private void saveProperties() {
        try (OutputStream output = Files.newOutputStream(CONFIG_FILE)) {
            properties.store(output, "CERCA User Settings");
            // Note: I removed the logger from here to prevent spamming the log 
            // if you ever save multiple properties in a row, but you can add it back if preferred!
        } catch (Exception ex) {
            logger.log("ERROR", "Failed to save settings: " + ex.getMessage());
        }
    }
}