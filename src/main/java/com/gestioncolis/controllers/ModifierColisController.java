package com.gestioncolis.controllers;

import com.gestioncolis.models.Colis;
import com.gestioncolis.services.ColisService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
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
    @FXML private Label     lblErreur;

    private final ColisService service = new ColisService();
    private Colis colisAModifier;

    public void setColis(Colis c) {
        this.colisAModifier = c;

        if (!"en_attente".equals(c.getStatut())) {
            lblErreur.setText("Modification impossible : le colis est déjà « "
                    + c.getStatut() + " ».");
            desactiverFormulaire();
            return;
        }

        lblTitre.setText("Modifier le colis ");
        lblStatutInfo.setText("Statut actuel : " + c.getStatut());

        tfDescription.setText(c.getDescription());
        tfArticles   .setText(c.getArticles());
        tfDepart     .setText(c.getAdresseDepart());
        tfDestination.setText(c.getAdresseDestination());
        tfPoids      .setText(String.valueOf(c.getPoids()));
        tfDimensions .setText(c.getDimensions());
    }

    private void desactiverFormulaire() {
        lblTitre.setText("Colis non modifiable");
        tfDescription.setDisable(true);
        tfArticles   .setDisable(true);
        tfDepart     .setDisable(true);
        tfDestination.setDisable(true);
        tfPoids      .setDisable(true);
        tfDimensions .setDisable(true);

        //pre-remplissage
        tfDescription.setText(colisAModifier.getDescription());
        tfArticles   .setText(colisAModifier.getArticles());
        tfDepart     .setText(colisAModifier.getAdresseDepart());
        tfDestination.setText(colisAModifier.getAdresseDestination());
        tfPoids      .setText(String.valueOf(colisAModifier.getPoids()));
        tfDimensions .setText(colisAModifier.getDimensions());
    }

    @FXML
    public void enregistrer() {
        lblErreur.setText("");

        if (!"en_attente".equals(colisAModifier.getStatut())) {
            lblErreur.setText("Modification impossible : statut « "
                    + colisAModifier.getStatut() + " ».");
            return;
        }

        if (tfDepart.getText().isBlank() || tfDestination.getText().isBlank()
                || tfPoids.getText().isBlank()) {
            lblErreur.setText("Départ, destination et poids sont obligatoires.");
            return;
        }

        double poids;
        try {
            poids = Double.parseDouble(tfPoids.getText().trim());
        } catch (NumberFormatException e) {
            lblErreur.setText("Le poids doit être un nombre décimal.");
            return;
        }

        colisAModifier.setDescription      (tfDescription.getText().trim());
        colisAModifier.setArticles         (tfArticles.getText().trim());
        colisAModifier.setAdresseDepart    (tfDepart.getText().trim());
        colisAModifier.setAdresseDestination(tfDestination.getText().trim());
        colisAModifier.setPoids            (poids);
        colisAModifier.setDimensions       (tfDimensions.getText().trim());

        try {
            service.modifier(colisAModifier);
            retourListe();
        } catch (Exception e) {
            lblErreur.setText("Erreur : " + e.getMessage());
        }
    }

    @FXML
    public void retourListe() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/listeColis.fxml"));
            tfDescription.getScene().setRoot(root);
        } catch (IOException e) {
            lblErreur.setText("Erreur navigation : " + e.getMessage());
        }
    }
}
