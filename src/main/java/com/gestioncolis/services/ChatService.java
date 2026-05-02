package com.gestioncolis.services;

import com.gestioncolis.entities.Conversation;
import com.gestioncolis.entities.Message;
import com.gestioncolis.entities.Utilisateurs;
import com.gestioncolis.utils.MyDataBase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ChatService {

    private final Connection cnx = MyDataBase.getInstance().getConx();

    public int getOrCreateConversation(int userId1, int userId2) throws SQLException {
        String query = "SELECT id_conv FROM conversations WHERE (user1_id = ? AND user2_id = ?) OR (user1_id = ? AND user2_id = ?)";
        PreparedStatement ps = cnx.prepareStatement(query);
        ps.setInt(1, userId1);
        ps.setInt(2, userId2);
        ps.setInt(3, userId2);
        ps.setInt(4, userId1);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            return rs.getInt("id_conv");
        }

        String insert = "INSERT INTO conversations (user1_id, user2_id) VALUES (?, ?)";
        PreparedStatement psInsert = cnx.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS);
        psInsert.setInt(1, userId1);
        psInsert.setInt(2, userId2);
        psInsert.executeUpdate();

        ResultSet generatedKeys = psInsert.getGeneratedKeys();
        if (generatedKeys.next()) {
            return generatedKeys.getInt(1);
        }
        return -1;
    }

    public Message sendMessage(int conversationId, int senderId, int receiverId, String content) throws SQLException {
        String query = "INSERT INTO messages (conversation_id, sender_id, receiver_id, content) VALUES (?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
        ps.setInt(1, conversationId);
        ps.setInt(2, senderId);
        ps.setInt(3, receiverId);
        ps.setString(4, content);
        ps.executeUpdate();

        String updateConv = "UPDATE conversations SET updated_at = CURRENT_TIMESTAMP WHERE id_conv = ?";
        PreparedStatement psUpdate = cnx.prepareStatement(updateConv);
        psUpdate.setInt(1, conversationId);
        psUpdate.executeUpdate();

        ResultSet generatedKeys = ps.getGeneratedKeys();
        if (generatedKeys.next()) {
            Message msg = new Message();
            msg.setIdMsg(generatedKeys.getInt(1));
            msg.setConversationId(conversationId);
            msg.setSenderId(senderId);
            msg.setReceiverId(receiverId);
            msg.setContent(content);
            msg.setCreatedAt(LocalDateTime.now());
            return msg;
        }
        return null;
    }

    public List<Conversation> getUserConversations(int userId) throws SQLException {
        List<Conversation> conversations = new ArrayList<>();

        String query = """
            SELECT c.*, 
                   CASE 
                       WHEN c.user1_id = ? THEN u2.id_utilisateur
                       ELSE u1.id_utilisateur
                   END as contact_id,
                   CASE 
                       WHEN c.user1_id = ? THEN CONCAT(u2.prenom, ' ', u2.nom)
                       ELSE CONCAT(u1.prenom, ' ', u1.nom)
                   END as contact_name,
                   CASE 
                       WHEN c.user1_id = ? THEN u2.role
                       ELSE u1.role
                   END as contact_role,
                   CASE 
                       WHEN c.user1_id = ? THEN u2.photo
                       ELSE u1.photo
                   END as contact_photo,
                   (SELECT content FROM messages WHERE conversation_id = c.id_conv ORDER BY created_at DESC LIMIT 1) as last_msg,
                   (SELECT created_at FROM messages WHERE conversation_id = c.id_conv ORDER BY created_at DESC LIMIT 1) as last_msg_time,
                   (SELECT COUNT(*) FROM messages WHERE conversation_id = c.id_conv AND receiver_id = ? AND is_read = FALSE) as unread_count
            FROM conversations c
            JOIN utilisateurs u1 ON u1.id_utilisateur = c.user1_id
            JOIN utilisateurs u2 ON u2.id_utilisateur = c.user2_id
            WHERE c.user1_id = ? OR c.user2_id = ?
            ORDER BY c.updated_at DESC
        """;

        PreparedStatement ps = cnx.prepareStatement(query);
        ps.setInt(1, userId);
        ps.setInt(2, userId);
        ps.setInt(3, userId);
        ps.setInt(4, userId);
        ps.setInt(5, userId);
        ps.setInt(6, userId);
        ps.setInt(7, userId);

        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            Conversation conv = new Conversation();
            conv.setIdConv(rs.getInt("id_conv"));
            conv.setUser1Id(rs.getInt("user1_id"));
            conv.setUser2Id(rs.getInt("user2_id"));
            conv.setContactName(rs.getString("contact_name"));
            conv.setContactRole(rs.getString("contact_role"));
            conv.setContactPhoto(rs.getString("contact_photo"));
            conv.setLastMessage(rs.getString("last_msg"));
            conv.setLastMessageTime(rs.getTimestamp("last_msg_time") != null ? rs.getTimestamp("last_msg_time").toLocalDateTime() : null);
            conv.setUnreadCount(rs.getInt("unread_count"));
            conversations.add(conv);
        }
        return conversations;
    }

    public List<Message> getConversationMessages(int conversationId, int currentUserId) throws SQLException {
        List<Message> messages = new ArrayList<>();

        String query = """
            SELECT m.*, 
                   u1.prenom as sender_prenom, u1.nom as sender_nom,
                   u2.prenom as receiver_prenom, u2.nom as receiver_nom
            FROM messages m
            JOIN utilisateurs u1 ON u1.id_utilisateur = m.sender_id
            JOIN utilisateurs u2 ON u2.id_utilisateur = m.receiver_id
            WHERE m.conversation_id = ?
            ORDER BY m.created_at ASC
        """;

        PreparedStatement ps = cnx.prepareStatement(query);
        ps.setInt(1, conversationId);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            Message msg = new Message();
            msg.setIdMsg(rs.getInt("id_msg"));
            msg.setConversationId(rs.getInt("conversation_id"));
            msg.setSenderId(rs.getInt("sender_id"));
            msg.setReceiverId(rs.getInt("receiver_id"));
            msg.setContent(rs.getString("content"));
            msg.setRead(rs.getBoolean("is_read"));
            msg.setCreatedAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null);
            msg.setSenderName(rs.getString("sender_prenom") + " " + rs.getString("sender_nom"));
            msg.setReceiverName(rs.getString("receiver_prenom") + " " + rs.getString("receiver_nom"));
            messages.add(msg);
        }

        markMessagesAsRead(conversationId, currentUserId);
        return messages;
    }

    private void markMessagesAsRead(int conversationId, int currentUserId) throws SQLException {
        String query = "UPDATE messages SET is_read = TRUE WHERE conversation_id = ? AND receiver_id = ? AND is_read = FALSE";
        PreparedStatement ps = cnx.prepareStatement(query);
        ps.setInt(1, conversationId);
        ps.setInt(2, currentUserId);
        ps.executeUpdate();
    }

    public List<Utilisateurs> searchUsers(String query, int currentUserId) throws SQLException {
        List<Utilisateurs> users = new ArrayList<>();
        String searchQuery = "SELECT * FROM utilisateurs WHERE (nom LIKE ? OR prenom LIKE ? OR email LIKE ?) AND id_utilisateur != ?";
        PreparedStatement ps = cnx.prepareStatement(searchQuery);
        String searchPattern = "%" + query + "%";
        ps.setString(1, searchPattern);
        ps.setString(2, searchPattern);
        ps.setString(3, searchPattern);
        ps.setInt(4, currentUserId);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            Utilisateurs u = new Utilisateurs();
            u.setIdUtilisateur(rs.getInt("id_utilisateur"));
            u.setEmail(rs.getString("email"));
            u.setNom(rs.getString("nom"));
            u.setPrenom(rs.getString("prenom"));
            u.setTelephone(rs.getString("telephone"));
            u.setRole(com.gestioncolis.enums.Role.valueOf(rs.getString("role")));
            u.setPhoto(rs.getString("photo"));
            users.add(u);
        }
        return users;
    }

    // NOUVELLES MÉTHODES

    public void deleteConversation(int conversationId) throws SQLException {
        String query = "DELETE FROM conversations WHERE id_conv = ?";
        PreparedStatement ps = cnx.prepareStatement(query);
        ps.setInt(1, conversationId);
        ps.executeUpdate();
    }

    public void editMessage(int messageId, String newContent) throws SQLException {
        String query = "UPDATE messages SET content = ?, edited_at = CURRENT_TIMESTAMP WHERE id_msg = ?";
        PreparedStatement ps = cnx.prepareStatement(query);
        ps.setString(1, newContent);
        ps.setInt(2, messageId);
        ps.executeUpdate();
    }

    public void deleteMessage(int messageId) throws SQLException {
        String query = "DELETE FROM messages WHERE id_msg = ?";
        PreparedStatement ps = cnx.prepareStatement(query);
        ps.setInt(1, messageId);
        ps.executeUpdate();
    }
}