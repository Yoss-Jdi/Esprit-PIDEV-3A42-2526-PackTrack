package com.gestioncolis.entities;

import java.time.LocalDateTime;

public class Conversation {
    private int idConv;
    private int user1Id;
    private int user2Id;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Informations supplémentaires
    private String contactName;
    private String contactRole;
    private String contactPhoto;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private int unreadCount;
    private boolean isOnline;

    public Conversation() {}

    // Getters et Setters
    public int getIdConv() { return idConv; }
    public void setIdConv(int idConv) { this.idConv = idConv; }

    public int getUser1Id() { return user1Id; }
    public void setUser1Id(int user1Id) { this.user1Id = user1Id; }

    public int getUser2Id() { return user2Id; }
    public void setUser2Id(int user2Id) { this.user2Id = user2Id; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }

    public String getContactRole() { return contactRole; }
    public void setContactRole(String contactRole) { this.contactRole = contactRole; }

    public String getContactPhoto() { return contactPhoto; }
    public void setContactPhoto(String contactPhoto) { this.contactPhoto = contactPhoto; }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public LocalDateTime getLastMessageTime() { return lastMessageTime; }
    public void setLastMessageTime(LocalDateTime lastMessageTime) { this.lastMessageTime = lastMessageTime; }

    public int getUnreadCount() { return unreadCount; }
    public void setUnreadCount(int unreadCount) { this.unreadCount = unreadCount; }

    public boolean isOnline() { return isOnline; }
    public void setOnline(boolean online) { isOnline = online; }
}