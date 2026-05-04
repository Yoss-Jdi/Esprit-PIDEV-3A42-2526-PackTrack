package com.gestioncolis.utils;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Properties;
import java.io.InputStream;

public final class DataSource {

    // ================= CONFIG =================
    private final String url;
    private final String user;
    private final String password;

    private Connection cnx;
    private static DataSource instance;

    // ================= CONSTRUCTOR =================
    private DataSource() {

        // priorité : system env > properties > default
        Properties props = new Properties();

        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("forum.properties")) {
            if (is != null) props.load(is);
        } catch (Exception e) {
            System.out.println("Impossible de charger forum.properties: " + e.getMessage());
        }

        this.url = getValue("DB_URL",
                props.getProperty("db.url", "jdbc:mysql://localhost:3306/trackpackdb"));

        this.user = getValue("DB_USER",
                props.getProperty("db.user", "root"));

        this.password = getValue("DB_PASSWORD",
                props.getProperty("db.password", ""));

        ensureDatabaseExists();
        connect();
        ensureTables();
    }

    // ================= SINGLETON =================
    public static synchronized DataSource getInstance() {
        if (instance == null) {
            instance = new DataSource();
        }
        return instance;
    }

    // ================= CONNECTION =================
    public Connection getCnx() {
        try {
            if (cnx == null || cnx.isClosed()) {
                connect();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Connexion DB invalide", e);
        }
        return cnx;
    }

    public void closeConnection() {
        try {
            if (cnx != null && !cnx.isClosed()) {
                cnx.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur fermeture DB", e);
        }
    }

    // ================= CORE DB =================
    private void connect() {
        try {
            cnx = DriverManager.getConnection(url, user, password);
            System.out.println("Connected successfully !");
        } catch (SQLException e) {
            throw new RuntimeException("Erreur connexion DB: " + e.getMessage(), e);
        }
    }

    private void ensureDatabaseExists() {
        String dbName = extractDbName(url);
        String serverUrl = extractServerUrl(url);

        try (Connection conn = DriverManager.getConnection(serverUrl, user, password);
             Statement st = conn.createStatement()) {

            st.executeUpdate(
                    "CREATE DATABASE IF NOT EXISTS `" + dbName + "`"
            );

        } catch (SQLException e) {
            throw new RuntimeException("Impossible de creer la base", e);
        }
    }

    private void ensureTables() {
        try (Statement st = getCnx().createStatement()) {

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS technicien (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    nom VARCHAR(120),
                    prenom VARCHAR(120),
                    specialite VARCHAR(120),
                    telephone VARCHAR(50),
                    email VARCHAR(150)
                )
            """);

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS vehicule (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    matricule VARCHAR(120),
                    marque VARCHAR(120),
                    modele VARCHAR(120),
                    couleur VARCHAR(80),
                    prix_location DOUBLE,
                    disponible BOOLEAN,
                    technicien_id INT NULL
                )
            """);

        } catch (SQLException e) {
            throw new RuntimeException("Erreur creation tables", e);
        }
    }

    // ================= UTILITIES =================
    public LocalDateTime toLocalDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp ts = rs.getTimestamp(column);
        return ts != null ? ts.toLocalDateTime() : null;
    }

    public long extractGeneratedId(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.getGeneratedKeys()) {
            if (rs.next()) return rs.getLong(1);
        }
        throw new RuntimeException("No generated key");
    }

    public long count(String sql, Object... params) {
        try (PreparedStatement ps = getCnx().prepareStatement(sql)) {

            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0;
            }

        } catch (SQLException e) {
            throw new RuntimeException("SQL error count", e);
        }
    }

    // ================= HELPERS =================
    private String getValue(String key, String def) {
        String v = System.getenv(key);
        return (v == null || v.isBlank()) ? def : v;
    }

    private String extractDbName(String url) {
        return url.substring(url.lastIndexOf('/') + 1).split("\\?")[0];
    }

    private String extractServerUrl(String url) {
        return url.substring(0, url.lastIndexOf('/') + 1);
    }
}