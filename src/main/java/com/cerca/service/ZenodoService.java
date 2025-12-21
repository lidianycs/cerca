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

public class ZenodoService {

    private final HttpClient client;
    private final LogService logger;

    public ZenodoService(LogService logger) {
        this.logger = logger;
        this.client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public boolean verify(ReferenceItem item) {
        String queryTerm = item.getPdfTitle();
        boolean isRawFallback = (queryTerm == null || queryTerm.equals("Unknown Title") || queryTerm.length() < 5);
        if (isRawFallback) queryTerm = item.getRawText();

        if (queryTerm == null || queryTerm.length() < 5) return false;

        try {
            String cleanQuery = queryTerm.replaceAll("[^a-zA-Z0-9\\s]", " ").trim();
            String encodedQuery = URLEncoder.encode(cleanQuery, StandardCharsets.UTF_8);
            String url = "https://zenodo.org/api/records?q=metadata.title:(" + encodedQuery + ")&sort=bestmatch&size=1";

            logger.log("API_REQ", "Zenodo URL: " + url);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parseAndScore(response.body(), item);
            } else {
                logger.log("ERROR", "Zenodo API Error: " + response.statusCode());
            }

        } catch (Exception e) {
            logger.log("ERROR", "Zenodo Connection Error: " + e.getMessage());
        }
        return false;
    }

    private boolean parseAndScore(String json, ReferenceItem item) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            if (!root.has("hits") || !root.getAsJsonObject("hits").has("hits")) return false;
            
            JsonArray hits = root.getAsJsonObject("hits").getAsJsonArray("hits");
            if (hits.size() == 0) return false;

            JsonObject record = hits.get(0).getAsJsonObject();
            JsonObject metadata = record.getAsJsonObject("metadata");

            // --- 1. Extract Data ---
            String zenTitle = metadata.has("title") ? metadata.get("title").getAsString() : "Unknown";
            
            StringBuilder sb = new StringBuilder();
            if (metadata.has("creators")) {
                JsonArray creators = metadata.getAsJsonArray("creators");
                for (var c : creators) {
                    JsonObject creatorObj = c.getAsJsonObject();
                    if (creatorObj.has("name")) {
                        if (sb.length() > 0) sb.append("; ");
                        sb.append(creatorObj.get("name").getAsString());
                    }
                }
            }
            String zenAuthors = sb.toString();

            // --- 2. Scoring ---
            boolean isRawFallback = item.getPdfTitle().equals("Unknown Title");
            String pdfSource = isRawFallback ? item.getRawText() : item.getPdfTitle();
            String pdfAuthors = item.getAuthors();

            int titleScore = isRawFallback 
                ? FuzzySearch.partialRatio(zenTitle.toLowerCase(), pdfSource.toLowerCase())
                : FuzzySearch.ratio(zenTitle.toLowerCase(), pdfSource.toLowerCase());

            int authorScore = FuzzySearch.tokenSortRatio(zenAuthors, pdfAuthors);
            
            int finalScore;
            if (authorScore < 40) {
                finalScore = Math.min(titleScore, 50);
            } else {
                finalScore = (int) ((titleScore * 0.6) + (authorScore * 0.4));
            }

          
            if (finalScore > 50) {
           
                Platform.runLater(() -> {
                    item.crossrefTitleProperty().set(zenTitle);   
                    item.dBAuthorsProperty().set(zenAuthors); 
                    
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
                
                logger.log("API_RES", String.format("[ZENODO] Match Found: '%s' (Score: %d)", zenTitle, finalScore));
                return true;
            }

        } catch (Exception e) {
            logger.log("ERROR", "Error parsing Zenodo JSON: " + e.getMessage());
        }
        return false;
    }
}