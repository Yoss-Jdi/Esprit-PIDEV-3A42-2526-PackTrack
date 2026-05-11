package com.gestioncolis.services;

import com.gestioncolis.entities.User;
import com.gestioncolis.entities.Utilisateurs;
import com.gestioncolis.enums.Role;
import com.gestioncolis.utils.DataSource;
import com.gestioncolis.utils.PasswordUtils;
import com.gestioncolis.utils.SessionManager;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.*;
import java.util.Optional;

public class AuthService {

    private static final String SYMFONY_API_URL = "http://localhost:8000/api/login";
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    private User currentUser;

    public AuthService() {
    }

    /**
     * Login via API Symfony (pour les utilisateurs créés côté Symfony)
     */
    public User login(String email, String password) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            throw new RuntimeException("Email et mot de passe sont obligatoires.");
        }

        try {
            // Essayer d'abord l'authentification via API Symfony
            User user = authenticateViaSymfonyAPI(email.trim().toLowerCase(), password);
            this.currentUser = user;
            return user;
        } catch (Exception apiException) {
            // Fallback : authentification locale (pour les utilisateurs créés en JavaFX)
            Optional<User> found = findByEmail(email.trim().toLowerCase());
            if (found.isEmpty()) {
                throw new RuntimeException("Identifiants invalides.");
            }

            User user = found.get();
            if (!PasswordUtils.matches(password, user.getPasswordHash())) {
                throw new RuntimeException("Identifiants invalides.");
            }

            this.currentUser = user;
            return user;
        }
    }

    /**
     * Authentification via l'API Symfony
     */
    private User authenticateViaSymfonyAPI(String email, String password) throws Exception {
        // Créer le JSON de la requête
        JsonObject jsonBody = new JsonObject();
        jsonBody.addProperty("email", email);
        jsonBody.addProperty("password", password);

        // Créer la requête HTTP
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SYMFONY_API_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody.toString()))
                .build();

        // Envoyer la requête
        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        System.out.println("=== SYMFONY API RESPONSE ===");
        System.out.println("Status: " + response.statusCode());
        System.out.println("Body: " + response.body());
        System.out.println("============================");

        // Parser la réponse
        JsonObject jsonResponse = gson.fromJson(response.body(), JsonObject.class);

        if (jsonResponse.get("success").getAsBoolean()) {
            JsonObject userData = jsonResponse.getAsJsonObject("user");

            // Créer l'objet User
            User user = new User();
            user.setId(userData.get("id").getAsLong());
            user.setEmail(userData.get("email").getAsString());
            user.setNom(userData.get("nom").getAsString() + " " +
                    (userData.has("prenom") ? userData.get("prenom").getAsString() : ""));
            user.setPasswordHash(userData.get("mot_de_passe").getAsString()); // Le hash Symfony
            user.setRole(Role.valueOf(userData.get("role").getAsString()));


            System.out.println("✅ User mappé : " + user.getEmail() + " / " + user.getRole());

            // Synchroniser avec la base locale
            syncUserToLocalDB(user);

            return user;
        } else {
            throw new Exception(jsonResponse.get("message").getAsString());
        }
    }

    /**
     * Synchroniser l'utilisateur Symfony avec la base locale
     */
    private void syncUserToLocalDB(User user) {
        String checkSql = "SELECT id_utilisateur FROM utilisateurs WHERE id_utilisateur = ?";
        String updateSql = "UPDATE utilisateurs SET nom = ?, email = ?, mot_de_passe = ?, role = ? WHERE id_utilisateur = ?";
        String insertSql = "INSERT INTO utilisateurs (id_utilisateur, nom, email, mot_de_passe, role, prenom, created_at) VALUES (?, ?, ?, ?, ?, ?, NOW())";

        try (PreparedStatement checkStmt = DataSource.getInstance().getCnx().prepareStatement(checkSql)) {
            checkStmt.setLong(1, user.getId());
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                // Update existing user
                try (PreparedStatement updateStmt = DataSource.getInstance().getCnx().prepareStatement(updateSql)) {
                    updateStmt.setString(1, user.getNom());
                    updateStmt.setString(2, user.getEmail());
                    updateStmt.setString(3, user.getPasswordHash());
                    updateStmt.setString(4, user.getRole().name());
                    updateStmt.setLong(5, user.getId());
                    updateStmt.executeUpdate();
                }
            } else {
                // Insert new user
                try (PreparedStatement insertStmt = DataSource.getInstance().getCnx().prepareStatement(insertSql)) {
                    insertStmt.setLong(1, user.getId());
                    insertStmt.setString(2, user.getNom());
                    insertStmt.setString(3, user.getEmail());
                    insertStmt.setString(4, user.getPasswordHash());
                    insertStmt.setString(5, user.getRole().name());
                    insertStmt.setString(6, ""); // prenom vide
                    insertStmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            // Log l'erreur mais ne pas bloquer l'authentification
            System.err.println("Erreur lors de la synchronisation locale: " + e.getMessage());
        }
    }

    public void logout() {
        this.currentUser = null;
    }

    public User syncFromMainSession() {
        return syncFromMainSession(SessionManager.getInstance().getUtilisateurConnecte());
    }

    public User syncFromMainSession(Utilisateurs sessionUser) {
        if (sessionUser == null) {
            this.currentUser = null;
            return null;
        }

        String email = sessionUser.getEmail() == null ? null : sessionUser.getEmail().trim().toLowerCase();
        if (email == null || email.isBlank()) {
            throw new RuntimeException("Impossible de synchroniser l'utilisateur forum sans email.");
        }

        User forumUser = findByEmail(email).orElseGet(() -> createForumMirror(sessionUser, email));
        forumUser.setNom(buildDisplayName(sessionUser));
        forumUser.setEmail(email);
        forumUser.setRole(mapForumRole(sessionUser.getRole()));

        this.currentUser = forumUser;
        return forumUser;
    }

    public boolean isAuthenticated() {
        return currentUser != null;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public User requireUser() {
        if (currentUser == null) {
            syncFromMainSession();
        }
        if (currentUser == null) throw new RuntimeException("Aucun utilisateur connecte.");
        return currentUser;
    }

    public User requireAdmin() {
        User user = requireUser();
        if (!user.isAdmin()) throw new RuntimeException("Action reservee a l'administrateur.");
        return user;
    }

    private Optional<User> findByEmail(String email) {
        String sql = "SELECT id_utilisateur, nom, email, mot_de_passe, role FROM utilisateurs WHERE email = ?";
        try (PreparedStatement stmt = DataSource.getInstance().getCnx().prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapUser(rs));
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la recherche utilisateur.", e);
        }
    }

    private User createForumMirror(Utilisateurs sessionUser, String email) {
        String sql = "INSERT INTO utilisateurs (nom, email, mot_de_passe, role, prenom, created_at) VALUES (?, ?, ?, ?, ?, NOW())";
        try (PreparedStatement stmt = DataSource.getInstance().getCnx().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, buildDisplayName(sessionUser));
            stmt.setString(2, email);
            stmt.setString(3, PasswordUtils.hash(email + "#forum"));
            stmt.setString(4, sessionUser.getRole().name());
            stmt.setString(5, sessionUser.getPrenom() != null ? sessionUser.getPrenom() : "");
            stmt.executeUpdate();
            return findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Utilisateur forum créé mais introuvable."));
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la synchronisation de l'utilisateur forum.", e);
        }
    }

    private String buildDisplayName(Utilisateurs sessionUser) {
        String prenom = sessionUser.getPrenom() == null ? "" : sessionUser.getPrenom().trim();
        String nom = sessionUser.getNom() == null ? "" : sessionUser.getNom().trim();
        String fullName = (prenom + " " + nom).trim();
        return fullName.isBlank() ? (sessionUser.getEmail() == null ? "Utilisateur" : sessionUser.getEmail().trim()) : fullName;
    }

    private Role mapForumRole(Role role) {
        return role;
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getLong("id_utilisateur"));
        u.setNom(rs.getString("nom"));
        u.setEmail(rs.getString("email"));
        u.setPasswordHash(rs.getString("mot_de_passe"));
        u.setRole(Role.valueOf(rs.getString("role")));
        return u;
    }
}