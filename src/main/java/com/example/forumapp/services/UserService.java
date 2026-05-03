package com.example.forumapp.services;

import com.example.forumapp.entities.User;
import com.example.forumapp.entities.UserRole;
import com.example.forumapp.utils.DataSource;
import com.example.forumapp.utils.PasswordUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserService {

    private final AuthService auth;
    private final PostService postService;
    private final CommentService commentService;

    public UserService(AuthService auth, PostService postService, CommentService commentService) {
        this.auth = auth;
        this.postService = postService;
        this.commentService = commentService;
    }

    public List<User> getAllUsers() {
        auth.requireAdmin();
        String sql = "SELECT id, nom, email, password, role FROM users ORDER BY role DESC, nom ASC";
        List<User> list = new ArrayList<>();
        try (PreparedStatement stmt = DataSource.getInstance().getCnx().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) list.add(mapUser(rs));
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors du chargement des utilisateurs.", e);
        }
    }

    public Optional<User> findById(long userId) {
        String sql = "SELECT id, nom, email, password, role FROM users WHERE id = ?";
        try (PreparedStatement stmt = DataSource.getInstance().getCnx().prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapUser(rs));
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la recherche utilisateur.", e);
        }
    }

    public Optional<User> findByEmailForAdmin(String email) {
        auth.requireAdmin();
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

    public Optional<User> getMostActiveUser() {
        auth.requireAdmin();
        String sql = """
            SELECT u.id, u.nom, u.email, u.password, u.role,
                   (SELECT COUNT(*) FROM posts p WHERE p.author_id = u.id) +
                   (SELECT COUNT(*) FROM comments c WHERE c.author_id = u.id) AS total_activity
            FROM users u
            ORDER BY total_activity DESC
            LIMIT 1
        """;
        try (PreparedStatement stmt = DataSource.getInstance().getCnx().prepareStatement(sql)) {
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapUser(rs));
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la recherche de l'utilisateur le plus actif.", e);
        }
    }

    public User saveUser(User draft, String rawPassword) {
        auth.requireAdmin();
        if (draft.getNom() == null || draft.getNom().isBlank()) throw new RuntimeException("Le nom est obligatoire.");
        if (draft.getEmail() == null || draft.getEmail().isBlank()) throw new RuntimeException("L'email est obligatoire.");
        if (draft.getRole() == null) throw new RuntimeException("Le role est obligatoire.");

        User existing = draft.getId() > 0 ? findById(draft.getId())
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable.")) : null;

        checkEmailUniqueness(draft.getEmail().trim().toLowerCase(), existing);

        if (existing == null) {
            if (rawPassword == null || rawPassword.isBlank()) throw new RuntimeException("Le mot de passe est obligatoire a la creation.");
            return createUser(draft, rawPassword);
        }

        if (existing.isAdmin() && draft.getRole() != existing.getRole() && countByRole(existing.getRole()) <= 1) {
            throw new RuntimeException("Impossible de retirer le dernier administrateur.");
        }

        return updateUser(draft, rawPassword, existing);
    }

    public void deleteUser(long userId) {
        User actor = auth.requireAdmin();
        if (actor.getId() == userId) throw new RuntimeException("Un administrateur ne peut pas supprimer son propre compte.");

        User existing = findById(userId).orElseThrow(() -> new RuntimeException("Utilisateur introuvable."));
        if (existing.isAdmin() && countByRole(UserRole.ADMIN) <= 1) {
            throw new RuntimeException("Impossible de supprimer le dernier administrateur.");
        }
        if (postService.countByAuthor(userId) > 0 || commentService.countByAuthor(userId) > 0) {
            throw new RuntimeException("Supprimez d'abord les posts/commentaires de cet utilisateur.");
        }

        try (PreparedStatement stmt = DataSource.getInstance().getCnx().prepareStatement("DELETE FROM users WHERE id = ?")) {
            stmt.setLong(1, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la suppression de l'utilisateur.", e);
        }
    }

    public long countAll() {
        return DataSource.getInstance().countBySql("SELECT COUNT(*) FROM users");
    }

    public long countLikes() {
        return DataSource.getInstance().countBySql("SELECT COUNT(*) FROM likes");
    }

    private long countByRole(UserRole role) {
        return DataSource.getInstance().countBySql("SELECT COUNT(*) FROM users WHERE role = ?", role.name());
    }

    private void checkEmailUniqueness(String email, User existing) {
        String sql = "SELECT id FROM users WHERE LOWER(email) = ?";
        try (PreparedStatement stmt = DataSource.getInstance().getCnx().prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    long foundId = rs.getLong("id");
                    if (existing == null || foundId != existing.getId()) {
                        throw new RuntimeException("Cet email est deja utilise.");
                    }
                }
            }
        } catch (SQLException e) {
            if (e.getMessage().contains("email")) throw new RuntimeException(e.getMessage());
            throw new RuntimeException("Erreur lors de la verification de l'email.", e);
        }
    }

    private User createUser(User draft, String rawPassword) {
        String sql = "INSERT INTO users (nom, email, password, role) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = DataSource.getInstance().getCnx().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, draft.getNom().trim());
            stmt.setString(2, draft.getEmail().trim().toLowerCase());
            stmt.setString(3, PasswordUtils.hash(rawPassword.trim()));
            stmt.setString(4, draft.getRole().name());
            stmt.executeUpdate();
            return findById(DataSource.getInstance().extractGeneratedId(stmt))
                    .orElseThrow(() -> new RuntimeException("Utilisateur cree mais introuvable."));
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la creation de l'utilisateur.", e);
        }
    }

    private User updateUser(User draft, String rawPassword, User existing) {
        String sql = "UPDATE users SET nom = ?, email = ?, password = ?, role = ? WHERE id = ?";
        String hash = (rawPassword == null || rawPassword.isBlank())
                ? existing.getPasswordHash()
                : PasswordUtils.hash(rawPassword.trim());
        try (PreparedStatement stmt = DataSource.getInstance().getCnx().prepareStatement(sql)) {
            stmt.setString(1, draft.getNom().trim());
            stmt.setString(2, draft.getEmail().trim().toLowerCase());
            stmt.setString(3, hash);
            stmt.setString(4, draft.getRole().name());
            stmt.setLong(5, draft.getId());
            stmt.executeUpdate();
            return findById(draft.getId())
                    .orElseThrow(() -> new RuntimeException("Utilisateur mis a jour mais introuvable."));
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la mise a jour de l'utilisateur.", e);
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
