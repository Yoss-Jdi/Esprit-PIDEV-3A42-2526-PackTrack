package com.gestioncolis.services;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.gestioncolis.entities.Forum;
import com.gestioncolis.entities.Post;
import com.gestioncolis.entities.User;
import com.gestioncolis.utils.DataSource;
import com.gestioncolis.utils.PostImageStorage;

public class PostService {

    private final AuthService auth;
    // MODERATION: Injection du service de modération
    private final ModerationService moderationService;

    public PostService(AuthService auth, ModerationService moderationService) {
        this.auth = auth;
        this.moderationService = moderationService;
        ensurePostImageColumn();
    }

    public List<Post> getPostsByForum(long forumId, String searchText, long viewerId) {
        String sql = """
                SELECT p.id, p.forum_id, f.title AS forum_title,
                       p.author_id, u.nom AS author_name,
                       p.title, p.content, p.image_path, p.created_at, p.updated_at,
                       COUNT(DISTINCT l.id) AS like_count,
                       COUNT(DISTINCT c.id) AS comment_count,
                       MAX(CASE WHEN l.user_id = ? THEN 1 ELSE 0 END) AS liked
                FROM posts p
                JOIN forums f ON f.id = p.forum_id
                JOIN utilisateurs u ON u.id_utilisateur = p.author_id
                LEFT JOIN likes l ON l.target_type = 'POST' AND l.target_id = p.id
                LEFT JOIN comments c ON c.post_id = p.id
                WHERE p.forum_id = ?
                  AND (LOWER(p.title) LIKE ? OR LOWER(p.content) LIKE ? OR LOWER(u.nom) LIKE ?)
                GROUP BY p.id, p.forum_id, f.title, p.author_id, u.nom, p.title, p.content, p.image_path, p.created_at, p.updated_at
                ORDER BY p.updated_at DESC
                """;
        String pattern = "%" + normalize(searchText) + "%";
        List<Post> list = new ArrayList<>();
        try (PreparedStatement stmt = DataSource.getInstance().getCnx().prepareStatement(sql)) {
            stmt.setLong(1, viewerId);
            stmt.setLong(2, forumId);
            stmt.setString(3, pattern);
            stmt.setString(4, pattern);
            stmt.setString(5, pattern);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapPost(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors du chargement des posts.", e);
        }
    }

    public List<Post> getAllPostsForAdmin(long viewerId) {
        auth.requireAdmin();
        String sql = """
                SELECT p.id, p.forum_id, f.title AS forum_title,
                       p.author_id, u.nom AS author_name,
                       p.title, p.content, p.image_path, p.created_at, p.updated_at,
                       COUNT(DISTINCT l.id) AS like_count,
                       COUNT(DISTINCT c.id) AS comment_count,
                       MAX(CASE WHEN l.user_id = ? THEN 1 ELSE 0 END) AS liked
                FROM posts p
                JOIN forums f ON f.id = p.forum_id
                JOIN utilisateurs u ON u.id_utilisateur = p.author_id
                LEFT JOIN likes l ON l.target_type = 'POST' AND l.target_id = p.id
                LEFT JOIN comments c ON c.post_id = p.id
                GROUP BY p.id, p.forum_id, f.title, p.author_id, u.nom, p.title, p.content, p.image_path, p.created_at, p.updated_at
                ORDER BY p.updated_at DESC
                """;
        List<Post> list = new ArrayList<>();
        try (PreparedStatement stmt = DataSource.getInstance().getCnx().prepareStatement(sql)) {
            stmt.setLong(1, viewerId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapPost(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors du chargement des posts admin.", e);
        }
    }

    public List<Post> searchAllPostsForAdmin(String searchText, long viewerId) {
        auth.requireAdmin();
        return searchAllPostsInternal(searchText, viewerId);
    }

    public List<Post> searchAllPostsForUser(String searchText, long viewerId) {
        auth.requireUser();
        return searchAllPostsInternal(searchText, viewerId);
    }

    private List<Post> searchAllPostsInternal(String searchText, long viewerId) {
        String sql = """
                SELECT p.id, p.forum_id, f.title AS forum_title,
                       p.author_id, u.nom AS author_name,
                       p.title, p.content, p.image_path, p.created_at, p.updated_at,
                       COUNT(DISTINCT l.id) AS like_count,
                       COUNT(DISTINCT c.id) AS comment_count,
                       MAX(CASE WHEN l.user_id = ? THEN 1 ELSE 0 END) AS liked
                FROM posts p
                JOIN forums f ON f.id = p.forum_id
                JOIN utilisateurs u ON u.id_utilisateur = p.author_id
                LEFT JOIN likes l ON l.target_type = 'POST' AND l.target_id = p.id
                LEFT JOIN comments c ON c.post_id = p.id
                WHERE LOWER(p.title) LIKE ? OR LOWER(p.content) LIKE ? OR LOWER(u.nom) LIKE ?
                GROUP BY p.id, p.forum_id, f.title, p.author_id, u.nom, p.title, p.content, p.image_path, p.created_at, p.updated_at
                ORDER BY p.updated_at DESC
                """;
        String pattern = "%" + normalize(searchText) + "%";
        List<Post> list = new ArrayList<>();
        try (PreparedStatement stmt = DataSource.getInstance().getCnx().prepareStatement(sql)) {
            stmt.setLong(1, viewerId);
            stmt.setString(2, pattern);
            stmt.setString(3, pattern);
            stmt.setString(4, pattern);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapPost(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la recherche globale des posts.", e);
        }
    }

    public List<Post> getHotPosts(int limit, long viewerId) {
        auth.requireAdmin();
        String sql = """
                SELECT p.id, p.forum_id, f.title AS forum_title,
                       p.author_id, u.nom AS author_name,
                       p.title, p.content, p.image_path, p.created_at, p.updated_at,
                       COUNT(DISTINCT l.id) AS like_count,
                       COUNT(DISTINCT c.id) AS comment_count,
                       MAX(CASE WHEN l.user_id = ? THEN 1 ELSE 0 END) AS liked
                FROM posts p
                JOIN forums f ON f.id = p.forum_id
                JOIN utilisateurs u ON u.id_utilisateur = p.author_id
                LEFT JOIN likes l ON l.target_type = 'POST' AND l.target_id = p.id
                LEFT JOIN comments c ON c.post_id = p.id
                GROUP BY p.id, p.forum_id, f.title, p.author_id, u.nom, p.title, p.content, p.image_path, p.created_at, p.updated_at
                ORDER BY COUNT(DISTINCT c.id) DESC, p.updated_at DESC
                LIMIT ?
                """;
        List<Post> list = new ArrayList<>();
        try (PreparedStatement stmt = DataSource.getInstance().getCnx().prepareStatement(sql)) {
            stmt.setLong(1, viewerId);
            stmt.setInt(2, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapPost(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération des posts populaires.", e);
        }
    }

    public Optional<Post> findById(long postId, long viewerId) {
        String sql = """
                SELECT p.id, p.forum_id, f.title AS forum_title,
                       p.author_id, u.nom AS author_name,
                       p.title, p.content, p.image_path, p.created_at, p.updated_at,
                       COUNT(DISTINCT l.id) AS like_count,
                       COUNT(DISTINCT c.id) AS comment_count,
                       MAX(CASE WHEN l.user_id = ? THEN 1 ELSE 0 END) AS liked
                FROM posts p
                JOIN forums f ON f.id = p.forum_id
                JOIN utilisateurs u ON u.id_utilisateur = p.author_id
                LEFT JOIN likes l ON l.target_type = 'POST' AND l.target_id = p.id
                LEFT JOIN comments c ON c.post_id = p.id
                WHERE p.id = ?
                GROUP BY p.id, p.forum_id, f.title, p.author_id, u.nom, p.title, p.content, p.image_path, p.created_at, p.updated_at
                """;
        try (PreparedStatement stmt = DataSource.getInstance().getCnx().prepareStatement(sql)) {
            stmt.setLong(1, viewerId);
            stmt.setLong(2, postId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapPost(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors du chargement du post.", e);
        }
    }

    public Post savePost(Post draft) {
        User actor = auth.requireUser();

        // MODERATION: Vérifier le contenu avec l'API de modération
        if (moderationService.isInappropriate(draft.getTitle() + " " + draft.getContent())) {
            String violation = moderationService.getViolationSummary(draft.getTitle() + " " + draft.getContent());
            throw new IllegalArgumentException("Contenu détecté comme inapproprié: " + violation);
        }
        if (draft.getForumId() <= 0) {
            throw new RuntimeException("Un forum doit etre selectionne.");
        }
        if (draft.getTitle() == null || draft.getTitle().isBlank()) {
            throw new RuntimeException("Le titre du post est obligatoire.");
        }
        if (draft.getContent() == null || draft.getContent().isBlank()) {
            throw new RuntimeException("Le contenu du post est obligatoire.");
        }

        if (draft.getId() == 0) {
            draft.setAuthorId(actor.getId());
            return createPost(draft, actor.getId());
        } else {
            Post existing = findById(draft.getId(), actor.getId())
                    .orElseThrow(() -> new RuntimeException("Post introuvable."));
            requireCanManage(actor, existing);
            Post updated = updatePost(draft, actor.getId());
            cleanupReplacedImage(existing.getImagePath(), updated.getImagePath());
            return updated;
        }
    }

    public void deletePost(long postId) {
        User actor = auth.requireUser();
        Post existing = findById(postId, actor.getId())
                .orElseThrow(() -> new RuntimeException("Post introuvable."));
        requireCanManage(actor, existing);

        cleanLikesForPost(postId);
        try (PreparedStatement stmt = DataSource.getInstance().getCnx().prepareStatement("DELETE FROM posts WHERE id = ?")) {
            stmt.setLong(1, postId);
            stmt.executeUpdate();
            PostImageStorage.deleteStoredImage(existing.getImagePath());
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la suppression du post.", e);
        }
    }

    public void toggleLike(long postId) {
        User actor = auth.requireUser();
        findById(postId, actor.getId()).orElseThrow(() -> new RuntimeException("Post introuvable."));

        boolean exists = DataSource.getInstance().countBySql(
                "SELECT COUNT(*) FROM likes WHERE user_id = ? AND target_type = 'POST' AND target_id = ?",
                actor.getId(), postId) > 0;

        try {
            if (exists) {
                try (PreparedStatement stmt = DataSource.getInstance().getCnx().prepareStatement(
                        "DELETE FROM likes WHERE user_id = ? AND target_type = 'POST' AND target_id = ?")) {
                    stmt.setLong(1, actor.getId());
                    stmt.setLong(2, postId);
                    stmt.executeUpdate();
                }
            } else {
                try (PreparedStatement stmt = DataSource.getInstance().getCnx().prepareStatement(
                        "INSERT INTO likes (user_id, target_type, target_id) VALUES (?, 'POST', ?)")) {
                    stmt.setLong(1, actor.getId());
                    stmt.setLong(2, postId);
                    stmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors du toggle like post.", e);
        }
    }

    public long countAll() {
        return DataSource.getInstance().countBySql("SELECT COUNT(*) FROM posts");
    }

    public long countByAuthor(long authorId) {
        return DataSource.getInstance().countBySql("SELECT COUNT(*) FROM posts WHERE author_id = ?", authorId);
    }

    private Post createPost(Post draft, long viewerId) {
        String sql = "INSERT INTO posts (forum_id, author_id, title, content, image_path) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = DataSource.getInstance().getCnx().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, draft.getForumId());
            stmt.setLong(2, draft.getAuthorId());
            stmt.setString(3, draft.getTitle().trim());
            stmt.setString(4, draft.getContent().trim());
            stmt.setString(5, normalizeImagePath(draft.getImagePath()));
            stmt.executeUpdate();
            return findById(DataSource.getInstance().extractGeneratedId(stmt), viewerId)
                    .orElseThrow(() -> new RuntimeException("Post cree mais introuvable."));
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la creation du post.", e);
        }
    }

    private Post updatePost(Post draft, long viewerId) {
        String sql = "UPDATE posts SET forum_id = ?, title = ?, content = ?, image_path = ? WHERE id = ?";
        try (PreparedStatement stmt = DataSource.getInstance().getCnx().prepareStatement(sql)) {
            stmt.setLong(1, draft.getForumId());
            stmt.setString(2, draft.getTitle().trim());
            stmt.setString(3, draft.getContent().trim());
            stmt.setString(4, normalizeImagePath(draft.getImagePath()));
            stmt.setLong(5, draft.getId());
            stmt.executeUpdate();
            return findById(draft.getId(), viewerId)
                    .orElseThrow(() -> new RuntimeException("Post mis a jour mais introuvable."));
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la mise a jour du post.", e);
        }
    }

    private void cleanLikesForPost(long postId) {
        try {
            try (PreparedStatement stmt = DataSource.getInstance().getCnx().prepareStatement(
                    "DELETE likes FROM likes JOIN comments ON likes.target_type = 'COMMENT' AND likes.target_id = comments.id WHERE comments.post_id = ?")) {
                stmt.setLong(1, postId);
                stmt.executeUpdate();
            }
            try (PreparedStatement stmt = DataSource.getInstance().getCnx().prepareStatement(
                    "DELETE FROM likes WHERE target_type = 'POST' AND target_id = ?")) {
                stmt.setLong(1, postId);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors du nettoyage des likes du post.", e);
        }
    }

    private void requireCanManage(User actor, Post post) {
        if (!actor.isAdmin() && post.getAuthorId() != actor.getId()) {
            throw new RuntimeException("Seul l'auteur ou un admin peut modifier/supprimer ce post.");
        }
    }

    public boolean canManage(User actor, Post post) {
        return actor != null && post != null && (actor.isAdmin() || post.getAuthorId() == actor.getId());
    }

    private Post mapPost(ResultSet rs) throws SQLException {
        Post p = new Post();
        p.setId(rs.getLong("id"));
        p.setForumId(rs.getLong("forum_id"));
        p.setAuthorId(rs.getLong("author_id"));
        p.setTitle(rs.getString("title"));
        p.setContent(rs.getString("content"));
        p.setImagePath(rs.getString("image_path"));
        p.setCreatedAt(DataSource.getInstance().toLocalDateTime(rs, "created_at"));
        p.setUpdatedAt(DataSource.getInstance().toLocalDateTime(rs, "updated_at"));
        p.setLikeCount(rs.getLong("like_count"));
        p.setCommentCount(rs.getLong("comment_count"));
        p.setLikedByCurrentUser(rs.getInt("liked") == 1);

        Forum forum = new Forum();
        forum.setId(rs.getLong("forum_id"));
        forum.setTitle(rs.getString("forum_title"));
        p.setForum(forum);

        User author = new User();
        author.setId(rs.getLong("author_id"));
        author.setNom(rs.getString("author_name"));
        p.setAuthor(author);

        return p;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private String normalizeImagePath(String imagePath) {
        return imagePath == null || imagePath.isBlank() ? null : imagePath.trim();
    }

    private void cleanupReplacedImage(String previousPath, String nextPath) {
        if (previousPath != null && !previousPath.equals(nextPath)) {
            PostImageStorage.deleteStoredImage(previousPath);
        }
    }

    private void ensurePostImageColumn() {
        String sql = "SHOW COLUMNS FROM posts LIKE 'image_path'";
        try (PreparedStatement stmt = DataSource.getInstance().getCnx().prepareStatement(sql); ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return;
            }
            try (Statement alter = DataSource.getInstance().getCnx().createStatement()) {
                alter.executeUpdate("ALTER TABLE posts ADD COLUMN image_path VARCHAR(500) NULL AFTER content");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la verification du schema des posts.", e);
        }
    }
}
