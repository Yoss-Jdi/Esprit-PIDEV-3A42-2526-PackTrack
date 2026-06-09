package com.esprit.controllers;

import com.esprit.entities.Facture;
import com.esprit.services.FactureService;
import com.esprit.services.PdfService;
import com.esprit.utils.MyDataBase;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.sql.*;
import java.time.Year;
import java.util.Locale;

public class FactureController {

    private final FactureService factureService = new FactureService();
    private final PdfService pdfService = new PdfService();

    @FXML private TextField numeroField;
    @FXML private TextField montantHTField;
    @FXML private TextField montantTTCField;
    @FXML private TextField tvaField;

    @FXML private ComboBox<String> statutCombo;
    @FXML private ComboBox<Integer> cbLivraison;

    private Facture factureEnEdition = null;

    private Runnable onOpenTableView;

    public void setOnOpenTableView(Runnable action) {
        this.onOpenTableView = action;
    }

    @FXML
    public void initialize() {
        statutCombo.getItems().addAll("payee", "en attente", "annulee");
        chargerLivraisons();
        montantTTCField.setEditable(false);
        montantHTField.textProperty().addListener((o, a, b) -> calculTTC());
        tvaField.textProperty().addListener((o, a, b) -> calculTTC());
    }

    // ============================================================
    //  NOTIFICATION TOAST (VERSION AMÉLIORÉE)
    // ============================================================
    private enum NotifType { SUCCESS, ERROR, WARNING, INFO }

    private void showNotification(NotifType type, String title, String message) {

        Stage owner = (Stage) numeroField.getScene().getWindow();

        String accent, bgColor, iconText;
        switch (type) {
            case SUCCESS -> { accent = "#1D9E75"; bgColor = "#E1F5EE"; iconText = "✓"; }
            case ERROR   -> { accent = "#E24B4A"; bgColor = "#FCEBEB"; iconText = "✕"; }
            case WARNING -> { accent = "#BA7517"; bgColor = "#FAEEDA"; iconText = "!"; }
            default      -> { accent = "#378ADD"; bgColor = "#E6F1FB"; iconText = "i"; }
        }

        // Icône PLUS GRANDE
        Label icon = new Label(iconText);
        icon.setStyle(
                "-fx-background-color: " + bgColor + ";" +
                        "-fx-background-radius: 50%;" +
                        "-fx-min-width: 34px; -fx-min-height: 34px;" +
                        "-fx-max-width: 34px; -fx-max-height: 34px;" +
                        "-fx-alignment: center;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 16px;" +
                        "-fx-text-fill: " + accent + ";"
        );

        // Texte PLUS GRAND
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: #1a1a1a;");

        Label msgLabel = new Label(message);
        msgLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #444;");
        msgLabel.setWrapText(true);
        msgLabel.setMaxWidth(320);

        VBox textBox = new VBox(6, titleLabel, msgLabel);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        Popup popup = new Popup();

        Label closeBtn = new Label("×");
        closeBtn.setStyle("-fx-font-size: 20px; -fx-text-fill: #999; -fx-cursor: hand;");
        closeBtn.setOnMouseClicked(e -> popup.hide());

        HBox body = new HBox(15, icon, textBox, closeBtn);
        body.setAlignment(Pos.CENTER_LEFT);
        body.setPadding(new Insets(18));

        Rectangle strip = new Rectangle(5, 1);
        strip.setFill(Color.web(accent));
        strip.heightProperty().bind(body.heightProperty());

        HBox root = new HBox(strip, body);
        root.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: #e0e0e0;" +
                        "-fx-border-radius: 10;" +
                        "-fx-background-radius: 10;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 20, 0, 0, 6);"
        );

        // LARGEUR AUGMENTÉE
        root.setPrefWidth(420);

        popup.getContent().add(root);
        popup.setAutoHide(true);

        // 🔼 POSITION EN HAUT À DROITE
        double x = owner.getX() + owner.getWidth() - 450;
        double y = owner.getY() + 20;

        popup.show(owner, x, y);

