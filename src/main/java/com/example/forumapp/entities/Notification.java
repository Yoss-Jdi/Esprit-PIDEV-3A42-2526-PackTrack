package com.example.forumapp.entities;

import java.time.LocalDateTime;

public class Notification {

    private long id;
    private long recipientId;
    private long actorId;
    private long postId;
    private long commentId;
    private String message;
    private boolean read;
    private LocalDateTime createdAt;

    // Transient relations for display
    private String actorName;
    private String postTitle;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getRecipientId() { return recipientId; }
    public void setRecipientId(long recipientId) { this.recipientId = recipientId; }

    public long getActorId() { return actorId; }
    public void setActorId(long actorId) { this.actorId = actorId; }

    public long getPostId() { return postId; }
    public void setPostId(long postId) { this.postId = postId; }

    public long getCommentId() { return commentId; }
    public void setCommentId(long commentId) { this.commentId = commentId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getActorName() { return actorName; }
    public void setActorName(String actorName) { this.actorName = actorName; }

    public String getPostTitle() { return postTitle; }
    public void setPostTitle(String postTitle) { this.postTitle = postTitle; }

    @Override
    public String toString() {
        return message != null && message.length() > 60 ? message.substring(0, 57) + "..." : String.valueOf(message);
    }
}
