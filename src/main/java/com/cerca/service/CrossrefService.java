package com.cerca.service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import com.cerca.model.ReferenceItem;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javafx.scene.paint.Color;
import me.xdrop.fuzzywuzzy.FuzzySearch;

public class CrossrefService {
	
	private final LogService logger;
	private final HttpClient client;

	
	
	public CrossrefService(LogService logger) {
        this.logger = logger;
        this.client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build();
    }

	public boolean verifyItem(ReferenceItem item) {
		try {
			String jsonResponse = null;

			
			if (item.getDetectedDoi() != null && item.getDetectedDoi().contains("10.")) {
				jsonResponse = callApi("https://api.crossref.org/works/" + item.getDetectedDoi());
			}

			
			if (jsonResponse == null) {
				String query = item.getPdfTitle();
				if (query == null || query.equals("Unknown Title"))
					query = item.getRawText();
				if (query.length() > 200)
					query = query.substring(0, 200);

				String title = cleanText(item.getPdfTitle());
		        String author = cleanText(item.getAuthors());
				logger.log("API_REQ", String.format("ID %d | Querying: Title='%s' Author='%s'", 
		                item.getId(), title, author));
				String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
				jsonResponse = callApi("https://api.crossref.org/works?query.bibliographic=" + encoded + "&rows=1");
				
			}

			if (jsonResponse != null) {
				parseAndScore(jsonResponse, item);
				return true;
			} else {
				markNotFound(item);
				logger.log("API_RES", String.format("ID %d | No results returned from Crossref.", item.getId()));
				return false;
			}

		} catch (Exception e) {
			markNotFound(item);
			logger.log("ERROR", "API Request failed for ID " + item.getId() + ": " + e.getMessage());
			return false;
		}
	}

	private String callApi(String url) throws Exception {
		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url))
				.header("User-Agent", "Cerca/1.0 (mailto:cerca.app@gmail.com)").GET().build();
		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
		if (response.statusCode() == 200)
			return response.body();
		return null;
	}

	private void parseAndScore(String json, ReferenceItem item) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonObject message;

        
        if (root.has("message-type") && root.get("message-type").getAsString().equals("work")) {
            message = root.getAsJsonObject("message");
        } else {
            var items = root.getAsJsonObject("message").getAsJsonArray("items");
            if (items.size() == 0) { markNotFound(item); return; }
            message = items.get(0).getAsJsonObject();
        }

       
        String crTitle = "Unknown";
        if (message.has("title") && message.getAsJsonArray("title").size() > 0) {
            crTitle = message.getAsJsonArray("title").get(0).getAsString();
        }
        item.crossrefTitleProperty().set(crTitle);

        String crAuthors = "";
        if (message.has("author")) {
            var authorArray = message.getAsJsonArray("author");
            StringBuilder sb = new StringBuilder();
            for (var a : authorArray) {
                JsonObject authObj = a.getAsJsonObject();
                String family = authObj.has("family") ? authObj.get("family").getAsString() : "";
                String given = authObj.has("given") ? authObj.get("given").getAsString() : "";
                if (sb.length() > 0) sb.append("; ");
                sb.append((given + " " + family).trim());
            }
            crAuthors = sb.toString();
        }
        item.dBAuthorsProperty().set(crAuthors);

        

        boolean isRawFallback = item.getPdfTitle().equals("Unknown Title");
        String pdfSource = isRawFallback ? item.getRawText() : item.getPdfTitle();
        String pdfAuthors = item.getAuthors(); // Ensure you added the getter to ReferenceItem!

        
        int titleScore;
        if (isRawFallback) {
            
            titleScore = FuzzySearch.partialRatio(crTitle.toLowerCase(), pdfSource.toLowerCase());
        } else {
                  
            titleScore = FuzzySearch.ratio(crTitle.toLowerCase(), pdfSource.toLowerCase());
        }

        score(item, crTitle, crAuthors, pdfAuthors, titleScore);
    }

	private void score(ReferenceItem item, String crTitle, String crAuthors, String pdfAuthors, int titleScore) {
		
        int authorScore = FuzzySearch.tokenSortRatio(crAuthors, pdfAuthors);

        
        int finalScore;

        if (authorScore < 50) {
            
            finalScore = Math.min(titleScore, 50); 
        } else {
            
            finalScore = (int) ((titleScore * 0.6) + (authorScore * 0.4));
        }

        
        item.matchScoreProperty().set(finalScore);
        
        logger.log("API_RES", String.format("ID %d | Match Found: Score=%d%% | Title: %s", 
                item.getId(), finalScore, crTitle));

        if (finalScore > 75) { 
            item.statusProperty().set("✅ PASS");
            item.statusColorProperty().set(Color.GREEN);
            item.setVerified(true);
        } else if (finalScore > 50) {
            item.statusProperty().set("! CHECK");
            item.statusColorProperty().set(Color.ORANGE);
        } else {
            item.statusProperty().set("❌ FAIL");
            item.statusColorProperty().set(Color.RED);
        }
	}
	
	/** Helper to safely format text for a URL**/
	private String cleanText(String text) {
	    if (text == null) return "";
	    
	    
	    String clean = text.replaceAll("[^a-zA-Z0-9\\s]", "");
	    
	    
	    return clean.trim().replace(" ", "+");
	}

	private void markNotFound(ReferenceItem item) {
		item.statusProperty().set("❌ NOT FOUND");
		item.statusColorProperty().set(Color.RED);
		item.matchScoreProperty().set(0);
	}
}