package com.cerca.service.extraction;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Call GROBID to extract a reference from a citation.
 * <p>
 * To run GROBID in a Docker container, call
 * <code>docker run --rm -p 8070:8070 grobid/grobid:0.8.2-full</code>.
 */
public class GrobidTextExtractor implements TextExtractor {
    private static final String GROBID_URL = "http://localhost:8070";

    private final HttpClient httpClient;

    public GrobidTextExtractor() {
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public ParsedData parse(String rawReference) {
        try {
            String responseXml = callGrobid(rawReference);
            return parseTei(responseXml, rawReference);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse reference with GROBID", e);
        }
    }

    private String callGrobid(String citation) throws IOException, InterruptedException {
        String body = "citations=" + URLEncoder.encode(citation, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GROBID_URL + "/api/processCitation"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("GROBID returned status " + response.statusCode());
        }

        return response.body();
    }

    private ParsedData parseTei(String xml, String rawReference) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);

        Document doc = factory.newDocumentBuilder()
                .parse(new InputSource(new StringReader(xml)));

        var xpath = XPathFactory.newInstance().newXPath();

        // --- Authors ---
        NodeList surnameNodes = (NodeList) xpath.evaluate("//author/persName/surname", doc, XPathConstants.NODESET);

        List<String> authors = new ArrayList<>();
        for (int i = 0; i < surnameNodes.getLength(); i++) {
            authors.add(surnameNodes.item(i).getTextContent().trim());
        }

        String authorsJoined = String.join(", ", authors);

        // --- Title (analytic preferred) ---
        String title = xpath.evaluate("//analytic/title/text()", doc).trim();

        if (title.isEmpty()) {
            title = xpath.evaluate("//monogr/title/text()", doc).trim();
        }

        // --- DOI ---
        String doi = xpath.evaluate("//idno[@type='DOI']/text()", doc).trim();

        return new ParsedData(authorsJoined, title);
    }
}
