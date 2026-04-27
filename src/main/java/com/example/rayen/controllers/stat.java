package com.example.rayen.controllers;

import com.example.rayen.SceneNavigator;
import com.example.rayen.entities.technicien;
import com.example.rayen.services.Servicetechnicien;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

public class stat {
    private static final DecimalFormat MOYENNE_FORMAT = new DecimalFormat("0.0");

    private final Servicetechnicien servicetechnicien;
    private final SceneNavigator sceneNavigator;

    @FXML
    private BarChart<String, Number> vehiculesChart;
    @FXML
    private CategoryAxis technicienAxis;
    @FXML
    private NumberAxis vehiculesAxis;
    @FXML
    private Label technicienCountLabel;
    @FXML
    private Label assignedVehiculeCountLabel;
    @FXML
    private Label averageLabel;
    @FXML
    private Label chartStatusLabel;
    @FXML
    private Label infoLabel;

    public stat(Servicetechnicien servicetechnicien, SceneNavigator sceneNavigator) {
        this.servicetechnicien = servicetechnicien;
        this.sceneNavigator = sceneNavigator;
    }

    @FXML
    private void initialize() {
        vehiculesChart.setAnimated(false);
        vehiculesChart.setLegendVisible(false);
        vehiculesChart.setCategoryGap(10);
        vehiculesChart.setBarGap(4);
        technicienAxis.setGapStartAndEnd(false);
        vehiculesAxis.setForceZeroInRange(true);
        vehiculesAxis.setMinorTickVisible(false);
        rafraichirStatistiques();
    }

    @FXML
    private void handleRefresh() {
        rafraichirStatistiques();
    }

    @FXML
    private void handleGoVehicules() {
        try {
            sceneNavigator.showDisplayView("Retour a la liste des vehicules.");
        } catch (IOException exception) {
            afficherErreur("Impossible d'ouvrir la liste des vehicules.", exception);
        }
    }

    @FXML
    private void handleGoTechniciens() {
        try {
            sceneNavigator.showDisplayTechnicienView("Retour a la liste des techniciens.");
        } catch (IOException exception) {
            afficherErreur("Impossible d'ouvrir la liste des techniciens.", exception);
        }
    }

    @FXML
    private void handleGoStats() {
        infoLabel.setText("Vous etes deja dans les statistiques des techniciens.");
    }

    @FXML
    private void handleOpenChatbot() {
        try {
            sceneNavigator.showChatbotWindow();
        } catch (IOException exception) {
            afficherErreur("Impossible d'ouvrir le chatbot Ollama.", exception);
        }
    }

