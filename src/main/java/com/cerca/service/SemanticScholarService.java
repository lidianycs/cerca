package com.cerca.service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import com.cerca.model.ReferenceItem;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javafx.application.Platform;
import javafx.scene.paint.Color;
import me.xdrop.fuzzywuzzy.FuzzySearch;	

/**
 * Queries the SemanticScholar API to search for a paper by title and verifies
 * if it exists.
 * * @author Lidiany Cerqueira
 */
public class SemanticScholarService {

    private final HttpClient client;
    private final LogService logger;
    private String apiKey = ""; // Stores the API key

    // Semantic Scholar API Endpoint
    private static final String API_URL = "https://api.semanticscholar.org/graph/v1/paper/search";

    public SemanticScholarService(LogService logger) {
        this.logger = logger;
        this.client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }
    

    public boolean verify(ReferenceItem item) {
    	
    	if(apiKey.equals("")) {
    		logger.log("ERROR", "Set your SemanticScholar API key.");
    		return false;
    	}
    	
        String queryTerm = item.getPdfTitle();
        int maxRetries = 2;
        int waitTime = 3000;

        boolean isRawFallback = (queryTerm == null || queryTerm.equals("Unknown Title") || queryTerm.length() < 5);
        if (isRawFallback)
            queryTerm = item.getRawText();

        if (queryTerm == null || queryTerm.length() < 5)
            return false;
            
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                String cleanQuery = queryTerm.replaceAll("[^a-zA-Z0-9\\s]", " ").trim();
                String encodedQuery = URLEncoder.encode(cleanQuery, StandardCharsets.UTF_8);

                try {
                    Thread.sleep(waitTime);
                } catch (InterruptedException ignored) {
                }
                
                String url = API_URL + "?query=" + encodedQuery + "&limit=1&fields=title,authors,externalIds,url";

                logger.log("API_REQ", "SemanticScholar URL: " + url);

                // --- UPDATED: Inject API Key into Headers ---
                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET();

                // Add the header if the key was loaded successfully
                if (!apiKey.isEmpty()) {
                    requestBuilder.header("x-api-key", apiKey);
                }

                HttpRequest request = requestBuilder.build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    return parseAndScore(response.body(), item);
                } else if (response.statusCode() == 429) {
                    logger.log("ERROR", "SemanticScholar Rate Limit Hit (429)");
                    try {
                        Thread.sleep(waitTime);
                    } catch (InterruptedException ignored) {
                    }
                    waitTime *= 2; 
                    continue; 
                } else if (response.statusCode() == 403) {
                    // 403 usually means the API key is invalid
                    logger.log("ERROR", "SemanticScholar Forbidden (403) - Check if your API key is valid.");
                    return false; // Don't retry if the key is rejected
                } else {
                    logger.log("ERROR", "SemanticScholar API Error: " + response.statusCode());
                }

            } catch (Exception e) {
                logger.log("ERROR", "SemanticScholar Connection Error: " + e.getMessage());
            }
        }
        return false;
    }

    private boolean parseAndScore(String json, ReferenceItem item) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();

            if (!root.has("data") || root.getAsJsonArray("data").size() == 0)
                return false;

            JsonObject paper = root.getAsJsonArray("data").get(0).getAsJsonObject();

            // 1. Extract Data
            String dbTitle = paper.has("title") ? paper.get("title").getAsString() : "Unknown";

            StringBuilder sb = new StringBuilder();
            if (paper.has("authors")) {
                JsonArray authorsArray = paper.getAsJsonArray("authors");
                for (int i = 0; i < authorsArray.size(); i++) {
                    JsonObject authorObj = authorsArray.get(i).getAsJsonObject();
                    if (authorObj.has("name")) {
                        if (sb.length() > 0)
                            sb.append("; ");
                        sb.append(authorObj.get("name").getAsString());
                    }
                }
            }
            String dbAuthors = sb.toString();

            String dbDoi = "";
            if (paper.has("externalIds")) {
                JsonObject ids = paper.getAsJsonObject("externalIds");
                if (ids.has("DOI")) {
                    dbDoi = ids.get("DOI").getAsString();
                } else if (ids.has("ArXiv")) {
                    dbDoi = "arXiv:" + ids.get("ArXiv").getAsString();
                }
            }
            
            if (dbDoi.isEmpty() && paper.has("url")) {
                dbDoi = paper.get("url").getAsString();
            }

            // 2. Scoring
            boolean isRawFallback = item.getPdfTitle().equals("Unknown Title");
            String pdfSource = isRawFallback ? item.getRawText() : item.getPdfTitle();
            String pdfAuthors = item.getAuthors();

            int titleScore = isRawFallback ? FuzzySearch.partialRatio(dbTitle.toLowerCase(), pdfSource.toLowerCase())
                    : FuzzySearch.ratio(dbTitle.toLowerCase(), pdfSource.toLowerCase());

            int authorScore = FuzzySearch.tokenSortRatio(dbAuthors, pdfAuthors);

            int finalScore;
            if (authorScore < 40) {
                finalScore = Math.min(titleScore, 50);
            } else {
                finalScore = (int) ((titleScore * 0.6) + (authorScore * 0.4));
            }

            // 3. UI Updates
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

                logger.log("API_RES", String.format("[S.SCHOLAR] Match Found: '%s' (Score: %d)", dbTitle, finalScore));
                return true;
            }

        } catch (Exception e) {
            logger.log("ERROR", "Error parsing Semantic Scholar JSON: " + e.getMessage());
        }
        return false;
    }

	/**
	 * @return
	 */
	public String getApiKey() {
		// TODO Auto-generated method stub
		return apiKey;
	}

	/**
	 * @param newKey
	 */
	public void setApiKey(String newKey) {
		this.apiKey = newKey;
		
	}
}