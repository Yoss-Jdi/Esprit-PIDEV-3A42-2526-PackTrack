package com.esprit.controllers;

import com.esprit.services.PredictionService;
import com.esprit.utils.MyDataBase;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

public class FactureStatsController {

    // ── Graphique 1 : CA réel ─────────────────────────────────────────────────
    @FXML private LineChart<String, Number> lineChartReel;

    // ── Graphique 2 : prédiction ML ───────────────────────────────────────────
    @FXML private LineChart<String, Number> lineChartPrediction;
    @FXML private Label                     labelChargementGraph;

    // ── Prédiction 1 mois ─────────────────────────────────────────────────────
    @FXML private DatePicker        datePickerPrediction;
    @FXML private Button            btnPredire;
    @FXML private Label             labelResultatCA;
    @FXML private Label             labelMarge;
    @FXML private Label             labelModele;
    @FXML private ProgressIndicator progressPrediction;

    private final PredictionService predictionService = new PredictionService();

    // Retry config
    private static final int MAX_TENTATIVES = 10;   // essais max
    private static final int DELAI_MS       = 2000; // 2 secondes entre chaque essai

    // ─────────────────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        lineChartReel.setAnimated(false);
        lineChartReel.setCreateSymbols(true);
        lineChartPrediction.setAnimated(false);
        lineChartPrediction.setCreateSymbols(true);

        // 1. CA réel depuis MySQL
        loadCAReelHistorique();

        // 2. Prédiction ML avec retry automatique
        chargerGraphiquePredictionAvecRetry();

        // 3. DatePicker
        datePickerPrediction.setValue(LocalDate.now().plusMonths(1));
        progressPrediction.setVisible(false);

