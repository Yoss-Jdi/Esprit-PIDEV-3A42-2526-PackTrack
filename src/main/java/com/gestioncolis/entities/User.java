package com.gestioncolis.entities;

import java.util.ArrayList;
import java.util.List;
import com.gestioncolis.enums.Role;

public class User {

    private long id;
    private String nom;
    private String email;
    private String passwordHash;
    private Role role;

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

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    
    // ...existing code...
    
    public boolean isAdmin() { return role == Role.ADMIN; }


    @Override
    public String toString() { return nom + " (" + role + ")"; }
}
