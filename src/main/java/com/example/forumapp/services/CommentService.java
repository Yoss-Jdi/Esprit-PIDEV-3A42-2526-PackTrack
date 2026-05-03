package com.example.forumapp.services;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.example.forumapp.entities.Comment;
import com.example.forumapp.entities.Post;
import com.example.forumapp.entities.User;
import com.example.forumapp.utils.DataSource;

public class CommentService {

    private final AuthService auth;
    // MODERATION: Injection du service de modération
    private final ModerationService moderationService;
    // NOTIFICATIONS: Injection du service de notifications
    private final NotificationService notificationService;

    public CommentService(AuthService auth, ModerationService moderationService, NotificationService notificationService) {
        this.auth = auth;
        this.moderationService = moderationService;
        this.notificationService = notificationService;
    }

    public List<Comment> getCommentsByPost(long postId, long viewerId) {
        String sql = """
                SELECT c.id, c.post_id, p.title AS post_title,
                       c.author_id, u.nom AS author_name,
                       c.content, c.created_at, c.updated_at,
                       COUNT(DISTINCT l.id) AS like_count,
                       MAX(CASE WHEN l.user_id = ? THEN 1 ELSE 0 END) AS liked
                FROM comments c
                JOIN posts p ON p.id = c.post_id
                JOIN users u ON u.id = c.author_id
                LEFT JOIN likes l ON l.target_type = 'COMMENT' AND l.target_id = c.id
                WHERE c.post_id = ?
                GROUP BY c.id, c.post_id, p.title, c.author_id, u.nom, c.content, c.created_at, c.updated_at
                ORDER BY c.updated_at DESC
                """;
        List<Comment> list = new ArrayList<>();
        try (PreparedStatement stmt = DataSource.getInstance().getCnx().prepareStatement(sql)) {
            stmt.setLong(1, viewerId);
            stmt.setLong(2, postId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapComment(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors du chargement des commentaires.", e);
        }
    }

    public List<Comment> getAllCommentsForAdmin(long viewerId) {
        auth.requireAdmin();
        String sql = """
                SELECT c.id, c.post_id, p.title AS post_title,
                       c.author_id, u.nom AS author_name,
                       c.content, c.created_at, c.updated_at,
                       COUNT(DISTINCT l.id) AS like_count,
                       MAX(CASE WHEN l.user_id = ? THEN 1 ELSE 0 END) AS liked
                FROM comments c
                JOIN posts p ON p.id = c.post_id
                JOIN users u ON u.id = c.author_id
                LEFT JOIN likes l ON l.target_type = 'COMMENT' AND l.target_id = c.id
                GROUP BY c.id, c.post_id, p.title, c.author_id, u.nom, c.content, c.created_at, c.updated_at
                ORDER BY c.updated_at DESC
                """;
        List<Comment> list = new ArrayList<>();
        try (PreparedStatement stmt = DataSource.getInstance().getCnx().prepareStatement(sql)) {
            stmt.setLong(1, viewerId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapComment(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors du chargement admin des commentaires.", e);
        }
    }

    public Optional<Comment> findById(long commentId, long viewerId) {
        String sql = """
                SELECT c.id, c.post_id, p.title AS post_title,
                       c.author_id, u.nom AS author_name,
                       c.content, c.created_at, c.updated_at,
                       COUNT(DISTINCT l.id) AS like_count,
                       MAX(CASE WHEN l.user_id = ? THEN 1 ELSE 0 END) AS liked
                FROM comments c
                JOIN posts p ON p.id = c.post_id
                JOIN users u ON u.id = c.author_id
                LEFT JOIN likes l ON l.target_type = 'COMMENT' AND l.target_id = c.id
                WHERE c.id = ?
                GROUP BY c.id, c.post_id, p.title, c.author_id, u.nom, c.content, c.created_at, c.updated_at
                """;
        try (PreparedStatement stmt = DataSource.getInstance().getCnx().prepareStatement(sql)) {
            stmt.setLong(1, viewerId);
            stmt.setLong(2, commentId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapComment(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors du chargement du commentaire.", e);
        }
    }

    public Comment saveComment(Comment draft) {
        User actor = auth.requireUser();

        // MODERATION: Vérifier le contenu du commentaire avec l'API de modération
        if (moderationService.isInappropriate(draft.getContent())) {
            String violation = moderationService.getViolationSummary(draft.getContent());
            throw new IllegalArgumentException("Commentaire détecté comme inapproprié: " + violation);
        }
        if (draft.getPostId() <= 0) {
            throw new RuntimeException("Un post doit etre selectionne.");
        }
        if (draft.getContent() == null || draft.getContent().isBlank()) {
            throw new RuntimeException("Le contenu du commentaire est obligatoire.");
        }

        if (draft.getId() == 0) {
            draft.setAuthorId(actor.getId());
            Comment created = createComment(draft, actor.getId());

            // NOTIFICATIONS: Notifier le propriétaire du post
            try {
                long postOwnerId = getPostAuthorId(draft.getPostId());
                if (postOwnerId > 0 && postOwnerId != actor.getId()) {
                    String message = actor.getNom() + " a commenté sur votre post";
                    notificationService.createNotification(
                            postOwnerId, actor.getId(), draft.getPostId(), created.getId(), message);
                }
            } catch (Exception e) {
                System.out.println("Warning: Could not send notification: " + e.getMessage());
            }

            return created;
        } else {
            Comment existing = findById(draft.getId(), actor.getId())
                    .orElseThrow(() -> new RuntimeException("Commentaire introuvable."));
            requireCanManage(actor, existing);
            return updateComment(draft, actor.getId());
        }
    }

    public void deleteComment(long commentId) {
        User actor = auth.requireUser();
        Comment existing = findById(commentId, actor.getId())
                .orElseThrow(() -> new RuntimeException("Commentaire introuvable."));
        requireCanManage(actor, existing);

        try {
            try (PreparedStatement stmt = DataSource.getInstance().getCnx().prepareStatement(
                    "DELETE FROM likes WHERE target_type = 'COMMENT' AND target_id = ?")) {
                stmt.setLong(1, commentId);
                stmt.executeUpdate();
            }
            try (PreparedStatement stmt = DataSource.getInstance().getCnx().prepareStatement("DELETE FROM comments WHERE id = ?")) {
                stmt.setLong(1, commentId);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la suppression du commentaire.", e);
        }
    }

    public void toggleLike(long commentId) {
        User actor = auth.requireUser();
        findById(commentId, actor.getId()).orElseThrow(() -> new RuntimeException("Commentaire introuvable."));

        boolean exists = DataSource.getInstance().countBySql(
                "SELECT COUNT(*) FROM likes WHERE user_id = ? AND target_type = 'COMMENT' AND target_id = ?",
                actor.getId(), commentId) > 0;

        try {
            if (exists) {
                try (PreparedStatement stmt = DataSource.getInstance().getCnx().prepareStatement(
                        "DELETE FROM likes WHERE user_id = ? AND target_type = 'COMMENT' AND target_id = ?")) {
                    stmt.setLong(1, actor.getId());
                    stmt.setLong(2, commentId);
                    stmt.executeUpdate();
                }
            } else {
                try (PreparedStatement stmt = DataSource.getInstance().getCnx().prepareStatement(
                        "INSERT INTO likes (user_id, target_type, target_id) VALUES (?, 'COMMENT', ?)")) {
                    stmt.setLong(1, actor.getId());
                    stmt.setLong(2, commentId);
                    stmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors du toggle like commentaire.", e);
        }
    }

    public long countAll() {
        return DataSource.getInstance().countBySql("SELECT COUNT(*) FROM comments");
    }

    public long countByAuthor(long authorId) {
        return DataSource.getInstance().countBySql("SELECT COUNT(*) FROM comments WHERE author_id = ?", authorId);
    }

    private Comment createComment(Comment draft, long viewerId) {
        String sql = "INSERT INTO comments (post_id, author_id, content) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = DataSource.getInstance().getCnx().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, draft.getPostId());
            stmt.setLong(2, draft.getAuthorId());
            stmt.setString(3, draft.getContent().trim());
            stmt.executeUpdate();
            return findById(DataSource.getInstance().extractGeneratedId(stmt), viewerId)
                    .orElseThrow(() -> new RuntimeException("Commentaire cree mais introuvable."));
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la creation du commentaire.", e);
        }
    }

    private Comment updateComment(Comment draft, long viewerId) {
        String sql = "UPDATE comments SET post_id = ?, content = ? WHERE id = ?";
        try (PreparedStatement stmt = DataSource.getInstance().getCnx().prepareStatement(sql)) {
            stmt.setLong(1, draft.getPostId());
            stmt.setString(2, draft.getContent().trim());
            stmt.setLong(3, draft.getId());
            stmt.executeUpdate();
            return findById(draft.getId(), viewerId)
                    .orElseThrow(() -> new RuntimeException("Commentaire mis a jour mais introuvable."));
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la mise a jour du commentaire.", e);
        }
    }

    private void requireCanManage(User actor, Comment comment) {
        if (!actor.isAdmin() && comment.getAuthorId() != actor.getId()) {
            throw new RuntimeException("Seul l'auteur ou un admin peut modifier/supprimer ce commentaire.");
        }
    }

    public boolean canManage(User actor, Comment comment) {
        return actor != null && comment != null && (actor.isAdmin() || comment.getAuthorId() == actor.getId());
    }

    private Comment mapComment(ResultSet rs) throws SQLException {
        Comment c = new Comment();
        c.setId(rs.getLong("id"));
        c.setPostId(rs.getLong("post_id"));
        c.setAuthorId(rs.getLong("author_id"));
        c.setContent(rs.getString("content"));
        c.setCreatedAt(DataSource.getInstance().toLocalDateTime(rs, "created_at"));
        c.setUpdatedAt(DataSource.getInstance().toLocalDateTime(rs, "updated_at"));
        c.setLikeCount(rs.getLong("like_count"));
        c.setLikedByCurrentUser(rs.getInt("liked") == 1);

        Post post = new Post();
        post.setId(rs.getLong("post_id"));
        post.setTitle(rs.getString("post_title"));
        c.setPost(post);

        User author = new User();
        author.setId(rs.getLong("author_id"));
        author.setNom(rs.getString("author_name"));
        c.setAuthor(author);

        return c;
    }

    /**
     * Retrieves the author ID of a post (used for notifications).
     */
    private long getPostAuthorId(long postId) {
        String sql = "SELECT author_id FROM posts WHERE id = ?";
        try (PreparedStatement stmt = DataSource.getInstance().getCnx().prepareStatement(sql)) {
            stmt.setLong(1, postId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("author_id");
                }
            }
        } catch (SQLException e) {
            System.out.println("Warning: Could not get post author: " + e.getMessage());
        }
        return -1;
    }
}
