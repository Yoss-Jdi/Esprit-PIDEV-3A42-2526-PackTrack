package com.gestioncolis.entities;

import com.gestioncolis.enums.Role;

import java.time.LocalDateTime;

public class Utilisateurs {

    private int idUtilisateur;
    private String email;
    private String motDePasse;
    private String nom;
    private String prenom;
    private String telephone;   // nullable
    private Role role;
    private String photo;       // nullable
    private LocalDateTime createdAt;

    // ─── Constructeur complet (sans id, pour INSERT) ───────────────────────────
    public Utilisateurs(String email, String motDePasse, String nom, String prenom,
                        String telephone, Role role, String photo, LocalDateTime createdAt) {
        this.email      = email;
        this.motDePasse = motDePasse;
        this.nom        = nom;
        this.prenom     = prenom;
        this.telephone  = telephone;
        this.role       = role;
        this.photo      = photo;
        this.createdAt  = createdAt;
    }

    // ─── Constructeur complet (avec id, pour SELECT) ───────────────────────────
    public Utilisateurs(int idUtilisateur, String email, String motDePasse, String nom,
                        String prenom, String telephone, Role role, String photo,
                        LocalDateTime createdAt) {
        this.idUtilisateur = idUtilisateur;
        this.email         = email;
        this.motDePasse    = motDePasse;
        this.nom           = nom;
        this.prenom        = prenom;
        this.telephone     = telephone;
        this.role          = role;
        this.photo         = photo;
        this.createdAt     = createdAt;
    }

    // ─── Constructeur vide ─────────────────────────────────────────────────────
    public Utilisateurs() {}

    // ─── Getters & Setters ─────────────────────────────────────────────────────
    public int getIdUtilisateur()              { return idUtilisateur; }
    public void setIdUtilisateur(int id)       { this.idUtilisateur = id; }

    public String getEmail()                   { return email; }
    public void setEmail(String email)         { this.email = email; }

    public String getMotDePasse()              { return motDePasse; }
    public void setMotDePasse(String mdp)      { this.motDePasse = mdp; }

    public String getNom()                     { return nom; }
    public void setNom(String nom)             { this.nom = nom; }

    public String getPrenom()                  { return prenom; }
    public void setPrenom(String prenom)       { this.prenom = prenom; }

    public String getTelephone()               { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public Role getRole()                      { return role; }
    public void setRole(Role role)             { this.role = role; }

    public String getPhoto()                   { return photo; }
    public void setPhoto(String photo)         { this.photo = photo; }

    public LocalDateTime getCreatedAt()                    { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt)      { this.createdAt = createdAt; }

    // ─── toString ──────────────────────────────────────────────────────────────
    @Override
    public String toString() {
        return "Utilisateurs{" +
                "id="          + idUtilisateur +
                ", email='"    + email         + '\'' +
                ", nom='"      + nom           + '\'' +
                ", prenom='"   + prenom        + '\'' +
                ", telephone='"+ telephone     + '\'' +
                ", role="      + role          +
                ", photo='"    + photo         + '\'' +
                ", createdAt=" + createdAt     +
                '}';
    }
}