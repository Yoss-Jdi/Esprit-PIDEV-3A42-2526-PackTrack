package com.example.forumapp.entities;

import java.util.ArrayList;
import java.util.List;

public class User {

    private long id;
    private String nom;
    private String email;
    private String passwordHash;
    private UserRole role;

    private List<Post> posts = new ArrayList<>();
    private List<Comment> comments = new ArrayList<>();

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }

    public List<Post> getPosts() { return posts; }
    public void setPosts(List<Post> posts) { this.posts = posts; }

    public List<Comment> getComments() { return comments; }
    public void setComments(List<Comment> comments) { this.comments = comments; }

    public boolean isAdmin() { return role == UserRole.ADMIN; }

    @Override
    public String toString() { return nom + " (" + role + ")"; }
}
