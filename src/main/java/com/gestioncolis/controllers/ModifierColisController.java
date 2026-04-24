package com.gestioncolis.controllers;

import com.gestioncolis.models.Colis;
import com.gestioncolis.services.ColisService;
import com.gestioncolis.utils.MapHelper;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.io.IOException;

public class ModifierColisController {

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

    // ── Labels d'erreur individuels ───────────────────────────────────
    @FXML private Label lblErrDescription;
    @FXML private Label lblErrDepart;
    @FXML private Label lblErrDestination;
    @FXML private Label lblErrPoids;
    @FXML private Label lblErrGlobal;

    private final ColisService colisService = new ColisService();
    private Colis colisEnCours;

    @FXML
    public void initialize() {
        configurerBoutonsMap();
    }

    // ─────────────────────────────────────────────────────────────────
    // Injection du colis à modifier (appelée depuis ListeColisController)
    // ─────────────────────────────────────────────────────────────────

    public void setColis(Colis colis) {
        this.colisEnCours = colis;

        lblTitre.setText("Modifier le colis #" + colis.getId());
        lblStatutInfo.setText("Statut actuel : " + colis.getStatut());

        tfDescription.setText(nvl(colis.getDescription()));
        tfArticles   .setText(nvl(colis.getArticles()));
        tfDepart     .setText(nvl(colis.getAdresseDepart()));
        tfDestination.setText(nvl(colis.getAdresseDestination()));
        tfPoids      .setText(String.valueOf(colis.getPoids()));
        tfDimensions .setText(nvl(colis.getDimensions()));
    }

    // ─────────────────────────────────────────────────────────────────
    // Boutons carte — délégation à MapHelper
    // ─────────────────────────────────────────────────────────────────

    private void configurerBoutonsMap() {
        if (btnMapDepart != null) {
            btnMapDepart.setOnAction(e -> {
                MapHelper.ouvrirCarte("Choisir l'adresse de départ", tfDepart);
                if (!tfDepart.getText().isBlank()) effacerErreur(lblErrDepart);
            });
        }
        if (btnMapDestination != null) {
            btnMapDestination.setOnAction(e -> {
                MapHelper.ouvrirCarte("Choisir l'adresse de destination", tfDestination);
                if (!tfDestination.getText().isBlank()) effacerErreur(lblErrDestination);
            });
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Validation et enregistrement
    // ─────────────────────────────────────────────────────────────────

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
                if (poids <= 0) {
                    afficherErreur(lblErrPoids, "Le poids doit être positif.");
                    valide = false;
                } else if (poids >= 1000) {
                    afficherErreur(lblErrPoids, "Le poids ne peut pas dépasser 1000 kg.");
                    valide = false;
                }
            } catch (NumberFormatException e) {
                afficherErreur(lblErrPoids, "Le poids doit être un nombre décimal (ex : 2.5).");
                valide = false;
            }
        }

        if (!valide) return;

        // Appliquer les modifications sur l'objet existant
        colisEnCours.setDescription(desc);
        colisEnCours.setArticles(tfArticles.getText().trim());
        colisEnCours.setAdresseDepart(tfDepart.getText().trim());
        colisEnCours.setAdresseDestination(tfDestination.getText().trim());
        colisEnCours.setPoids(poids);
        colisEnCours.setDimensions(tfDimensions.getText().trim());

        try {
            colisService.modifier(colisEnCours);
            retourListe();
        } catch (Exception e) {
            afficherErreur(lblErrGlobal, "Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void retourListe() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/listeColis.fxml"));
            tfDescription.getScene().setRoot(root);
        } catch (IOException e) {
            afficherErreur(lblErrGlobal, "Erreur navigation : " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────

    private String nvl(String s) {
        return s != null ? s : "";
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