    private void rafraichirStatistiques() {
        ObservableList<technicien> techniciens = servicetechnicien.afficherTechniciens();
        int totalTechniciens = techniciens.size();
        int totalVehiculesAssignes = techniciens.stream()
                .mapToInt(technicien::getNombreVehicules)
                .sum();
        double moyenneVehicules = totalTechniciens == 0 ? 0 : (double) totalVehiculesAssignes / totalTechniciens;

        technicienCountLabel.setText(String.valueOf(totalTechniciens));
        assignedVehiculeCountLabel.setText(String.valueOf(totalVehiculesAssignes));
        averageLabel.setText(MOYENNE_FORMAT.format(moyenneVehicules));

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        Map<XYChart.Data<String, Number>, technicien> donneesParTechnicien = new LinkedHashMap<>();
        techniciens.stream()
                .sorted(Comparator.comparingInt(technicien::getNombreVehicules)
                        .reversed()
                        .thenComparing(technicien::getNomComplet, String.CASE_INSENSITIVE_ORDER))
                .forEach(technicien -> {
                    XYChart.Data<String, Number> donnee = new XYChart.Data<>(
                            construireLibelleCategorie(technicien),
                            technicien.getNombreVehicules()
                    );
                    series.getData().add(donnee);
                    donneesParTechnicien.put(donnee, technicien);
                });
        vehiculesChart.getData().setAll(series);
        configurerAxeVertical(techniciens);
        Platform.runLater(() -> installerInfoBulles(donneesParTechnicien));

        if (totalTechniciens == 0) {
            chartStatusLabel.setText("Aucun technicien enregistre pour le moment.");
            infoLabel.setText("Ajoutez un technicien pour afficher la repartition des vehicules.");
            return;
        }

        if (totalVehiculesAssignes == 0) {
            chartStatusLabel.setText("Les techniciens existent, mais aucun vehicule n'est encore assigne.");
            infoLabel.setText("Techniciens : " + totalTechniciens + " | Vehicules assignes : 0");
            return;
        }

        technicien technicienLePlusCharge = techniciens.stream()
                .max(Comparator.comparingInt(technicien::getNombreVehicules)
                        .thenComparing(technicien::getNomComplet, String.CASE_INSENSITIVE_ORDER))
                .orElse(null);

        if (technicienLePlusCharge != null) {
            chartStatusLabel.setText("Technicien le plus charge : "
                    + construireNomAffiche(technicienLePlusCharge)
                    + " avec "
                    + technicienLePlusCharge.getNombreVehicules()
                    + " vehicule(s).");
        } else {
            chartStatusLabel.setText("Repartition des vehicules par technicien.");
        }

        infoLabel.setText("Techniciens : " + totalTechniciens + " | Vehicules assignes : " + totalVehiculesAssignes);
    }

    private void configurerAxeVertical(ObservableList<technicien> techniciens) {
        int maxVehicules = techniciens.stream()
                .mapToInt(technicien::getNombreVehicules)
                .max()
                .orElse(0);

        double tickUnit;
        if (maxVehicules <= 5) {
            tickUnit = 1;
        } else if (maxVehicules <= 15) {
            tickUnit = 2;
        } else {
            tickUnit = 5;
        }

        double upperBound = Math.max(5, Math.ceil((maxVehicules + tickUnit) / tickUnit) * tickUnit);
        vehiculesAxis.setAutoRanging(false);
        vehiculesAxis.setLowerBound(0);
        vehiculesAxis.setUpperBound(upperBound);
        vehiculesAxis.setTickUnit(tickUnit);
    }

    private void installerInfoBulles(Map<XYChart.Data<String, Number>, technicien> donneesParTechnicien) {
        for (Map.Entry<XYChart.Data<String, Number>, technicien> entree : donneesParTechnicien.entrySet()) {
            XYChart.Data<String, Number> data = entree.getKey();
            technicien technicienAssocie = entree.getValue();

            if (data.getNode() == null) {
                continue;
            }

            String contenuInfoBulle = construireNomAffiche(technicienAssocie)
                    + "\nVehicules : " + technicienAssocie.getNombreVehicules()
                    + "\nListe : " + technicienAssocie.getVehiculesResume();
            Tooltip.install(data.getNode(), new Tooltip(contenuInfoBulle));
        }
    }

    private String construireLibelleCategorie(technicien technicien) {
        return abregerNom(construireNomAffiche(technicien), 12);
    }

    private String construireNomAffiche(technicien technicien) {
        String nomComplet = technicien.getNomComplet();
        return nomComplet == null || nomComplet.isBlank()
                ? "Technicien"
                : nomComplet;
    }

    private String abregerNom(String nom, int longueurMax) {
        if (nom.length() <= longueurMax) {
            return nom;
        }

        String[] morceaux = nom.trim().split("\\s+");
        if (morceaux.length >= 2 && !morceaux[1].isBlank()) {
            return morceaux[0] + " " + morceaux[1].charAt(0) + ".";
        }

        return nom.substring(0, Math.max(0, longueurMax - 1)) + ".";
    }

    private void afficherErreur(String message, Exception exception) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(message);
        alert.setContentText(exception.getMessage());
        alert.showAndWait();
    }
}
