package com.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.VBox;

public class AdminDashboardController {

    @FXML
    private VBox contentArea;

    // ================= DASHBOARD =================
    @FXML
    public void showDashboard() {
        contentArea.getChildren().setAll(load("/com/esprit/dashboard.fxml"));
    }

    // ================= FACTURES =================
    @FXML
    public void showFactures() {
        contentArea.getChildren().setAll(load("/com/esprit/facture-table-view.fxml"));
    }

    @FXML
    public void showAjoutFacture() {
        contentArea.getChildren().setAll(load("/com/esprit/facture-view.fxml"));
    }

    // ================= RECOMPENSES =================
    @FXML
    public void showRecompenses() {
        contentArea.getChildren().setAll(load("/com/esprit/recompense-table-view.fxml"));
    }

    @FXML
    public void showAjoutRecompense() {
        contentArea.getChildren().setAll(load("/com/esprit/recompense-view.fxml"));
    }

    // ================= STATS =================
    @FXML
    public void showFactureStats() {
        contentArea.getChildren().setAll(load("/com/esprit/facture-stats.fxml"));
    }

    @FXML
    public void showRecompenseStats() {
        contentArea.getChildren().setAll(load("/com/esprit/recompense-stats.fxml"));
    }

    // ================= LOADER =================
    private Node load(String fxml) {
        try {
            return FXMLLoader.load(getClass().getResource(fxml));
        } catch (Exception e) {
            e.printStackTrace();
            return new VBox();
        }
    }
}