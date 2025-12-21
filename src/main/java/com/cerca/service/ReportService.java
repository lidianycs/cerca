package com.cerca.service;

import com.cerca.model.ReferenceItem;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReportService {

    public void exportReport(List<ReferenceItem> items, File file) throws IOException {
        StringBuilder sb = new StringBuilder();
        
        long total = items.size();
        long verified = items.stream().filter(i -> i.isVerified()).count();
        long suspicious = total - verified;
        
        
        sb.append("CERCA - INTEGRITY DIAGNOSTIC REPORT\n");
        sb.append("Generated: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))).append("\n");
        sb.append("File: ").append(file.getName()).append("\n");
        sb.append("* DISCLAIMER: This software is an experimental tool intended\n"
        		+ " to help verify bibliographic references, but is not 100% accurate. \n" +
	               "It does not replace manual verification. Always check the original source.\n");
        sb.append("==================================================\n\n");
        
        sb.append("SUMMARY\n");
        sb.append("-------\n");
        sb.append(String.format("Total References: %d\n", total));
        sb.append(String.format("✅ Verified:       %d\n", verified));
        sb.append(String.format("⚠️ Review Needed:  %d\n", suspicious));
        
        sb.append("\n==================================================\n");
        sb.append("DIAGNOSTICS: ITEMS REQUIRING ATTENTION\n");
        sb.append("==================================================\n\n");

        if (suspicious == 0) {
            sb.append("No issues detected. All references verified with high confidence.\n");
        } else {
            //int count = 1;
            for (ReferenceItem item : items) {
            	
                if (item.getMatchScore() < 80 && !item.isVerified()) {
                    sb.append(String.format("#%s\n", item.getId()));
                    
                    String diagnosis = getDiagnosis(item);
                    sb.append("🔴 DIAGNOSIS: ").append(diagnosis).append("\n");
                    sb.append("--------------------------------------------------\n");
                    
                    // 1. PDF DATA (What was found in the document)
                    sb.append("   PDF Title:   ").append(item.getPdfTitle()).append("\n");
                    sb.append("   PDF Authors: ").append(item.getAuthors()).append("\n"); 
                    
                    // 2. CROSSREF DATA (What was found in the database)
                    if (item.getDbTitle() != null && !item.getDbTitle().isEmpty()) {
                        sb.append("\n");
                        sb.append("   DB Title:    ").append(item.getDbTitle()).append("\n");
                        sb.append("   DB Authors:  ").append(item.getDbAuthors()).append("\n"); 
                        sb.append("   Similarity:  ").append(item.getMatchScore()).append("%\n");
                    }
                    
                    if (item.getDetectedDoi() != null && !item.getDetectedDoi().isEmpty()) {
                        sb.append("   PDF DOI:     ").append(item.getDetectedDoi()).append("\n");
                    }
                    
                    sb.append("\n");
                }
            }
        }
        
        sb.append("==================================================\n");
        sb.append("End of Report\n");
       

        Files.writeString(file.toPath(), sb.toString());
    }

    private String getDiagnosis(ReferenceItem item) {
        if (item.getDbTitle() == null || item.getDbTitle().isEmpty()) {
            return "NO MATCH FOUND. This paper does not appear in the Crossref/Zenodo databases.";
        }

        if (item.getDetectedDoi() != null && !item.getDetectedDoi().isEmpty()) {
            if (item.getMatchScore() < 40) {
                return "POTENTIAL DOI HIJACKING. The DOI provided points to a completely different paper.";
            }
        }

        String pdfAuth = item.getAuthors().toLowerCase();
        String dbAuth = item.getDbAuthors().toLowerCase();
        
        if (item.getMatchScore() > 60 && !pdfAuth.isEmpty() && !dbAuth.isEmpty()) {
            String firstAuthor = pdfAuth.split(" ")[0].replaceAll(",", "");
            if (!dbAuth.contains(firstAuthor)) {
                return "AUTHOR MISMATCH. Titles are similar, but the author lists do not match.";
            }
        }

        int diff = 100 - item.getMatchScore();
        if (item.getMatchScore() < 50) {
            return "SIGNIFICANT TITLE MISMATCH. Database record differs by " + diff + "%.";
        }

        return "LOW CONFIDENCE MATCH. Verify spelling or formatting.";
    }
}