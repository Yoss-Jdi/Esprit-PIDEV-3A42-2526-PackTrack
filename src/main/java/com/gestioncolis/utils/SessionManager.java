package com.gestioncolis.utils;

import com.gestioncolis.models.Utilisateur;

/**
 * Singleton qui conserve l'utilisateur connecté pour toute la session JavaFX.
 *
 * Utilisation :
 *   // À la connexion :
 *   SessionManager.getInstance().setUtilisateurConnecte(user);
 *
 *   // N'importe où dans l'application :
 *   Utilisateur moi = SessionManager.getInstance().getUtilisateurConnecte();
 */
public class SessionManager {

    private static SessionManager instance;
    private Utilisateur utilisateurConnecte;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) instance = new SessionManager();
        return instance;
    }

    public Utilisateur getUtilisateurConnecte() {
        return utilisateurConnecte;
    }

    public void setUtilisateurConnecte(Utilisateur u) {
        this.utilisateurConnecte = u;
    }

    public boolean isConnecte() {
        return utilisateurConnecte != null;
    }

    public void deconnecter() {
        utilisateurConnecte = null;
    }

    // ── Raccourcis pratiques ──────────────────────────────────────────
    public int getIdConnecte() {
        if (!isConnecte()) throw new IllegalStateException("Aucun utilisateur connecté");
        return utilisateurConnecte.getId();
    }

    public boolean isEntreprise() {
        return isConnecte() &&
               utilisateurConnecte.getRole() == Utilisateur.Role.ROLE_ENTREPRISE;
    }

    public boolean isLivreur() {
        return isConnecte() &&
               utilisateurConnecte.getRole() == Utilisateur.Role.ROLE_LIVREUR;
    }

    public boolean isClient() {
        return isConnecte() &&
               utilisateurConnecte.getRole() == Utilisateur.Role.ROLE_CLIENT;
    }
}
