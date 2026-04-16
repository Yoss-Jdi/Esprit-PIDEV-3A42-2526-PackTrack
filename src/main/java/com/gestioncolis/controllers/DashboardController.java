package com.gestioncolis.controllers;

import com.gestioncolis.models.Utilisateur;
import com.gestioncolis.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class DashboardController {

    @FXML private Label lblBonjour;
    @FXML private VBox  carteColis;
    @FXML private VBox  carteLivraisons;

    @FXML
    public void initialize() {
        Utilisateur u = SessionManager.getInstance().getUtilisateurConnecte();
        if (u == null) return;

        lblBonjour.setText("Bonjour, " + u.getDisplayName().trim());

        setCard(carteColis,      false);
        setCard(carteLivraisons, false);

        switch (u.getRole()) {
            case ROLE_ENTREPRISE -> setCard(carteColis, true);
            case ROLE_LIVREUR    -> setCard(carteLivraisons, true);
            case ROLE_CLIENT     -> setCard(carteColis, true);
            case ROLE_ADMIN      -> {
                setCard(carteColis,      true);
                setCard(carteLivraisons, true);
            }
        }
    }

    private void setCard(VBox card, boolean visible) {
        card.setVisible(visible);
        card.setManaged(visible);
    }

    @FXML
    public void ouvrirColis(MouseEvent event) {
        naviguer("/fxml/listeColis.fxml", event);
    }

    @FXML
    public void ouvrirLivraisons(MouseEvent event) {
        naviguer("/fxml/listeLivraisons.fxml", event);
    }

    @FXML
    public void seDeconnecter() {
        SessionManager.getInstance().deconnecter();
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
            lblBonjour.getScene().setRoot(root);
        } catch (IOException e) {
            System.err.println("Erreur déconnexion : " + e.getMessage());
        }
    }

    private void naviguer(String fxml, MouseEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            ((javafx.scene.Node) event.getSource()).getScene().setRoot(root);
        } catch (IOException e) {
            System.err.println("Erreur navigation vers " + fxml + " : " + e.getMessage());
            e.printStackTrace();
        }
    }
}