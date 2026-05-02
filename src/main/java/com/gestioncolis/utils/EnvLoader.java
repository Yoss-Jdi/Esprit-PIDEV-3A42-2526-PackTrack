package com.gestioncolis.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Charge les variables d'un fichier .env situé à la racine du projet.
 *
 * Priorité : variable d'environnement système > .env
 *
 * Format attendu dans .env :
 *   OPENWEATHER_API_KEY=abc123
 *   TOMTOM_API_KEY=xyz456
 *   # Commentaires ignorés
 */
public class EnvLoader {

    private static final Map<String, String> ENV = new HashMap<>();
    private static boolean loaded = false;

    /** Chemins cherchés dans l'ordre (racine du projet ou répertoire courant). */
    private static final String[] CANDIDATES = {
            ".env",
            "../.env",
            System.getProperty("user.dir") + File.separator + ".env"
    };

    static {
        load();
    }

    private static void load() {
        if (loaded) return;
        loaded = true;

        for (String path : CANDIDATES) {
            File f = new File(path);
            if (f.exists() && f.isFile()) {
                try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        line = line.trim();
                        // Ignorer commentaires et lignes vides
                        if (line.isEmpty() || line.startsWith("#")) continue;
                        int eq = line.indexOf('=');
                        if (eq < 1) continue;
                        String key   = line.substring(0, eq).trim();
                        String value = line.substring(eq + 1).trim();
                        // Retirer les guillemets optionnels : KEY="value" ou KEY='value'
                        if (value.length() >= 2
                                && ((value.startsWith("\"") && value.endsWith("\""))
                                || (value.startsWith("'")  && value.endsWith("'")))) {
                            value = value.substring(1, value.length() - 1);
                        }
                        ENV.put(key, value);
                    }
                    System.out.println("[EnvLoader] Fichier .env chargé : " + f.getAbsolutePath());
                    return;
                } catch (IOException e) {
                    System.err.println("[EnvLoader] Impossible de lire " + path + " : " + e.getMessage());
                }
            }
        }
        System.out.println("[EnvLoader] Aucun fichier .env trouvé — variables système uniquement.");
    }

    /**
     * Retourne la valeur associée à {@code key}.
     * Priorité : variable d'environnement système > .env > {@code defaultValue}.
     */
    public static String get(String key, String defaultValue) {
        // 1. Variable d'environnement système (export / System properties)
        String sys = System.getenv(key);
        if (sys != null && !sys.isBlank()) return sys;

        // 2. Fichier .env
        String env = ENV.get(key);
        if (env != null && !env.isBlank()) return env;

        // 3. Valeur par défaut
        return defaultValue;
    }

    /** Surcharge sans valeur par défaut — retourne null si absent. */
    public static String get(String key) {
        return get(key, null);
    }
}