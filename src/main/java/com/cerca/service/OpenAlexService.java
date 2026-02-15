package com.cerca.service;

import com.cerca.model.ReferenceItem;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.scene.paint.Color;
import me.xdrop.fuzzywuzzy.FuzzySearch;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class OpenAlexService {

    private final HttpClient client;
    private final LogService logger;   
  
      
    private String email;
    
    private static final String API_URL = "https://api.openalex.org/works";

    public OpenAlexService(LogService logger) {
        this.logger = logger;
        this.client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public boolean verify(ReferenceItem item) {
        String queryTerm = item.getPdfTitle();
        if (queryTerm == null || queryTerm.length() < 5) return false;

        try {
            // 1. Build Query
            // We search for the title and ask for specific fields to make it faster
            String encodedQuery = URLEncoder.encode(queryTerm, StandardCharsets.UTF_8);
            
            // mailto param puts you in the fast lane
            String url = API_URL + "?search=" + encodedQuery + "&per-page=1&mailto=" + email;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            // 2. Send Request
            String title = cleanText(item.getPdfTitle());
	        String author = cleanText(item.getAuthors());
	        
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            logger.log("API_REQ", String.format("Open Alex ID %d | Querying: Title='%s' Author='%s' Email='%s'", 
	                item.getId(), title, author, email));
            
            if (response.statusCode() == 200) {
                return parseAndScore(response.body(), item);
            } else {
                logger.log("ERROR", "OpenAlex Error: " + response.statusCode());
            }

        } catch (Exception e) {
            logger.log("ERROR", "OpenAlex Connection Error: " + e.getMessage());
        }
        return false;
    }
    
	/** Helper to safely format text for a URL**/
	private String cleanText(String text) {
	    if (text == null) return "";
	    
	    
	    String clean = text.replaceAll("[^a-zA-Z0-9\\s]", "");
	    
	    
	    return clean.trim().replace(" ", "+");
	}

    private boolean parseAndScore(String json, ReferenceItem item) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonArray results = root.getAsJsonArray("results");

            if (results.size() == 0) return false;

            // Get best match
            JsonObject work = results.get(0).getAsJsonObject();

            // --- 1. Extract Data ---
            String dbTitle = work.has("title") && !work.get("title").isJsonNull() 
                           ? work.get("title").getAsString() 
                           : "Unknown";
            
            // Extract DOI
            String dbDoi = "";
            if (work.has("doi") && !work.get("doi").isJsonNull()) {
                dbDoi = work.get("doi").getAsString(); // OpenAlex returns full URL: https://doi.org/10.xxx
            }

            // Extract Authors
            StringBuilder sb = new StringBuilder();
            if (work.has("authorships")) {
                JsonArray authorships = work.getAsJsonArray("authorships");
                for (int i = 0; i < authorships.size(); i++) {
                    JsonObject authObj = authorships.get(i).getAsJsonObject();
                    if (authObj.has("author")) {
                        JsonObject authorDetails = authObj.getAsJsonObject("author");
                        if (authorDetails.has("display_name")) {
                            if (sb.length() > 0) sb.append("; ");
                            sb.append(authorDetails.get("display_name").getAsString());
                        }
                    }
                }
            }
            String dbAuthors = sb.toString();
            
            // Need final variable for lambda
            final String finalDbDoi = dbDoi;

            // --- 2. Scoring (Same Standard Logic) ---
            int titleScore = FuzzySearch.ratio(dbTitle.toLowerCase(), item.getPdfTitle().toLowerCase());
            int authorScore = FuzzySearch.tokenSortRatio(dbAuthors, item.getAuthors());

            int finalScore;
            if (authorScore < 40) {
                finalScore = Math.min(titleScore, 50);
            } else {
                finalScore = (int) ((titleScore * 0.6) + (authorScore * 0.4));
            }

            // --- 3. Update UI ---
            if (finalScore > 50) {
                Platform.runLater(() -> {
                	item.crossrefTitleProperty().set(dbTitle);
					item.dBAuthorsProperty().set(dbAuthors);
                    

                    
                    item.matchScoreProperty().set(finalScore);

                    if (finalScore > 75) {
                        item.statusProperty().set("✅ PASS");
                        item.statusColorProperty().set(Color.GREEN);
                        item.setVerified(true);
                    } else {
                        item.statusProperty().set("! CHECK");
                        item.statusColorProperty().set(Color.ORANGE);
                    }
                });
                
                logger.log("API_RES", "[OpenAlex] Match: " + dbTitle);
                return true;
            }

        } catch (Exception e) {
            logger.log("ERROR", "OpenAlex Parse Error: " + e.getMessage());
        }
        return false;
    }

	/**
	 * @return the email
	 */
	public String getEmail() {
		return email;
	}

	/**
	 * @param email the email to set
	 */
	public void setEmail(String email) {
		this.email = email;
	}
}