        datePickerPrediction.setDayCellFactory(dp -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (date.isBefore(LocalDate.now())) {
                    setDisable(true);
                    setStyle("-fx-background-color: #f0f0f0;");
                }
            }
        });
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GRAPHIQUE 1 — CA RÉEL depuis MySQL
    // ══════════════════════════════════════════════════════════════════════════
    private void loadCAReelHistorique() {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("CA reel");

        try {
            Connection cnx = MyDataBase.getInstance().getConnection();
            String sql =
                    "SELECT YEAR(dateEmission)  AS annee, " +
                            "       MONTH(dateEmission) AS mois, " +
                            "       SUM(montantTTC)     AS ca " +
                            "FROM factures " +
                            "WHERE dateEmission IS NOT NULL " +
                            "  AND statut != 'Annulee' " +
                            "GROUP BY YEAR(dateEmission), MONTH(dateEmission) " +
                            "ORDER BY YEAR(dateEmission), MONTH(dateEmission)";

            PreparedStatement ps = cnx.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                int    mois  = rs.getInt("mois");
                int    annee = rs.getInt("annee");
                double ca    = rs.getDouble("ca");
                series.getData().add(
                        new XYChart.Data<>(getMonthName(mois) + " " + annee, ca)
                );
            }

            lineChartReel.getData().clear();
            lineChartReel.getData().add(series);

            Platform.runLater(() -> {
                if (series.getNode() != null) {
                    series.getNode().setStyle(
                            "-fx-stroke: #2563eb; -fx-stroke-width: 2.5px;");
                }
                series.getData().forEach(d -> {
                    if (d.getNode() != null) {
                        d.getNode().setStyle(
                                "-fx-background-color: #2563eb, white;" +
                                        "-fx-background-radius: 5px; -fx-padding: 5px;");
                    }
                });
            });

        } catch (Exception e) {
            System.out.println("Erreur CA reel : " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GRAPHIQUE 2 — PRÉDICTION ML avec retry automatique
    // ══════════════════════════════════════════════════════════════════════════
    private void chargerGraphiquePredictionAvecRetry() {
        new Thread(() -> {

            //  Attendre que Flask soit prêt (retry jusqu'à MAX_TENTATIVES fois)
            for (int tentative = 1; tentative <= MAX_TENTATIVES; tentative++) {

                final int t = tentative;

                // Mise à jour du label en cours
                Platform.runLater(() ->
                        labelChargementGraph.setText(
                                "Connexion au serveur ML... (" + t + "/" + MAX_TENTATIVES + ")"
                        )
                );

                // Attendre avant d'essayer
                try { Thread.sleep(DELAI_MS); } catch (InterruptedException ignored) {}

                // Tenter de récupérer les prédictions
                List<double[]> predictions = predictionService.predirePlusieursMois(6);
                List<String>   labels      = predictionService.labelsPlusieursMois(6);

                if (!predictions.isEmpty()) {
                    // Flask répond → afficher le graphique
                    final List<double[]> predFinal  = predictions;
                    final List<String>   labelFinal = labels;

                    Platform.runLater(() -> {
                        labelChargementGraph.setVisible(false);
                        afficherGraphiquePrediction(predFinal, labelFinal);
                    });
                    return; // sortir du thread
                }

                System.out.println("[ML] Tentative " + t + "/" + MAX_TENTATIVES
                        + " — Flask pas encore prêt...");
            }

            //  Toutes les tentatives épuisées
            Platform.runLater(() -> {
                labelChargementGraph.setText(
                        "Serveur ML non disponible apres " + MAX_TENTATIVES + " tentatives."
                );
                labelChargementGraph.setStyle(
                        "-fx-font-size: 12px; -fx-text-fill: #ef4444;");
                labelChargementGraph.setVisible(true);
            });

        }).start();
    }

    // ─── Affichage du graphique prédiction ────────────────────────────────────
    private void afficherGraphiquePrediction(List<double[]> predictions, List<String> labels) {
        XYChart.Series<String, Number> seriesPred = new XYChart.Series<>();
        seriesPred.setName("CA predit");

        for (int i = 0; i < predictions.size(); i++) {
            double[] p     = predictions.get(i);
            String   label = i < labels.size()
                    ? labels.get(i)
                    : getMonthName((int) p[0]) + " " + (int) p[1];
            seriesPred.getData().add(new XYChart.Data<>(label, p[2]));
        }

        lineChartPrediction.getData().clear();
        lineChartPrediction.getData().add(seriesPred);

        // Style orange pointillés
        Platform.runLater(() -> {
            if (seriesPred.getNode() != null) {
                seriesPred.getNode().setStyle(
                        "-fx-stroke: #f97316;" +
                                "-fx-stroke-width: 2.5px;" +
                                "-fx-stroke-dash-array: 10 6;"
                );
            }
            seriesPred.getData().forEach(d -> {
                if (d.getNode() != null) {
                    d.getNode().setStyle(
                            "-fx-background-color: #f97316, white;" +
                                    "-fx-background-radius: 5px; -fx-padding: 5px;"
                    );
                }
            });
        });
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PRÉDICTION 1 MOIS — bouton
    // ══════════════════════════════════════════════════════════════════════════
    @FXML
    private void lancerPrediction() {
        LocalDate date = datePickerPrediction.getValue();

        if (date == null) {
            labelResultatCA.setText("Choisissez un mois !");
            labelResultatCA.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 13px;");
            return;
        }

        int mois  = date.getMonthValue();
        int annee = date.getYear();

        btnPredire.setDisable(true);
        btnPredire.setText("...");
        progressPrediction.setVisible(true);
        labelResultatCA.setText("Calcul en cours...");
        labelResultatCA.setStyle("-fx-text-fill: #7c3aed; -fx-font-style: italic;");
        labelMarge.setText("");
        labelModele.setText("");

        new Thread(() -> {
            double ca  = predictionService.predireCA(mois, annee);
            double mae = predictionService.getMAE();

            Platform.runLater(() -> {
                progressPrediction.setVisible(false);
                btnPredire.setDisable(false);
                btnPredire.setText("Predire");

                if (ca < 0) {
                    labelResultatCA.setText("Serveur ML non disponible.");
                    labelResultatCA.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12px;");
                    return;
                }

                String nomMois = date.getMonth()
                        .getDisplayName(TextStyle.FULL, Locale.FRENCH);

                labelResultatCA.setText(String.format("%.2f TND", ca));
                labelResultatCA.setStyle(
                        "-fx-text-fill: #7c3aed; -fx-font-size: 26px; -fx-font-weight: bold;");

                if (mae > 0) {
                    labelMarge.setText(String.format(
                            "Intervalle estime : [%.0f - %.0f] TND",
                            Math.max(0, ca - mae), ca + mae));
                    labelMarge.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 12px;");
                }

                labelModele.setText(
                        String.format("Prediction pour %s %d", nomMois, annee));
                labelModele.setStyle(
                        "-fx-text-fill: #9ca3af; -fx-font-size: 11px; -fx-font-style: italic;");
            });
        }).start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    private String getMonthName(int m) {
        return switch (m) {
            case 1  -> "Jan";
            case 2  -> "Fev";
            case 3  -> "Mar";
            case 4  -> "Avr";
            case 5  -> "Mai";
            case 6  -> "Juin";
            case 7  -> "Juil";
            case 8  -> "Aout";
            case 9  -> "Sep";
            case 10 -> "Oct";
            case 11 -> "Nov";
            case 12 -> "Dec";
            default -> "";
        };
    }
}
