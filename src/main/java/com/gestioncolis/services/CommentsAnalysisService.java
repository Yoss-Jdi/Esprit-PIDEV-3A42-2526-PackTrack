package com.gestioncolis.services;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONObject;

import com.gestioncolis.utils.DataSource;

public class CommentsAnalysisService {

    private final String apiKey;
    private final HttpClient httpClient;
    private static final String GEMINI_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";
    private static final int TIMEOUT_SECONDS = 30;

    public CommentsAnalysisService() {
        this.apiKey = loadApiKey();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        System.out.println("[CommentsAnalysisService-GEMINI] Initialisé avec Gemini (gemini-2.5-flash)");
    }

    private String loadApiKey() {
        Properties props = new Properties();
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("forum.properties")) {
            if (is != null) {
                props.load(is);
            }
        } catch (Exception e) {
            System.out.println("[CommentsAnalysisService-GEMINI] Erreur chargement clé: " + e.getMessage());
        }
        return props.getProperty("gemini.api.key", "");
    }

    public String analyzePostComments(long postId) throws Exception {
        System.out.println("\n========== COMMENTS ANALYSIS START (GEMINI) ==========");
        System.out.println("[ANALYSIS-GEMINI] Récupération des commentaires pour post_id: " + postId);

        List<String> comments = fetchCommentsByPostId(postId);
        System.out.println("[ANALYSIS-GEMINI] Nombre de commentaires trouvés: " + comments.size());

        if (comments.isEmpty()) {
            System.out.println("[ANALYSIS-GEMINI] Aucun commentaire trouvé pour ce post");
            System.out.println("========== COMMENTS ANALYSIS END ==========\n");
            return "Aucun commentaire à analyser pour ce post.";
        }

        String concatenatedComments = concatenateComments(comments);
        System.out.println("[ANALYSIS-GEMINI] Commentaires concaténés (longueur: " + concatenatedComments.length() + " caractères)");

        String analysis = callGeminiForAnalysis(concatenatedComments);
        System.out.println("[ANALYSIS-GEMINI] Analyse reçue de Gemini");
        System.out.println("========== COMMENTS ANALYSIS END ==========\n");

        return analysis;
    }

    private List<String> fetchCommentsByPostId(long postId) throws SQLException {
        List<String> comments = new ArrayList<>();
        String sql = "SELECT content FROM comments WHERE post_id = ? ORDER BY created_at ASC";

        try (PreparedStatement stmt = DataSource.getInstance().getCnx().prepareStatement(sql)) {
            stmt.setLong(1, postId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String content = rs.getString("content");
                    if (content != null && !content.isBlank()) {
                        comments.add(content.trim());
                    }
                }
            }
        }
        return comments;
    }

    private String concatenateComments(List<String> comments) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < comments.size(); i++) {
            sb.append("Commentaire ").append(i + 1).append(": ")
                    .append(comments.get(i))
                    .append("\n\n");
        }
        return sb.toString();
    }

    private String callGeminiForAnalysis(String commentText) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException("Gemini API key non configurée dans forum.properties");
        }

        System.out.println("[ANALYSIS-GEMINI] Envoi de la requête à Gemini...");

        String systemPrompt = "Tu es un expert en analyse de feedback client pour une plateforme de forum. Analyse les commentaires suivants et identifie de manière structurée : 1) Les frustrations principales, 2) Les solutions proposées par les utilisateurs, 3) Le sentiment général (positif, négatif ou neutre). Réponds en français de façon claire.";
        String fullPrompt = systemPrompt + "\n\nVoici les commentaires à analyser :\n" + commentText;

        JSONObject partsObj = new JSONObject();
        partsObj.put("text", fullPrompt);

        JSONArray partsArray = new JSONArray();
        partsArray.put(partsObj);

        JSONObject contentObj = new JSONObject();
        contentObj.put("parts", partsArray);

        JSONArray contentsArray = new JSONArray();
        contentsArray.put(contentObj);

        JSONObject requestBody = new JSONObject();
        requestBody.put("contents", contentsArray);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GEMINI_ENDPOINT + "?key=" + apiKey))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("[ANALYSIS-GEMINI] Status HTTP: " + response.statusCode());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Gemini API status " + response.statusCode() + " : " + response.body());
            }

            return parseGeminiResponse(response.body());

        } catch (java.net.ConnectException ce) {
            System.out.println("[ANALYSIS-GEMINI] 🔴 Impossible de se connecter à Gemini");
            throw new RuntimeException("Gemini indisponible. Vérifiez votre connexion Internet et votre clé API.", ce);
        }
    }

    private String parseGeminiResponse(String jsonResponse) {
        try {
            JSONObject root = new JSONObject(jsonResponse);
            JSONArray candidates = root.getJSONArray("candidates");
            if (candidates.length() > 0) {
                JSONObject firstCandidate = candidates.getJSONObject(0);
                JSONObject content = firstCandidate.getJSONObject("content");
                JSONArray parts = content.getJSONArray("parts");
                if (parts.length() > 0) {
                    return parts.getJSONObject(0).getString("text").trim();
                }
            }
            return "Pas de réponse pertinente trouvée.";
        } catch (Exception e) {
            System.out.println("[ANALYSIS-GEMINI] Erreur parsing JSON: " + e.getMessage());
            return "Erreur lors de la lecture de la réponse de Gemini.";
        }
    }
}
