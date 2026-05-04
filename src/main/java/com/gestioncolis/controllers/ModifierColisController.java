package com.gestioncolis.controllers;

import com.gestioncolis.models.Colis;
import com.gestioncolis.entities.Utilisateurs;
import com.gestioncolis.services.ColisService;
import com.gestioncolis.utils.MapHelper;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;

import java.io.IOException;

public class ModifierColisController implements DashboardController.UserAware {

    @FXML private Label     lblTitre;
    @FXML private Label     lblStatutInfo;
    @FXML private TextField tfDescription;
    @FXML private TextField tfArticles;
    @FXML private TextField tfDepart;
    @FXML private TextField tfDestination;
    @FXML private TextField tfPoids;
    @FXML private TextField tfDimensions;
    @FXML private Button    btnMapDepart;
    @FXML private Button    btnMapDestination;

    @FXML private Label lblErrDescription;
    @FXML private Label lblErrDepart;
    @FXML private Label lblErrDestination;
    @FXML private Label lblErrPoids;
    @FXML private Label lblErrGlobal;

    private final ColisService service = new ColisService();
    private Colis colisEnCours;
    
    // ✅ NOUVEL ATTRIBUT POUR STOCKER L'UTILISATEUR COURANT
    private Utilisateurs currentUser;

    @FXML
    public void initialize() {
        if (btnMapDepart != null) {
            btnMapDepart.setOnAction(e -> {
                effacerErreur(lblErrDepart);
                MapHelper.ouvrirCarte("📍 Choisir l'adresse de départ", tfDepart);
            });
        }
        if (btnMapDestination != null) {
            btnMapDestination.setOnAction(e -> {
                effacerErreur(lblErrDestination);
                MapHelper.ouvrirCarte("🏁 Choisir l'adresse de destination", tfDestination);
            });
        }
    }

    /**
     * ✅ IMPLÉMENTATION DE L'INTERFACE UserAware
     * Permet au contrôleur d'être notifié de l'utilisateur courant lors du chargement
     */
    @Override
    public void setCurrentUser(Utilisateurs user) {
        this.currentUser = user;
    }


public void setColis(Colis c) {
        this.colisEnCours = c;
        lblTitre.setText("Modifier le colis #" + c.getId());
        lblStatutInfo.setText("Statut actuel : " + c.getStatut());
        tfDescription.setText(c.getDescription() != null ? c.getDescription() : "");
        tfArticles.setText(c.getArticles() != null ? c.getArticles() : "");
        tfDepart.setText(c.getAdresseDepart() != null ? c.getAdresseDepart() : "");
        tfDestination.setText(c.getAdresseDestination() != null ? c.getAdresseDestination() : "");
        tfPoids.setText(String.valueOf(c.getPoids()));
        tfDimensions.setText(c.getDimensions() != null ? c.getDimensions() : "");
    }

    @FXML
    public void enregistrer() {
        effacerToutesLesErreurs();
        boolean valide = true;

        String desc = tfDescription.getText().trim();
        if (!desc.isBlank() && desc.length() < 5) {
            afficherErreur(lblErrDescription, "Minimum 5 caractères.");
            valide = false;
        }
        if (tfDepart.getText().isBlank()) {
            afficherErreur(lblErrDepart, "L'adresse de départ est obligatoire.");
            valide = false;
        }
        if (tfDestination.getText().isBlank()) {
            afficherErreur(lblErrDestination, "L'adresse de destination est obligatoire.");
            valide = false;
        }
        double poids = 0;
        if (tfPoids.getText().isBlank()) {
            afficherErreur(lblErrPoids, "Le poids est obligatoire.");
            valide = false;
        } else {
            try {
                poids = Double.parseDouble(tfPoids.getText().trim());
                if (poids <= 0) { afficherErreur(lblErrPoids, "Le poids doit être positif."); valide = false; }
                else if (poids >= 1000) { afficherErreur(lblErrPoids, "Max 1000 kg."); valide = false; }
            } catch (NumberFormatException e) {
                afficherErreur(lblErrPoids, "Nombre décimal requis (ex: 2.5).");
                valide = false;
            }
        }
        if (!valide) return;

        colisEnCours.setDescription(desc);
        colisEnCours.setArticles(tfArticles.getText().trim());
        colisEnCours.setAdresseDepart(tfDepart.getText().trim());
        colisEnCours.setAdresseDestination(tfDestination.getText().trim());
        colisEnCours.setPoids(poids);
        colisEnCours.setDimensions(tfDimensions.getText().trim());

        try {
            service.modifier(colisEnCours);
            retourListe();
        } catch (Exception e) {
            afficherErreur(lblErrGlobal, "Erreur : " + e.getMessage());
        }
    }

    @FXML
    public void retourListe() {
        try {
            Utilisateurs u = currentUser != null ? currentUser : com.gestioncolis.utils.SessionManager.getInstance().getUtilisateurConnecte();
            String target = (u != null && u.getRole() == com.gestioncolis.enums.Role.ADMIN)
                    ? "/views/DashboardLayout.fxml"
                    : "/fxml/listeColis.fxml";
            FXMLLoader loader = new FXMLLoader(getClass().getResource(target));
            Parent root = loader.load();
            if (u != null) {
                Object ctrl = loader.getController();
                if (ctrl instanceof DashboardController.UserAware ua) {
                    ua.setCurrentUser(u);
                }
            }
            tfDescription.getScene().setRoot(root);
        } catch (IOException e) {
            afficherErreur(lblErrGlobal, "Erreur navigation : " + e.getMessage());
        }
    }

    private void afficherErreur(Label lbl, String msg) {
        if (lbl == null) return;
        lbl.setText("⚠ " + msg);
        lbl.setVisible(true);
        lbl.setManaged(true);
    }
    private void effacerErreur(Label lbl) {
        if (lbl == null) return;
        lbl.setText("");
        lbl.setVisible(false);
        lbl.setManaged(false);
    }
    private void effacerToutesLesErreurs() {
        effacerErreur(lblErrDescription);
        effacerErreur(lblErrDepart);
        effacerErreur(lblErrDestination);
        effacerErreur(lblErrPoids);
        effacerErreur(lblErrGlobal);
    }
}