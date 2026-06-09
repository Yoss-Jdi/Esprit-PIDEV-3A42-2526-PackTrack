package com.esprit.services;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class AiService {

    private static final String API_KEY = System.getProperty("gemini.api.key",
            "AIzaSyAF9f5ozP3pBiVkO02CCzBoYR9xwsPAzkQ");

    //  Liste de modèles en fallback automatique
    private static final String[] MODELES = {
            "gemini-2.5-flash",          // ✅ Modèle actuel recommandé 2026
            "gemini-2.5-flash-lite",     // ✅ Fallback léger
            "gemini-2.0-flash",          // ✅ Fallback stable
    };

    public String genererDescriptionRecompense(String nom, int points) {
        String prompt = "Tu es un assistant pour une application de livraison TrackPack. "
                + "Génère une description courte, professionnelle et attractive (2-3 phrases maximum) "
                + "pour cette récompense : '" + nom + "' "
                + "qui coûte " + points + " points de fidélité. "
                + "Réponds uniquement avec la description, sans introduction ni guillemets.";

        //  Essaie chaque modèle jusqu'à ce qu'un fonctionne
        for (String modele : MODELES) {
            String result = appelerAPI(modele, prompt);
            if (result != null) {
                return result;
            }
        }

        return "Quota API dépassé. Réessayez dans quelques minutes.";
    }

    private String appelerAPI(String modele, String prompt) {
        try {

            // Correct — v1beta supporte les nouveaux modèles
            String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/"
                    + modele + ":generateContent?key=" + API_KEY;

            String requestBody = "{"
                    + "\"contents\": [{"
                    + "\"parts\": [{\"text\": \""
                    + prompt.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    + "\"}]"
                    + "}]"
                    + "}";

            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(requestBody.getBytes(StandardCharsets.UTF_8));
            }

            int status = conn.getResponseCode();
            InputStream is = (status == 200)
                    ? conn.getInputStream()
                    : conn.getErrorStream();

            Scanner scanner = new Scanner(is, StandardCharsets.UTF_8);
            StringBuilder response = new StringBuilder();
            while (scanner.hasNextLine()) {
                response.append(scanner.nextLine());
            }
            scanner.close();

            // ✅ Si quota dépassé (429) → essaie le modèle suivant
            if (status == 429) {
                System.out.println("Quota dépassé pour : " + modele + " → essai suivant...");
                return null;
            }

            if (status != 200) {
                System.err.println("Erreur API (" + status + ") modèle " + modele + " : " + response);
                return null;
            }

            // Extraction du texte
            String json = response.toString();
            int start = json.indexOf("\"text\": \"") + 9;
            int end   = json.indexOf("\"", start);

            if (start < 9 || end < 0) {
                System.err.println("Réponse inattendue : " + json);
                return null;
            }

            return json.substring(start, end)
                    .replace("\\n", "\n")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\");

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}