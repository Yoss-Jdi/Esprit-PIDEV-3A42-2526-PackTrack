package com.gestioncolis.services;

import com.gestioncolis.entities.Forum;
import com.gestioncolis.utils.DataSource;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ForumService {

    private final AuthService auth;

    public ForumService(AuthService auth) {
        this.auth = auth;
    }

    public List<Forum> getForums(String searchText) {
        String sql = """
                SELECT f.id, f.title, f.description, f.created_at, f.updated_at,
                       COUNT(p.id) AS post_count
                FROM forums f
                LEFT JOIN posts p ON p.forum_id = f.id
                WHERE LOWER(f.title) LIKE ? OR LOWER(f.description) LIKE ?
                GROUP BY f.id, f.title, f.description, f.created_at, f.updated_at
                ORDER BY f.updated_at DESC, f.title ASC
                """;

        String pattern = "%" + normalize(searchText) + "%";
        List<Forum> list = new ArrayList<>();
        try (PreparedStatement stmt = DataSource.getInstance().getCnx().prepareStatement(sql)) {
            stmt.setString(1, pattern);
            stmt.setString(2, pattern);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapForum(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors du chargement des forums.", e);
        }
    }

    public Optional<Forum> findById(long forumId) {
        String sql = """
                SELECT f.id, f.title, f.description, f.created_at, f.updated_at,
                       COUNT(p.id) AS post_count
                FROM forums f
                LEFT JOIN posts p ON p.forum_id = f.id
                WHERE f.id = ?
                GROUP BY f.id, f.title, f.description, f.created_at, f.updated_at
                """;
        try (PreparedStatement stmt = DataSource.getInstance().getCnx().prepareStatement(sql)) {
            stmt.setLong(1, forumId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapForum(rs));
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors du chargement du forum.", e);
        }
    }

    public Forum saveForum(Forum draft) {
        auth.requireAdmin();
        if (draft.getTitle() == null || draft.getTitle().isBlank())
            throw new RuntimeException("Le titre du forum est obligatoire.");
        if (draft.getDescription() == null || draft.getDescription().isBlank())
            throw new RuntimeException("La description du forum est obligatoire.");

        if (draft.getId() == 0) {
            return createForum(draft);
        } else {
            return updateForum(draft);
        }
    }

    public void deleteForum(long forumId) {
        auth.requireAdmin();
        findById(forumId).orElseThrow(() -> new RuntimeException("Forum introuvable."));
        cleanLikesForForum(forumId);
        try (PreparedStatement stmt = DataSource.getInstance().getCnx().prepareStatement("DELETE FROM forums WHERE id = ?")) {
            stmt.setLong(1, forumId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la suppression du forum.", e);
        }
    }

    public long countAll() {
        return DataSource.getInstance().countBySql("SELECT COUNT(*) FROM forums");
    }

    private Forum createForum(Forum draft) {
        String sql = "INSERT INTO forums (title, description) VALUES (?, ?)";
        try (PreparedStatement stmt = DataSource.getInstance().getCnx().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, draft.getTitle().trim());
            stmt.setString(2, draft.getDescription().trim());
            stmt.executeUpdate();
            return findById(DataSource.getInstance().extractGeneratedId(stmt))
                    .orElseThrow(() -> new RuntimeException("Forum cree mais introuvable."));
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la creation du forum.", e);
        }
    }

    private Forum updateForum(Forum draft) {
        String sql = "UPDATE forums SET title = ?, description = ? WHERE id = ?";
        try (PreparedStatement stmt = DataSource.getInstance().getCnx().prepareStatement(sql)) {
            stmt.setString(1, draft.getTitle().trim());
            stmt.setString(2, draft.getDescription().trim());
            stmt.setLong(3, draft.getId());
            stmt.executeUpdate();
            return findById(draft.getId())
                    .orElseThrow(() -> new RuntimeException("Forum mis a jour mais introuvable."));
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la mise a jour du forum.", e);
        }
    }

    private void cleanLikesForForum(long forumId) {
        String sqlCommentLikes = """
                DELETE likes FROM likes
                JOIN comments ON likes.target_type = 'COMMENT' AND likes.target_id = comments.id
                JOIN posts ON comments.post_id = posts.id
                WHERE posts.forum_id = ?
                """;
        String sqlPostLikes = """
                DELETE likes FROM likes
                JOIN posts ON likes.target_type = 'POST' AND likes.target_id = posts.id
                WHERE posts.forum_id = ?
                """;
        try {
            try (PreparedStatement stmt = DataSource.getInstance().getCnx().prepareStatement(sqlCommentLikes)) {
                stmt.setLong(1, forumId);
                stmt.executeUpdate();
            }
            try (PreparedStatement stmt = DataSource.getInstance().getCnx().prepareStatement(sqlPostLikes)) {
                stmt.setLong(1, forumId);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors du nettoyage des likes du forum.", e);
        }
    }

    private Forum mapForum(ResultSet rs) throws SQLException {
        Forum f = new Forum();
        f.setId(rs.getLong("id"));
        f.setTitle(rs.getString("title"));
        f.setDescription(rs.getString("description"));
        f.setCreatedAt(DataSource.getInstance().toLocalDateTime(rs, "created_at"));
        f.setUpdatedAt(DataSource.getInstance().toLocalDateTime(rs, "updated_at"));
        f.setPostCount(rs.getLong("post_count"));
        return f;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}
