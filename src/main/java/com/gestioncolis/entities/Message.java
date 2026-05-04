package com.gestioncolis.entities;

import java.time.LocalDateTime;

public class Message {
    private int idMsg;
    private int conversationId;
    private int senderId;
    private int receiverId;
    private String content;
    private boolean isRead;
    private LocalDateTime createdAt;

    // Noms des utilisateurs (pour l'affichage)
    private String senderName;
    private String receiverName;

    public Message() {}

    public Message(int conversationId, int senderId, int receiverId, String content) {
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.content = content;
        this.isRead = false;
        this.createdAt = LocalDateTime.now();
    }

    // Getters et Setters
    public int getIdMsg() { return idMsg; }
    public void setIdMsg(int idMsg) { this.idMsg = idMsg; }

    public int getConversationId() { return conversationId; }
    public void setConversationId(int conversationId) { this.conversationId = conversationId; }

    public int getSenderId() { return senderId; }
    public void setSenderId(int senderId) { this.senderId = senderId; }

    public int getReceiverId() { return receiverId; }
    public void setReceiverId(int receiverId) { this.receiverId = receiverId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }
}