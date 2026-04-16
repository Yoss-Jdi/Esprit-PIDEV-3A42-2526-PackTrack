package com.gestioncolis.controllers;

import com.gestioncolis.models.Utilisateur;
import com.gestioncolis.services.UtilisateurService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.io.IOException;
import java.sql.SQLException;

public class LoginController {

    @FXML private TextField     tfEmail;
    @FXML private PasswordField pfMotDePasse;
    @FXML private Label         lblErreur;
    @FXML private Label         lblRole;

    private final UtilisateurService userService = new UtilisateurService();

    @FXML
    public void initialize() {
        // Permettre la connexion avec la touche Entrée depuis n'importe quel champ
        tfEmail.setOnKeyPressed(this::gererToucheEntree);
        pfMotDePasse.setOnKeyPressed(this::gererToucheEntree);
    }

    private void gererToucheEntree(KeyEvent e) {
        if (e.getCode() == KeyCode.ENTER) seConnecter();
    }

    @FXML
    public void seConnecter() {
        lblErreur.setText("");
        lblRole.setText("");

        String email = tfEmail.getText().trim();
        String mdp   = pfMotDePasse.getText();

        if (email.isBlank()) {
            lblErreur.setText("Veuillez saisir votre adresse e-mail.");
            tfEmail.requestFocus();
            return;
        }
        if (mdp.isBlank()) {
            lblErreur.setText("Veuillez saisir votre mot de passe.");
            pfMotDePasse.requestFocus();
            return;
        }

        try {
            Utilisateur u = userService.connecter(email, mdp);

            if (u == null) {
                lblErreur.setText("❌ Email ou mot de passe incorrect.");
                pfMotDePasse.clear();
                pfMotDePasse.requestFocus();
                return;
            }

            // ── Connexion réussie → rediriger selon le rôle ─────────
            System.out.println("[LOGIN] Connecté : " + u.getDisplayName()
                    + " | Rôle : " + u.getRole());

            String fxmlCible = switch (u.getRole()) {
                case ROLE_ENTREPRISE -> "/fxml/dashboard.fxml";
                case ROLE_LIVREUR    -> "/fxml/dashboard.fxml";
                // ROLE_ADMIN et ROLE_CLIENT → dashboard générique pour l'instant
                default              -> "/fxml/dashboard.fxml";
            };

            naviguerVers(fxmlCible);

        } catch (SQLException ex) {
            lblErreur.setText("Erreur de connexion à la base de données : " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void naviguerVers(String fxml) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            tfEmail.getScene().setRoot(root);
        } catch (IOException e) {
            lblErreur.setText("Erreur navigation : " + e.getMessage());
            e.printStackTrace();
        }
    }
}