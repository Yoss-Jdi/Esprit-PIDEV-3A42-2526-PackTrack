package com.example.rayen.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class DataSource {
    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/espritfx?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "";
    private static DataSource instance;

    private final String url;
    private final String user;
    private final String password;
    private Connection cnx;

    private DataSource() {
        this.url = lireConfiguration("DB_URL", DEFAULT_URL);
        this.user = lireConfiguration("DB_USER", DEFAULT_USER);
        this.password = lireConfiguration("DB_PASSWORD", DEFAULT_PASSWORD);
        initialiserBaseDeDonnees();
        connecter();
        initialiserTables();
    }

    public static synchronized DataSource getInstance() {
        if (instance == null) {
            instance = new DataSource();
        }
        return instance;
    }

    public Connection getCnx() {
        try {
            if (cnx == null || cnx.isClosed()) {
                connecter();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de verifier la connexion a la base de donnees.", e);
        }

        return cnx;
    }

    public void closeConnection() {
        if (cnx == null) {
            return;
        }

        try {
            if (!cnx.isClosed()) {
                cnx.close();
            }
            cnx = null;
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de fermer la connexion a la base de donnees.", e);
        }
    }

    private void initialiserBaseDeDonnees() {
        String nomBase = extraireNomBase(url);
        String urlServeur = construireUrlServeur(url);

        try (Connection connexionServeur = DriverManager.getConnection(urlServeur, user, password);
             Statement statement = connexionServeur.createStatement()) {
            statement.executeUpdate(
                    "CREATE DATABASE IF NOT EXISTS `" + nomBase + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
            );
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Impossible de creer ou de verifier la base de donnees espritfx.",
                    e
            );
        }
    }

    private void initialiserTables() {
        try (Statement statement = getCnx().createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS technicien (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        nom VARCHAR(120) NOT NULL,
                        prenom VARCHAR(120) NOT NULL,
                        specialite VARCHAR(150) NOT NULL,
                        telephone VARCHAR(50) NOT NULL,
                        email VARCHAR(190) NOT NULL UNIQUE
                    )
                    """);

            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS vehicule (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        matricule VARCHAR(120) NOT NULL UNIQUE,
                        marque VARCHAR(120) NOT NULL,
                        modele VARCHAR(120) NOT NULL,
                        couleur VARCHAR(80) NOT NULL,
                        prix_location DOUBLE NOT NULL,
                        disponible BOOLEAN NOT NULL,
                        technicien_id INT NULL,
                        CONSTRAINT fk_vehicule_technicien
                            FOREIGN KEY (technicien_id)
                            REFERENCES technicien(id)
                            ON DELETE SET NULL
                            ON UPDATE CASCADE
                    )
                    """);
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de creer les tables de l'application.", e);
        }
    }

    private void connecter() {
        try {
            cnx = DriverManager.getConnection(url, user, password);
            System.out.println("Connexion SQL etablie avec succes.");
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Echec de connexion a la base de donnees. Verifiez l'URL, l'utilisateur, le mot de passe et l'etat du serveur MySQL.",
                    e
            );
        }
    }

    private String construireUrlServeur(String jdbcUrl) {
        int indexQuery = jdbcUrl.indexOf('?');
        String suffixe = indexQuery >= 0 ? jdbcUrl.substring(indexQuery) : "";
        String baseSansParametres = indexQuery >= 0 ? jdbcUrl.substring(0, indexQuery) : jdbcUrl;
        int dernierSlash = baseSansParametres.lastIndexOf('/');

        if (dernierSlash <= "jdbc:mysql://".length()) {
            throw new IllegalStateException("L'URL JDBC doit contenir le nom de la base de donnees cible.");
        }

        return baseSansParametres.substring(0, dernierSlash + 1) + suffixe;
    }

    private String extraireNomBase(String jdbcUrl) {
        int indexQuery = jdbcUrl.indexOf('?');
        String baseSansParametres = indexQuery >= 0 ? jdbcUrl.substring(0, indexQuery) : jdbcUrl;
        int dernierSlash = baseSansParametres.lastIndexOf('/');

        if (dernierSlash < 0 || dernierSlash == baseSansParametres.length() - 1) {
            throw new IllegalStateException("Impossible d'extraire le nom de la base de donnees depuis l'URL JDBC.");
        }

        return baseSansParametres.substring(dernierSlash + 1).replace("`", "");
    }

    private String lireConfiguration(String cle, String valeurParDefaut) {
        String valeur = System.getenv(cle);
        return valeur == null || valeur.isBlank() ? valeurParDefaut : valeur.trim();
    }
}
