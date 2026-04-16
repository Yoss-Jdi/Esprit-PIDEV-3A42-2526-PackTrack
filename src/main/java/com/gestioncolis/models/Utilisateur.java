package com.gestioncolis.models;

import java.time.LocalDateTime;

public class Utilisateur {

    public enum Role {
        ROLE_ADMIN("ROLE_ADMIN"),
        ROLE_ENTREPRISE("ROLE_ENTREPRISE"),
        ROLE_LIVREUR("ROLE_LIVREUR"),
        ROLE_CLIENT("ROLE_CLIENT");

        private final String value;

        Role(String value) { this.value = value; }

        public String getValue() { return value; }

        public static Role fromString(String s) {
            if (s == null) return ROLE_CLIENT;
            String normalized = s.trim().toUpperCase();
            // Accepte "ROLE_CLIENT" ET "CLIENT" (format base de données)
            for (Role r : values()) {
                if (r.name().equals(normalized)) return r;               // ROLE_CLIENT
                if (r.name().replace("ROLE_", "").equals(normalized)) return r; // CLIENT
                if (r.value.equalsIgnoreCase(s.trim())) return r;
            }
            return ROLE_CLIENT;
        }
    }

    private int           id;
    private String        email;
    private String        motDePasse;
    private String        nom;
    private String        prenom;
    private String        telephone;
    private Role          role;
    private String        photo;
    private LocalDateTime createdAt;

    // ── Constructeur complet (mapping depuis ResultSet) ──────────────
    public Utilisateur(int id, String email, String motDePasse,
                       String nom, String prenom, String telephone,
                       Role role, String photo, LocalDateTime createdAt) {
        this.id         = id;
        this.email      = email;
        this.motDePasse = motDePasse;
        this.nom        = nom;
        this.prenom     = prenom;
        this.telephone  = telephone;
        this.role       = role;
        this.photo      = photo;
        this.createdAt  = createdAt;
    }

    // ── Getters / Setters ────────────────────────────────────────────
    public int           getId()           { return id; }
    public void          setId(int id)     { this.id = id; }

    public String        getEmail()                    { return email; }
    public void          setEmail(String email)        { this.email = email; }

    public String        getMotDePasse()               { return motDePasse; }
    public void          setMotDePasse(String mdp)     { this.motDePasse = mdp; }

    public String        getNom()                      { return nom; }
    public void          setNom(String nom)            { this.nom = nom; }

    public String        getPrenom()                   { return prenom; }
    public void          setPrenom(String prenom)      { this.prenom = prenom; }

    public String        getTelephone()                { return telephone; }
    public void          setTelephone(String tel)      { this.telephone = tel; }

    public Role          getRole()                     { return role; }
    public void          setRole(Role role)            { this.role = role; }

    public String        getPhoto()                    { return photo; }
    public void          setPhoto(String photo)        { this.photo = photo; }

    public LocalDateTime getCreatedAt()                { return createdAt; }
    public void          setCreatedAt(LocalDateTime d) { this.createdAt = d; }

    // ── Méthodes utilitaires ─────────────────────────────────────────
    public String getDisplayName() {
        return (prenom != null ? prenom : "") + " " + (nom != null ? nom : "");
    }

    /** Affiché dans les ComboBox */
    @Override
    public String toString() {
        return getDisplayName().trim() + " (" + email + ")";
    }
}
