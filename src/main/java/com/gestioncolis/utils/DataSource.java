package com.gestioncolis.utils;

import java.io.InputStream;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Properties;

public class DataSource {

    private final String URL;
    private final String USER;
    private final String PWD;

    private Connection cnx;

    private static DataSource instance;

    private DataSource() {
        Properties props = new Properties();

        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("forum.properties")) {

            if (is != null) {
                props.load(is);
            }

        } catch (Exception e) {
            System.out.println("Impossible de charger forum.properties: " + e.getMessage());
        }

        // ✅ UNIFICATION BASE DE DONNÉES
        URL = props.getProperty("db.url",
                "jdbc:mysql://localhost:3306/packtrackdb");

        USER = props.getProperty("db.user", "root");
        PWD = props.getProperty("db.password", "");

        try {
            cnx = DriverManager.getConnection(URL, USER, PWD);
            System.out.println("Connected successfully !");
        } catch (SQLException e) {
            System.out.println("Erreur connexion DB: " + e.getMessage());
        }
    }

    public static DataSource getInstance() {
        if (instance == null) {
            instance = new DataSource();
        }
        return instance;
    }

    public Connection getCnx() {
        return this.cnx;
    }

    // ================= UTILITIES =================

    public LocalDateTime toLocalDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp ts = rs.getTimestamp(column);
        return ts == null ? null : ts.toLocalDateTime();
    }

    public long extractGeneratedId(PreparedStatement stmt) throws SQLException {
        try (ResultSet keys = stmt.getGeneratedKeys()) {
            if (keys.next()) return keys.getLong(1);
            throw new RuntimeException("Impossible de récupérer la clé générée.");
        }
    }

    public long countBySql(String sql, Object... params) {
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {

            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0;
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erreur SQL lors du comptage.", e);
        }
    }
}