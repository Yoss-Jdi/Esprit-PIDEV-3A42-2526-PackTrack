package com.example.forumapp.services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.example.forumapp.utils.DataSource;

/**
 * Service de gestion de la configuration de l'application (ON/OFF Ollama, etc.)
 */
public class ConfigService {

    private static final String CONFIG_KEY_OLLAMA_ENABLED = "ollama_moderation_enabled";

    public ConfigService() {
        initializeConfig();
    }

    /**
     * Initialise la table de configuration si elle n'existe pas.
     */
    private void initializeConfig() {
        String sql = """
            CREATE TABLE IF NOT EXISTS app_config (
                config_key VARCHAR(100) PRIMARY KEY,
                config_value VARCHAR(500) NOT NULL,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            )
            """;

        try (PreparedStatement stmt = DataSource.getInstance().getCnx().prepareStatement(sql)) {
            stmt.executeUpdate();
            System.out.println("[CONFIG] Table app_config initialisée");

            // Initialiser avec les valeurs par défaut
            setDefaultConfig();
        } catch (SQLException e) {
            System.out.println("[CONFIG] Table app_config existe déjà ou erreur: " + e.getMessage());
        }
    }

    /**
     * Définit les configurations par défaut si elles n'existent pas.
     */
    private void setDefaultConfig() {
        if (!configExists(CONFIG_KEY_OLLAMA_ENABLED)) {
            setConfig(CONFIG_KEY_OLLAMA_ENABLED, "true");
        }
    }

    /**
     * Retourne true si la modération Ollama est activée.
     */
    public boolean isOllamaEnabled() {
        String value = getConfig(CONFIG_KEY_OLLAMA_ENABLED);
        return value == null || value.equalsIgnoreCase("true");
    }

    /**
     * Active ou désactive la modération Ollama.
     */
    public void setOllamaEnabled(boolean enabled) {
        setConfig(CONFIG_KEY_OLLAMA_ENABLED, enabled ? "true" : "false");
        System.out.println("[CONFIG] Modération Ollama: " + (enabled ? "✅ ACTIVÉE" : "❌ DÉSACTIVÉE"));
    }

    /**
     * Obtient une valeur de configuration.
     */
    public String getConfig(String key) {
        String sql = "SELECT config_value FROM app_config WHERE config_key = ?";
        try (PreparedStatement stmt = DataSource.getInstance().getCnx().prepareStatement(sql)) {
            stmt.setString(1, key);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("config_value");
                }
            }
        } catch (SQLException e) {
            System.out.println("[CONFIG] Erreur lecture config: " + e.getMessage());
        }
        return null;
    }

    /**
     * Définit une valeur de configuration.
     */
    public void setConfig(String key, String value) {
        String selectSql = "SELECT config_value FROM app_config WHERE config_key = ?";
        String insertSql = "INSERT INTO app_config (config_key, config_value) VALUES (?, ?)";
        String updateSql = "UPDATE app_config SET config_value = ? WHERE config_key = ?";

        try {
            Connection conn = DataSource.getInstance().getCnx();

            // Vérifier si la clé existe
            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                selectStmt.setString(1, key);
                try (ResultSet rs = selectStmt.executeQuery()) {
                    if (rs.next()) {
                        // Mettre à jour
                        try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                            updateStmt.setString(1, value);
                            updateStmt.setString(2, key);
                            updateStmt.executeUpdate();
                        }
                    } else {
                        // Insérer
                        try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                            insertStmt.setString(1, key);
                            insertStmt.setString(2, value);
                            insertStmt.executeUpdate();
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("[CONFIG] Erreur écriture config: " + e.getMessage());
        }
    }

    /**
     * Vérifie si une clé de configuration existe.
     */
    private boolean configExists(String key) {
        return getConfig(key) != null;
    }
}
