package com.cerca.service;

import com.cerca.model.ReferenceItem;
import pl.edu.icm.cermine.ContentExtractor;
import pl.edu.icm.cermine.bibref.model.BibEntry;
import pl.edu.icm.cermine.bibref.model.BibEntryFieldType; // Important!

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class CermineService {

    public List<ReferenceItem> extractReferences(File pdfFile) throws Exception {
        List<ReferenceItem> results = new ArrayList<>();
        
        
        ContentExtractor extractor = new ContentExtractor();
        
        try (InputStream is = new FileInputStream(pdfFile)) {
            extractor.setPDF(is);
            
            
            List<BibEntry> references = extractor.getReferences();

            int idCounter = 1;
            for (BibEntry ref : references) {
               
                String raw = ref.getText();

                
                String title = ref.getFirstFieldValue(BibEntryFieldType.TITLE);
                if (title == null) title = "Unknown Title";

                
                String doi = ref.getFirstFieldValue(BibEntryFieldType.DOI);
                
                
                List<String> authorList = ref.getAllFieldValues(BibEntryFieldType.AUTHOR);
                String authors = (authorList != null && !authorList.isEmpty()) 
                                 ? String.join("; ", authorList) 
                                 : "Unknown Authors";

               
                results.add(new ReferenceItem(
                    idCounter++, 
                    "WAITING", 
                    authors, 
                    title, 
                    raw, 
                    doi
                ));
            }
        }
        return results;
    }
}