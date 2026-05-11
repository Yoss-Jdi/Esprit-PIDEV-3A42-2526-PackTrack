package com.gestioncolis.controllers;

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
        contentArea.getChildren().setAll(load("/com/gestioncolis/dashboard.fxml"));
    }

    // ================= FACTURES =================
    @FXML
    public void showFactures() {
        contentArea.getChildren().setAll(load("/com/gestioncolis/facture-table-view.fxml"));
    }

    @FXML
    public void showAjoutFacture() {
        contentArea.getChildren().setAll(load("/com/gestioncolis/facture-view.fxml"));
    }

    // ================= RECOMPENSES =================
    @FXML
    public void showRecompenses() {
        contentArea.getChildren().setAll(load("/com/gestioncolis/recompense-table-view.fxml"));
    }

    @FXML
    public void showAjoutRecompense() {
        contentArea.getChildren().setAll(load("/com/gestioncolis/recompense-view.fxml"));
    }

    // ================= STATS =================
    @FXML
    public void showFactureStats() {
        contentArea.getChildren().setAll(load("/com/gestioncolis/facture-stats.fxml"));
    }

    @FXML
    public void showRecompenseStats() {
        contentArea.getChildren().setAll(load("/com/gestioncolis/recompense-stats.fxml"));
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
