
package com.gestioncolis.utils;

import com.gestioncolis.entities.Utilisateurs;
import com.gestioncolis.enums.Role;

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
    private Utilisateurs utilisateurConnecte;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) instance = new SessionManager();
        return instance;
    }

    public Utilisateurs getUtilisateurConnecte() {
        return utilisateurConnecte;
    }

    public void setUtilisateurConnecte(Utilisateurs u) {
        this.utilisateurConnecte = u;
    }

    public boolean isConnecte() {
        return utilisateurConnecte != null;
    }

    public void deconnecter() {
        utilisateurConnecte = null;
    }

    // ── Raccourcis ────────────────────────────────────────────────────────────
    public int getIdConnecte() {
        if (!isConnecte()) throw new IllegalStateException("Aucun utilisateur connecté");
        return utilisateurConnecte.getIdUtilisateur();  // ← était getId()
    }

    public boolean isEntreprise() {
        return isConnecte() && utilisateurConnecte.getRole() == Role.ENTREPRISE;
    }

    public boolean isLivreur() {
        return isConnecte() && utilisateurConnecte.getRole() == Role.LIVREUR;
    }

    public boolean isClient() {
        return isConnecte() && utilisateurConnecte.getRole() == Role.CLIENT;
    }

    // ── Méthode utilitaire pour l'affichage (remplace getDisplayName()) ───────
    // Utilisateurs n'a pas de getDisplayName() → on le calcule ici
    public String getDisplayName() {
        if (!isConnecte()) return "";
        String prenom = utilisateurConnecte.getPrenom() != null ? utilisateurConnecte.getPrenom() : "";
        String nom    = utilisateurConnecte.getNom()    != null ? utilisateurConnecte.getNom()    : "";
        return (prenom + " " + nom).trim();
    }
}
