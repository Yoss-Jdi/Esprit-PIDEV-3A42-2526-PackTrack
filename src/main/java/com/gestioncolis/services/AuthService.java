package com.gestioncolis.services;

import com.gestioncolis.entities.User;
import com.gestioncolis.entities.UserRole;
import com.gestioncolis.utils.DataSource;
import com.gestioncolis.utils.PasswordUtils;

import java.sql.*;
import java.util.Optional;

public class AuthService {

    private User currentUser;

    public AuthService() {
    }

    public User login(String email, String password) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            throw new RuntimeException("Email et mot de passe sont obligatoires.");
        }

        Optional<User> found = findByEmail(email.trim().toLowerCase());
        if (found.isEmpty()) throw new RuntimeException("Identifiants invalides.");

        User user = found.get();
        if (!PasswordUtils.matches(password, user.getPasswordHash())) {
            throw new RuntimeException("Identifiants invalides.");
        }

        this.currentUser = user;
        return user;
    }

    public void logout() {
        this.currentUser = null;
    }

    public boolean isAuthenticated() {
        return currentUser != null;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public User requireUser() {
        if (currentUser == null) throw new RuntimeException("Aucun utilisateur connecte.");
        return currentUser;
    }

    public User requireAdmin() {
        User user = requireUser();
        if (!user.isAdmin()) throw new RuntimeException("Action reservee a l'administrateur.");
        return user;
    }

    private Optional<User> findByEmail(String email) {
        String sql = "SELECT id, nom, email, password, role FROM users WHERE email = ?";
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

    private User mapUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getLong("id"));
        u.setNom(rs.getString("nom"));
        u.setEmail(rs.getString("email"));
        u.setPasswordHash(rs.getString("password"));
        u.setRole(UserRole.valueOf(rs.getString("role")));
        return u;
    }
}
