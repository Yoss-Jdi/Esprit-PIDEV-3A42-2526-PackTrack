package com.gestioncolis.services;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.gestioncolis.entities.Notification;
import com.gestioncolis.utils.DataSource;

public class NotificationService {

    public NotificationService() {
        ensureTable();
    }

    /**
     * Creates the notifications table if it does not exist.
     */
    private void ensureTable() {
        String sql = """
                CREATE TABLE IF NOT EXISTS notifications (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    recipient_id BIGINT NOT NULL,
                    actor_id BIGINT NOT NULL,
                    post_id BIGINT NOT NULL,
                    comment_id BIGINT NOT NULL,
                    message VARCHAR(500) NOT NULL,
                    is_read BOOLEAN DEFAULT FALSE,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (recipient_id) REFERENCES utilisateurs(id_utilisateur),
                    FOREIGN KEY (actor_id) REFERENCES utilisateurs(id_utilisateur),
                    FOREIGN KEY (post_id) REFERENCES posts(id),
                    FOREIGN KEY (comment_id) REFERENCES comments(id) ON DELETE CASCADE
                )
                """;
        try (Statement stmt = DataSource.getInstance().getCnx().createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            System.out.println("Warning: Could not create notifications table: " + e.getMessage());
        }
    }

    /**
     * Creates a new notification for a post owner when someone comments.
     */
    public void createNotification(long recipientId, long actorId, long postId, long commentId, String message) {
        // Don't notify yourself
        if (recipientId == actorId) {
            return;
        }
        String sql = "INSERT INTO notifications (recipient_id, actor_id, post_id, comment_id, message) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = DataSource.getInstance().getCnx().prepareStatement(sql)) {
            stmt.setLong(1, recipientId);
            stmt.setLong(2, actorId);
            stmt.setLong(3, postId);
            stmt.setLong(4, commentId);
            stmt.setString(5, message);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Warning: Could not create notification: " + e.getMessage());
        }
    }

     /**
      * Gets all notifications for a user, ordered by newest first.
      */
     public List<Notification> getNotifications(long userId, int limit) {
         String sql = """
                 SELECT n.id, n.recipient_id, n.actor_id, n.post_id, n.comment_id,
                        n.message, n.is_read, n.created_at,
                        u.nom AS actor_name, p.title AS post_title
                 FROM notifications n
                 JOIN utilisateurs u ON u.id_utilisateur = n.actor_id
                 JOIN posts p ON p.id = n.post_id
                 WHERE n.recipient_id = ?
                 ORDER BY n.created_at DESC
                 LIMIT ?
                 """;
         List<Notification> list = new ArrayList<>();
         try (PreparedStatement stmt = DataSource.getInstance().getCnx().prepareStatement(sql)) {
             stmt.setLong(1, userId);
             stmt.setInt(2, limit);
             try (ResultSet rs = stmt.executeQuery()) {
                 while (rs.next()) {
                     list.add(mapNotification(rs));
                 }
             }
         } catch (SQLException e) {
             // La table notifications n'existe pas ou les colonnes ne correspondent pas
             System.out.println("Warning: Could not load notifications (table may not exist): " + e.getMessage());
         }
         return list;
     }

     /**
      * Gets only unread notifications for a user.
      */
     public List<Notification> getUnreadNotifications(long userId) {
         String sql = """
                 SELECT n.id, n.recipient_id, n.actor_id, n.post_id, n.comment_id,
                        n.message, n.is_read, n.created_at,
                        u.nom AS actor_name, p.title AS post_title
                 FROM notifications n
                 JOIN utilisateurs u ON u.id_utilisateur = n.actor_id
                 JOIN posts p ON p.id = n.post_id
                 WHERE n.recipient_id = ? AND n.is_read = FALSE
                 ORDER BY n.created_at DESC
                 """;
         List<Notification> list = new ArrayList<>();
         try (PreparedStatement stmt = DataSource.getInstance().getCnx().prepareStatement(sql)) {
             stmt.setLong(1, userId);
             try (ResultSet rs = stmt.executeQuery()) {
                 while (rs.next()) {
                     list.add(mapNotification(rs));
                 }
             }
         } catch (SQLException e) {
             // La table notifications n'existe pas ou les colonnes ne correspondent pas
             System.out.println("Warning: Could not load unread notifications (table may not exist): " + e.getMessage());
         }
         return list;
     }

     /**
      * Counts unread notifications for a user (for the badge).
      */
     public long countUnread(long userId) {
         try {
             return DataSource.getInstance().countBySql(
                     "SELECT COUNT(*) FROM notifications WHERE recipient_id = ? AND is_read = FALSE", userId);
         } catch (RuntimeException e) {
             // La table notifications n'existe pas ou les colonnes ne correspondent pas.
             // Retourner 0 au lieu de lever une exception pour éviter de casser l'UI.
             System.out.println("Warning: Could not count unread notifications (table may not exist): " + e.getMessage());
             return 0;
         }
     }

    /**
     * Marks a single notification as read.
     */
    public void markAsRead(long notificationId) {
        String sql = "UPDATE notifications SET is_read = TRUE WHERE id = ?";
        try (PreparedStatement stmt = DataSource.getInstance().getCnx().prepareStatement(sql)) {
            stmt.setLong(1, notificationId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Warning: Could not mark notification as read: " + e.getMessage());
        }
    }

     /**
      * Marks all notifications as read for a user.
      */
     public void markAllAsRead(long userId) {
         String sql = "UPDATE notifications SET is_read = TRUE WHERE recipient_id = ? AND is_read = FALSE";
         try (PreparedStatement stmt = DataSource.getInstance().getCnx().prepareStatement(sql)) {
             stmt.setLong(1, userId);
             stmt.executeUpdate();
         } catch (SQLException e) {
             System.out.println("Warning: Could not mark all notifications as read (table may not exist): " + e.getMessage());
         }
     }

    /**
     * Gets the single most recent notification for a user.
     */
    public Notification getLatestNotification(long userId) {
        List<Notification> list = getNotifications(userId, 1);
        return list.isEmpty() ? null : list.get(0);
    }

    private Notification mapNotification(ResultSet rs) throws SQLException {
        Notification n = new Notification();
        n.setId(rs.getLong("id"));
        n.setRecipientId(rs.getLong("recipient_id"));
        n.setActorId(rs.getLong("actor_id"));
        n.setPostId(rs.getLong("post_id"));
        n.setCommentId(rs.getLong("comment_id"));
        n.setMessage(rs.getString("message"));
        n.setRead(rs.getBoolean("is_read"));
        n.setCreatedAt(DataSource.getInstance().toLocalDateTime(rs, "created_at"));
        n.setActorName(rs.getString("actor_name"));
        n.setPostTitle(rs.getString("post_title"));
        return n;
    }
}
