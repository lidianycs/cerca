/**
 * @author Lidiany Cerqueira
 */
package com.cerca.utils;

import pl.edu.icm.cermine.bibref.CRFBibReferenceParser;
import pl.edu.icm.cermine.bibref.model.BibEntry;
import pl.edu.icm.cermine.bibref.model.BibEntryFieldType;
import pl.edu.icm.cermine.exception.AnalysisException;

import java.util.List;

public class ReferenceParser {

    private static CRFBibReferenceParser parser;

    static {
        try {
            parser = CRFBibReferenceParser.getInstance();
        } catch (AnalysisException e) {
            System.err.println("CERMINE Model Error: " + e.getMessage());
        }
    }

    public static class ParsedData {
        public String authors;
        public String title;
        
        public ParsedData(String authors, String title) {
            this.authors = authors;
            this.title = title;
        }
    }

    public static ParsedData parse(String rawReference) {
        if (rawReference == null || rawReference.trim().isEmpty()) {
            return new ParsedData("Unknown", "Unknown");
        }

        String cleanRef = rawReference.replaceAll("^[\\[\\(]?\\d+[\\]\\)]?[.,:]?\\s*", "").trim();
        String authors = null;
        String title = null;

        if (parser != null) {
            try {
                BibEntry bibEntry = parser.parseBibReference(cleanRef);
                
                // --- FIX: USE getAllFieldValues ---
                List<String> authorList = bibEntry.getAllFieldValues(BibEntryFieldType.AUTHOR);
                
                if (authorList != null && !authorList.isEmpty()) {
                    authors = String.join(", ", authorList);
                }

                title = bibEntry.getFirstFieldValue(BibEntryFieldType.TITLE);
            } catch (Exception e) {
                // Ignore, proceed to fallback
            }
        }        
        
        // Fallback for title
        if (title == null || title.isEmpty()) {
            title = cleanRef;
        }

        return new ParsedData(authors, clean(title));
    }

    
    private static String clean(String input) {
        if (input == null) return "";
        return input.replaceAll("[.,;]+$", "").trim();
    }
}