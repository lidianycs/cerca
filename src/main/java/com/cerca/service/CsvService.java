package com.cerca.service;

import com.cerca.model.ReferenceItem;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Export the data to a CSV file locally stored.
 *
 * @author Lidiany Cerqueira
 */
public class CsvService {

    public void exportToCsv(List<ReferenceItem> data, File file) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            
            
            writer.write("ID;Verified;Status;Match Score;PDF Title;PDF Authors;Crossref Title;Crossref Authors;DOI\n");

           
            for (ReferenceItem item : data) {
                StringBuilder line = new StringBuilder();
                
                line.append(escapeCsv(String.valueOf(item.idProperty().get()))).append(";");
                line.append(item.isVerified()).append(";");
                line.append(escapeCsv(item.statusProperty().get())).append(";");
                line.append(item.getMatchScore()).append(";");
                
                line.append(escapeCsv(item.pdfTitleProperty().get())).append(";");
                line.append(escapeCsv(item.authorsProperty().get())).append(";");
                
                line.append(escapeCsv(item.crossrefTitleProperty().get())).append(";");
                line.append(escapeCsv(item.dBAuthorsProperty().get())).append(";");
                
                line.append(escapeCsv(item.getDetectedDoi()));
                
                writer.write(line.toString());
                writer.newLine();
            }
        }
    }

    /** Helper to handle commas and quotes inside the data**/
    private String escapeCsv(String data) {
        if (data == null) return "";
        String escaped = data.replaceAll("\"", "\"\""); // Double up quotes
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            return "\"" + escaped + "\""; // Wrap in quotes
        }
        return escaped;
    }
}