        // Animation
        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), root);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();

        PauseTransition pause = new PauseTransition(Duration.seconds(4));
        pause.setOnFinished(e -> {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(300), root);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(ev -> popup.hide());
            fadeOut.play();
        });

        pause.play();
    }

    // ============================================================
    // RESTE DU CODE (INCHANGÉ)
    // ============================================================

    @FXML
    private void ouvrirTableView() {
        if (onOpenTableView != null) onOpenTableView.run();
    }

    public void setFactureToEdit(Facture facture) {
        this.factureEnEdition = facture;
        numeroField.setText(facture.getNumero());
        numeroField.setEditable(false);
        montantHTField.setText(String.valueOf(facture.getMontantHT()));
        tvaField.setText(String.valueOf(facture.getTva()));
        montantTTCField.setText(String.valueOf(facture.getMontantTTC()));
        statutCombo.setValue(facture.getStatut());
        cbLivraison.setValue(facture.getIdLivraison());
    }

    @FXML
    private void genererNumeroFacture() {
        try {
            Connection cnx = MyDataBase.getInstance().getConnection();
            String year = String.valueOf(Year.now().getValue());
            String sql = "SELECT numero FROM factures WHERE numero LIKE ? ORDER BY id_facture DESC LIMIT 1";
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setString(1, "FAC-" + year + "%");
            ResultSet rs = ps.executeQuery();

            int next = 1;
            if (rs.next()) {
                String last = rs.getString("numero");
                String[] parts = last.split("-");
                next = Integer.parseInt(parts[2]) + 1;
            }

            String newNumero = String.format("FAC-%s-%03d", year, next);
            numeroField.setText(newNumero);
            showNotification(NotifType.INFO, "Numéro généré", newNumero);

        } catch (Exception e) {
            showNotification(NotifType.ERROR, "Erreur", e.getMessage());
        }
    }

    private void calculTTC() {
        try {
            if (montantHTField.getText().isEmpty() || tvaField.getText().isEmpty()) {
                montantTTCField.clear();
                return;
            }

            float ht  = Float.parseFloat(montantHTField.getText().replace(',', '.'));
            float tva = Float.parseFloat(tvaField.getText().replace(',', '.'));
            float ttc = ht + (ht * tva / 100);

            montantTTCField.setText(String.format(Locale.US, "%.2f", ttc));

        } catch (Exception e) {
            montantTTCField.clear();
        }
    }

    @FXML
    private void ajouterFacture() {

        clearErrors();

        boolean hasError = false;

        if (numeroField.getText().trim().isEmpty()) {
            markField(numeroField, true);
            hasError = true;
        }

        if (montantHTField.getText().trim().isEmpty()) {
            markField(montantHTField, true);
            hasError = true;
        }

        if (tvaField.getText().trim().isEmpty()) {
            markField(tvaField, true);
            hasError = true;
        }

        if (statutCombo.getValue() == null) {
            markField(statutCombo, true);
            hasError = true;
        }

        if (cbLivraison.getValue() == null) {
            markField(cbLivraison, true);
            hasError = true;
        }

        if (hasError) {
            showNotification(
                    NotifType.WARNING,
                    "Champs obligatoires",
                    "Veuillez remplir les champs en rouge."
            );
            return;
        }

        try {
            float ht  = Float.parseFloat(montantHTField.getText().replace(",", "."));
            float tva = Float.parseFloat(tvaField.getText().replace(",", "."));
            float ttc = Float.parseFloat(montantTTCField.getText().replace(",", "."));

            Facture f = new Facture(
                    numeroField.getText(),
                    ht,
                    ttc,
                    tva,
                    statutCombo.getValue(),
                    cbLivraison.getValue()
            );

            factureService.ajouter(f);

            showNotification(
                    NotifType.SUCCESS,
                    "Succès",
                    "Facture ajoutée avec succès."
            );

            clear();

        } catch (NumberFormatException e) {
            showNotification(
                    NotifType.ERROR,
                    "Valeur invalide",
                    "Montant HT ou TVA incorrect."
            );
        }
    }

    private void markField(Control field, boolean error) {
        if (error) {
            if (!field.getStyleClass().contains("error-field")) {
                field.getStyleClass().add("error-field");
            }
        } else {
            field.getStyleClass().remove("error-field");
        }
    }

    private void clearErrors() {
        markField(numeroField, false);
        markField(montantHTField, false);
        markField(tvaField, false);
        markField(statutCombo, false);
        markField(cbLivraison, false);
    }
    @FXML
    private void genererPdf() {
        try {
            float ht  = Float.parseFloat(montantHTField.getText());
            float tva = Float.parseFloat(tvaField.getText());
            float ttc = Float.parseFloat(montantTTCField.getText());

            Facture f = new Facture(
                    numeroField.getText(), ht, ttc, tva,
                    statutCombo.getValue(), cbLivraison.getValue()
            );

            String url = pdfService.generateFacturePdf(f);
            showNotification(NotifType.INFO, "PDF généré", url);

        } catch (Exception e) {
            showNotification(NotifType.ERROR, "Erreur PDF", e.getMessage());
        }
    }

    private void clear() {
        numeroField.clear();
        montantHTField.clear();
        montantTTCField.clear();
        tvaField.clear();
        statutCombo.setValue(null);
        cbLivraison.setValue(null);
    }

    private void chargerLivraisons() {
        try {
            Connection cnx = MyDataBase.getInstance().getConnection();
            Statement st = cnx.createStatement();
            ResultSet rs = st.executeQuery("SELECT ID_Livraison FROM livraisons");

            while (rs.next()) {
                cbLivraison.getItems().add(rs.getInt(1));
            }

        } catch (SQLException e) {
            System.err.println("Erreur chargement livraisons : " + e.getMessage());
        }
    }
}