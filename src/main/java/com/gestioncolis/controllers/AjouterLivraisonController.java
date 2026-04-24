package com.gestioncolis.controllers;

import com.gestioncolis.models.Colis;
import com.gestioncolis.models.Livraison;
import com.gestioncolis.services.ColisService;
import com.gestioncolis.services.LivraisonService;
import com.gestioncolis.services.PredictionResult;
import com.gestioncolis.services.PredictionService;
import com.gestioncolis.utils.SessionManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class AjouterLivraisonController {

    // ── Champs FXML ───────────────────────────────────────────────────
    @FXML private Label  lblLivreur;
    @FXML private Label  lblErreur;
    @FXML private Label  lblSelectionInfo;

    // Bandeau IA
    @FXML private Label  lblIaStatut;
    @FXML private Label  lblIaDistance;
    @FXML private Label  lblIaDuree;
    @FXML private Button btnEnregistrer;

    @FXML private TableView<Colis>           tableColisDisponibles;
    @FXML private TableColumn<Colis, String> colCDesc;
    @FXML private TableColumn<Colis, String> colCDep;
    @FXML private TableColumn<Colis, String> colCDest;
    @FXML private TableColumn<Colis, String> colCPoid;
    @FXML private TableColumn<Colis, String> colCMontant;

    // ── Services ──────────────────────────────────────────────────────
    private final LivraisonService  livraisonService  = new LivraisonService();
    private final ColisService      colisService      = new ColisService();
    private final PredictionService predictionService = new PredictionService();

    // ── État interne ──────────────────────────────────────────────────
    /** Dernière prédiction ML réussie — injectée dans la livraison à la confirmation. */
    private PredictionResult dernierePrediction = null;
    /** Tâche asynchrone en cours (annulable si changement de sélection rapide). */
    private Task<PredictionResult> taskCourante = null;

    // ─────────────────────────────────────────────────────────────────
    // Initialisation
    // ─────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        // Nom du livreur connecté
        if (SessionManager.getInstance().isConnecte()) {
            lblLivreur.setText(
                    SessionManager.getInstance().getUtilisateurConnecte().getDisplayName());
        } else {
            lblLivreur.setText("(mode dev)");
        }

        // Colonnes tableau
        colCDesc   .setCellValueFactory(d -> new SimpleStringProperty(
                nvl(d.getValue().getDescription(), "—")));
        colCDep    .setCellValueFactory(d -> new SimpleStringProperty(
                nvl(d.getValue().getAdresseDepart(), "—")));
        colCDest   .setCellValueFactory(d -> new SimpleStringProperty(
                nvl(d.getValue().getAdresseDestination(), "—")));
        colCPoid   .setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getPoids() + " kg"));
        colCMontant.setCellValueFactory(d -> new SimpleStringProperty(
                String.format("%.2f DT", d.getValue().calculerMontant())));

        // Surbrillance ligne sélectionnée
        tableColisDisponibles.setRowFactory(tv -> {
            TableRow<Colis> row = new TableRow<>();
            row.selectedProperty().addListener((obs, was, isNow) ->
                    row.setStyle(isNow ? "-fx-background-color: #d6eaf8;" : ""));
            return row;
        });

        // ── Listener sélection → déclenche la prédiction ML ──────────
        tableColisDisponibles.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, sel) -> {
                    if (sel != null) {
                        afficherResumeColis(sel);
                        lancerPrediction(sel);
                    } else {
                        lblSelectionInfo.setText("");
                        reinitialiserBandeauIA();
                    }
                });

        // Vérification santé ML (non bloquante)
        verifierServiceML();

        chargerColisDisponibles();
    }

    // ─────────────────────────────────────────────────────────────────
    // Chargement des colis disponibles
    // ─────────────────────────────────────────────────────────────────

    private void chargerColisDisponibles() {
        try {
            List<Colis> dispo = colisService.getColisDisponibles();
            tableColisDisponibles.setItems(FXCollections.observableArrayList(dispo));
        } catch (SQLException e) {
            lblErreur.setText("Erreur chargement colis : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Résumé colis sélectionné
    // ─────────────────────────────────────────────────────────────────

    private void afficherResumeColis(Colis sel) {
        lblSelectionInfo.setText(
                nvl(sel.getDescription(), "colis")
                        + "  ·  " + sel.getAdresseDepart()
                        + " → " + sel.getAdresseDestination()
                        + "  ·  " + sel.getPoids() + " kg"
                        + "  ·  " + String.format("%.2f DT", sel.calculerMontant()));
        lblErreur.setText("");
    }

    // ─────────────────────────────────────────────────────────────────
    // Prédiction ML asynchrone (JavaFX Task)
    // ─────────────────────────────────────────────────────────────────

    private void verifierServiceML() {
        Thread t = new Thread(() -> {
            boolean dispo = predictionService.estDisponible();
            if (!dispo) {
                Platform.runLater(() -> setStatutIA(
                        "⚠ Service ML hors ligne — estimations indisponibles", "#e67e22"));
            }
        }, "ml-health");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Annule toute tâche en cours, réinitialise l'UI, puis démarre
     * une nouvelle prédiction dans un thread daemon.
     */
    private void lancerPrediction(Colis colis) {
        // Annuler la tâche précédente si encore en cours
        if (taskCourante != null && taskCourante.isRunning()) {
            taskCourante.cancel(true);
        }

        // Reset état
        dernierePrediction = null;
        if (btnEnregistrer != null) btnEnregistrer.setDisable(true);
        setStatutIA("🔄 Analyse IA en cours…", "#2980b9");
        lblIaDistance.setText("");
        lblIaDuree.setText("");

        LocalDateTime maintenant = LocalDateTime.now();

        taskCourante = new Task<>() {
            @Override
            protected PredictionResult call() {
                return predictionService.predire(
                        colis.getAdresseDepart(),
                        colis.getAdresseDestination(),
                        colis.getPoids(),
                        maintenant);
            }
        };

        taskCourante.setOnSucceeded(ev -> {
            PredictionResult r = taskCourante.getValue();
            dernierePrediction = r;
            if (r.isSucces()) {
                afficherPredictionSucces(r);
            } else {
                afficherPredictionEchec(r.getErreur());
            }
            if (btnEnregistrer != null) btnEnregistrer.setDisable(false);
        });

        taskCourante.setOnFailed(ev -> {
            afficherPredictionEchec("Erreur inattendue");
            if (btnEnregistrer != null) btnEnregistrer.setDisable(false);
        });

        taskCourante.setOnCancelled(ev -> { /* silencieux */ });

        Thread t = new Thread(taskCourante, "ml-predict");
        t.setDaemon(true);
        t.start();
    }

    private void afficherPredictionSucces(PredictionResult r) {
        setStatutIA("✅ Estimation IA disponible", "#27ae60");
        lblIaDistance.setText(String.format("📍 Distance estimée : %.2f km", r.getDistanceKm()));
        lblIaDistance.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1e8449;");
        lblIaDuree.setText("⏱ Durée estimée : " + r.getDureeFormatee());
        lblIaDuree.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1e8449;");
    }

    private void afficherPredictionEchec(String raison) {
        setStatutIA("⚠ Estimation indisponible — distance et durée seront à 0", "#e67e22");
        lblIaDistance.setText("Raison : " + raison);
        lblIaDistance.setStyle("-fx-font-size: 11px; -fx-text-fill: #e67e22;");
        lblIaDuree.setText("");
    }

    private void reinitialiserBandeauIA() {
        if (taskCourante != null && taskCourante.isRunning()) taskCourante.cancel(true);
        dernierePrediction = null;
        setStatutIA("Sélectionnez un colis pour lancer l'analyse IA", "#888888");
        lblIaDistance.setText("");
        lblIaDuree.setText("");
        if (btnEnregistrer != null) btnEnregistrer.setDisable(false);
    }

    private void setStatutIA(String texte, String couleur) {
        lblIaStatut.setText(texte);
        lblIaStatut.setStyle("-fx-font-size: 12px; -fx-text-fill: " + couleur + ";");
    }

    // ─────────────────────────────────────────────────────────────────
    // Enregistrement de la livraison
    // ─────────────────────────────────────────────────────────────────

    @FXML
    public void enregistrer() {
        lblErreur.setText("");

        Colis colis = tableColisDisponibles.getSelectionModel().getSelectedItem();
        if (colis == null) {
            lblErreur.setText("Veuillez sélectionner un colis dans la liste.");
            return;
        }
        if (!SessionManager.getInstance().isConnecte()) {
            lblErreur.setText("Erreur : aucun utilisateur connecté.");
            return;
        }

        int livreurId = SessionManager.getInstance().getIdConnecte();
        Livraison liv = new Livraison(colis.getId(), livreurId, 0);

        // ── Injection des valeurs ML ──────────────────────────────────
        if (dernierePrediction != null && dernierePrediction.isSucces()) {
            liv.setDistanceKm(dernierePrediction.getDistanceKm());
            liv.setDureeEstimeeMinutes(dernierePrediction.getDureeMinutes());
        } else {
            // Fallback : 0 (le service ML était indisponible)
            liv.setDistanceKm(0);
            liv.setDureeEstimeeMinutes(0);
        }

        try {
            livraisonService.ajouter(liv);
            retourListe();
        } catch (Exception e) {
            lblErreur.setText("Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void retourListe() {
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("/fxml/listeLivraisons.fxml"));
            tableColisDisponibles.getScene().setRoot(root);
        } catch (IOException e) {
            lblErreur.setText("Erreur navigation : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String nvl(String s, String fallback) {
        return (s == null || s.isBlank()) ? fallback : s;
    }
}