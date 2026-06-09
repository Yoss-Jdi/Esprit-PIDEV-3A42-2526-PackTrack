package com.esprit.controllers;

import com.esprit.entities.Facture;
import com.esprit.services.FactureService;
import com.esprit.services.PdfService;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.*;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Comparator;

public class FactureTableController {

    private final FactureService factureService = new FactureService();
    private final PdfService pdfService = new PdfService();

    @FXML private TableView<Facture> factureTable;

    @FXML private TableColumn<Facture, String> numeroColumn;
    @FXML private TableColumn<Facture, Float> montantHTColumn;
    @FXML private TableColumn<Facture, Float> montantTTCColumn;
    @FXML private TableColumn<Facture, Float> tvaColumn;
    @FXML private TableColumn<Facture, String> statutColumn;
    @FXML private TableColumn<Facture, Integer> livraisonColumn;
    @FXML private TableColumn<Facture, Void> actionsColumn;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> triCombo;
    @FXML private Label messageLabel;

    private ObservableList<Facture> sourceList;
    private FilteredList<Facture> filteredData;

    private enum NotifType { SUCCESS, ERROR, WARNING, INFO }

    @FXML
    public void initialize() {
        initColumns();
        initCombo();
        loadFactures();
        addActions();

        // Recherche dynamique en temps réel
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterFactures());
    }

    private void initColumns() {
        numeroColumn.setCellValueFactory(new PropertyValueFactory<>("numero"));
        montantHTColumn.setCellValueFactory(new PropertyValueFactory<>("montantHT"));
        montantTTCColumn.setCellValueFactory(new PropertyValueFactory<>("montantTTC"));
        tvaColumn.setCellValueFactory(new PropertyValueFactory<>("tva"));
        statutColumn.setCellValueFactory(new PropertyValueFactory<>("statut"));
        livraisonColumn.setCellValueFactory(new PropertyValueFactory<>("idLivraison"));
    }

    private void initCombo() {
        triCombo.setItems(FXCollections.observableArrayList(
                "Numéro A-Z", "Numéro Z-A",
                "HT croissant", "HT décroissant",
                "TTC croissant", "TTC décroissant",
                "Statut", "Livraison"
        ));
    }

    private void loadFactures() {
        sourceList = FXCollections.observableArrayList(
                new ArrayList<>(factureService.afficherTous())
        );

        filteredData = new FilteredList<>(sourceList, p -> true);
        factureTable.setItems(filteredData);

        messageLabel.setText(sourceList.size() + " facture(s)");
    }

    @FXML
    private void filterFactures() {

        String keyword = searchField.getText();

        filteredData.setPredicate(f -> {

            if (keyword == null || keyword.trim().isEmpty()) {
                return true;
            }

            String lower = keyword.toLowerCase().trim();

            return (f.getNumero() != null &&
                    f.getNumero().toLowerCase().contains(lower))

                    || (f.getStatut() != null &&
                    f.getStatut().toLowerCase().contains(lower))

                    || String.valueOf(f.getMontantHT()).contains(lower)

                    || String.valueOf(f.getMontantTTC()).contains(lower)

                    || String.valueOf(f.getTva()).contains(lower)

                    || String.valueOf(f.getIdLivraison()).contains(lower);
        });

        factureTable.refresh();
        messageLabel.setText(filteredData.size() + " résultat(s)");
    }

    @FXML
    private void trierFactures() {

        String choix = triCombo.getValue();
        if (choix == null) return;

        switch (choix) {

            case "Numéro A-Z" ->
                    sourceList.sort(Comparator.comparing(Facture::getNumero));

            case "Numéro Z-A" ->
                    sourceList.sort(Comparator.comparing(Facture::getNumero).reversed());

            case "HT croissant" ->
                    sourceList.sort(Comparator.comparingDouble(Facture::getMontantHT));

            case "HT décroissant" ->
                    sourceList.sort(Comparator.comparingDouble(Facture::getMontantHT).reversed());

            case "TTC croissant" ->
                    sourceList.sort(Comparator.comparingDouble(Facture::getMontantTTC));

            case "TTC décroissant" ->
                    sourceList.sort(Comparator.comparingDouble(Facture::getMontantTTC).reversed());

            case "Statut" ->
                    sourceList.sort(Comparator.comparing(Facture::getStatut));

            case "Livraison" ->
                    sourceList.sort(Comparator.comparingInt(Facture::getIdLivraison));
        }

        factureTable.refresh();
    }

    @FXML
    private void resetAll() {
        searchField.clear();
        triCombo.setValue(null);
        loadFactures();
    }

    private void addActions() {

        actionsColumn.setCellFactory(col -> new TableCell<>() {

            private final Button btnModifier = new Button("Modifier");
            private final Button btnDelete = new Button("Supprimer");
            private final Button btnPDF = new Button("PDF");

            private final HBox box = new HBox(8, btnModifier, btnDelete, btnPDF);

            {
                box.setAlignment(Pos.CENTER);

                btnModifier.setStyle("-fx-background-color:#f59e0b;-fx-text-fill:white;");
                btnDelete.setStyle("-fx-background-color:#ef4444;-fx-text-fill:white;");
                btnPDF.setStyle("-fx-background-color:#378ADD;-fx-text-fill:white;");

                btnModifier.setOnAction(e -> modifierFacture());
                btnDelete.setOnAction(e -> supprimerFacture());
                btnPDF.setOnAction(e -> genererPDF());
            }

            private void modifierFacture() {
                Facture f = getTableRow().getItem();
                if (f == null) return;

                try {
                    FXMLLoader loader = new FXMLLoader(
                            getClass().getResource("/com/esprit/facture-view.fxml")
                    );

                    Stage stage = new Stage();
                    stage.setScene(new Scene(loader.load()));
                    stage.initModality(Modality.APPLICATION_MODAL);

                    FactureController controller = loader.getController();
                    controller.setFactureToEdit(f);

                    stage.showAndWait();

                    loadFactures();

                } catch (Exception ex) {
                    showNotification(NotifType.ERROR, "Erreur", "Impossible d'ouvrir.");
                }
            }

            private void supprimerFacture() {
                Facture f = getTableRow().getItem();
                if (f == null) return;

                factureService.supprimer(f.getIdFacture());
                loadFactures();

                showNotification(NotifType.SUCCESS,
                        "Suppression",
                        "Facture supprimée.");
            }

            private void genererPDF() {
                Facture f = getTableRow().getItem();
                if (f == null) return;

                String url = pdfService.generateFacturePdf(f);

                showNotification(
                        NotifType.INFO,
                        "PDF généré",
                        url
                );
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    private void showNotification(NotifType type, String title, String msg) {
        messageLabel.setText(msg);
    }
}