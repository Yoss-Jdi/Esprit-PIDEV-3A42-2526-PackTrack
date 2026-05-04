package com.gestioncolis.services;

import com.gestioncolis.entities.Post;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Properties;

public class GeminiChatbotService {

    private final PostService postService;
    private final String apiKey;
    private final HttpClient httpClient;

    private static final String GEMINI_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";

    public GeminiChatbotService(PostService postService) {
        this.postService = postService;
        this.apiKey = loadApiKey();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    private String loadApiKey() {
        Properties props = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("forum.properties")) {
            if (is != null) {
                props.load(is);
            }
        } catch (Exception e) {
            System.err.println("[GeminiChatbotService] Erreur lors du chargement de la clé API: " + e.getMessage());
        }
        return props.getProperty("gemini.api.key", "");
    }

    public String processUserMessage(String userMessage, long currentUserId) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return "Que puis-je faire pour vous ?";
        }

        String msgLower = userMessage.toLowerCase().trim();

        // 1. Recherche Locale (Prioritaire)
        if (msgLower.contains("cherche") || msgLower.contains("trouve") || msgLower.contains("sujet") || msgLower.contains("post")) {
            String searchTerm = extractSearchTerm(msgLower);
            if (!searchTerm.isEmpty()) {
                try {
                    List<Post> results = postService.searchAllPostsForUser(searchTerm, currentUserId);
                    if (results != null && !results.isEmpty()) {
                        StringBuilder sb = new StringBuilder("🔍 **Voici ce que j'ai trouvé :**\n");
                        for (int i = 0; i < Math.min(results.size(), 3); i++) {
                            Post p = results.get(i);
                            sb.append("- ").append(p.getTitle()).append(" (ID: ").append(p.getId()).append(")\n");
                        }
                        return sb.toString();
                    } else {
                        return "Aucun post trouvé pour : '" + searchTerm + "'.";
                    }
                } catch (Exception e) {
                    System.err.println("[GeminiChatbotService] Erreur DB: " + e.getMessage());
                    return "Erreur lors de la recherche dans la base de données.";
                }
            }
        }

        // 2. Fallback Intelligent vers Gemini 1.5 Flash
        return callGemini(userMessage);
    }

    private String extractSearchTerm(String msg) {
        String term = msg.replace("cherche", "")
                         .replace("trouve", "")
                         .replace("des posts sur", "")
                         .replace("des sujets sur", "")
                         .replace("un post sur", "")
                         .replace("un sujet sur", "")
                         .replace("sujet", "")
                         .replace("post", "")
                         .replace("les", "")
                         .replace("le", "")
                         .replace("la", "")
                         .trim();
        return term;
    }

    private String callGemini(String userMessage) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return "Erreur : La clé d'API Gemini n'est pas configurée dans forum.properties.";
        }

        try {
            String prompt = "Tu es un assistant utile pour notre application de forum. Réponds en français de manière très concise (max 3 phrases) à la demande suivante : " + userMessage;

            // Construction du payload JSON pour Gemini
            JSONObject partsObj = new JSONObject();
            partsObj.put("text", prompt);

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
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parseGeminiResponse(response.body());
            } else {
                System.err.println("[GeminiChatbotService] Erreur API Gemini. Code: " + response.statusCode() + " Body: " + response.body());
                return "Désolé, je n'ai pas pu générer de réponse (Code " + response.statusCode() + ").";
            }
        } catch (Exception e) {
            System.err.println("[GeminiChatbotService] Exception lors de l'appel Gemini: " + e.getMessage());
            return "Erreur de connexion à l'IA.";
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
            System.err.println("[GeminiChatbotService] Erreur parsing JSON: " + e.getMessage());
            return "Erreur lors de la lecture de la réponse de l'IA.";
        }
    }
}
