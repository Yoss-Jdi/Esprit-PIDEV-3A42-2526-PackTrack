package com.esprit.controllers;

import com.esprit.entities.Facture;
import com.esprit.entities.Livreur;
import com.esprit.entities.Recompense;
import com.esprit.services.AiService;
import com.esprit.services.RecompenseService;
import com.esprit.utils.MyDataBase;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
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
import javafx.util.StringConverter;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RecompenseDialogController {

    @FXML private ComboBox<String>  typeField;
    @FXML private TextField         valeurField;
    @FXML private TextArea          descriptionField;
    @FXML private TextField         seuilField;
    @FXML private ComboBox<Facture> factureCombo;
    @FXML private ComboBox<Livreur> livreurComboBox;

    @FXML private Button btnGenererIA;
    @FXML private Label  labelIA;
    private final AiService aiService = new AiService();

    private Recompense recompense;

    // =====================================================
    // NOTIFICATION TOAST
    // =====================================================
    private enum NotifType { SUCCESS, ERROR, WARNING, INFO }

    private void showNotification(NotifType type, String title, String message) {
        Stage owner = (Stage) typeField.getScene().getWindow();

        String accent, bgColor, iconText;
        switch (type) {
            case SUCCESS -> { accent = "#1D9E75"; bgColor = "#E1F5EE"; iconText = "✓"; }
            case ERROR   -> { accent = "#E24B4A"; bgColor = "#FCEBEB"; iconText = "✕"; }
            case WARNING -> { accent = "#BA7517"; bgColor = "#FAEEDA"; iconText = "!"; }
            default      -> { accent = "#378ADD"; bgColor = "#E6F1FB"; iconText = "i"; }
        }

        Label icon = new Label(iconText);
        icon.setStyle(
                "-fx-background-color: " + bgColor + ";" +
                        "-fx-background-radius: 50%;" +
                        "-fx-min-width: 26px; -fx-min-height: 26px;" +
                        "-fx-max-width: 26px;  -fx-max-height: 26px;" +
                        "-fx-alignment: center;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 13px;" +
                        "-fx-text-fill: " + accent + ";"
        );

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #1a1a1a;");

        Label msgLabel = new Label(message);
        msgLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #555555;");
        msgLabel.setWrapText(true);
        msgLabel.setMaxWidth(240);

        VBox textBox = new VBox(3, titleLabel, msgLabel);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        Popup popup = new Popup();
        Label closeBtn = new Label("×");
        closeBtn.setStyle("-fx-font-size: 18px; -fx-text-fill: #aaaaaa; -fx-cursor: hand;");
        closeBtn.setOnMouseClicked(e -> popup.hide());

        HBox body = new HBox(12, icon, textBox, closeBtn);
        body.setAlignment(Pos.TOP_LEFT);
        body.setPadding(new Insets(12, 14, 12, 12));

        Rectangle strip = new Rectangle(4, 1);
        strip.setFill(Color.web(accent));
        strip.heightProperty().bind(body.heightProperty());

        HBox root = new HBox(strip, body);
        root.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: #e0e0e0;" +
                        "-fx-border-width: 0.5;" +
                        "-fx-border-radius: 8;" +
                        "-fx-background-radius: 8;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.13), 14, 0, 4);"
        );
        root.setPrefWidth(320);

        popup.getContent().add(root);
        popup.setAutoHide(false);

        double x = owner.getX() + (owner.getWidth()  - 320) / 2;
        double y = owner.getY() +  owner.getHeight()  - 100;
        popup.show(owner, x, y);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), root);
        fadeIn.setFromValue(0); fadeIn.setToValue(1); fadeIn.play();

        PauseTransition pause = new PauseTransition(Duration.seconds(2.5));
        pause.setOnFinished(e -> {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(300), root);
            fadeOut.setFromValue(1); fadeOut.setToValue(0);
            fadeOut.setOnFinished(ev -> popup.hide());
            fadeOut.play();
        });
        pause.play();
    }

    // =====================================================
    // INIT
    // =====================================================
    @FXML
    public void initialize() {
        typeField.getItems().addAll(
                "Bon de réduction",
                "Livraison gratuite",
                "Cadeau",
                "Points bonus",
                "Remise fidélité"
        );

        factureCombo.setConverter(new StringConverter<Facture>() {
            @Override public String toString(Facture f) { return f == null ? "" : f.getNumero(); }
            @Override public Facture fromString(String s) { return null; }
        });

        livreurComboBox.setConverter(new StringConverter<Livreur>() {
            @Override public String toString(Livreur l) { return l == null ? "" : l.getNom(); }
            @Override public Livreur fromString(String s) { return null; }
        });

        loadFacturesFromDB();
        loadLivreursFromDB();

        factureCombo.setOnAction(e -> {
            Facture selected = factureCombo.getValue();
            if (selected != null) chargerLivreurParFacture(selected.getIdLivraison());
        });
    }

    // =====================================================
    // FACTURES
    // =====================================================
    private void loadFacturesFromDB() {
        List<Facture> factures = new ArrayList<>();
        try {
            Connection cnx = MyDataBase.getInstance().getConnection();
            PreparedStatement ps = cnx.prepareStatement("SELECT * FROM factures");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                factures.add(new Facture(
                        rs.getInt("ID_Facture"),
                        rs.getString("numero"),
                        rs.getTimestamp("dateEmission"),
                        rs.getFloat("montantHT"),
                        rs.getFloat("montantTTC"),
                        rs.getFloat("tva"),
                        rs.getString("statut"),
                        rs.getInt("livraison_id")
                ));
            }
            factureCombo.getItems().setAll(factures);
        } catch (Exception e) {
            System.out.println("Erreur factures: " + e.getMessage());
        }
    }

    // =====================================================
    // LIVREURS
    // =====================================================
    private void loadLivreursFromDB() {
        try {
            Connection cnx = MyDataBase.getInstance().getConnection();
            PreparedStatement ps = cnx.prepareStatement(
                    "SELECT id_utilisateur AS id, CONCAT(nom, ' ', prenom) AS nom " +
                            "FROM utilisateurs WHERE role = 'livreur'"
            );
            ResultSet rs = ps.executeQuery();
            List<Livreur> livreurs = new ArrayList<>();
            while (rs.next()) {
                livreurs.add(new Livreur(rs.getInt("id"), rs.getString("nom")));
            }
            livreurComboBox.getItems().setAll(livreurs);
        } catch (Exception e) {
            System.out.println("Erreur livreurs: " + e.getMessage());
        }
    }

    // =====================================================
    // AUTO-SÉLECTION LIVREUR
    // =====================================================
    private void chargerLivreurParFacture(int idLivraison) {
        try {
            Connection cnx = MyDataBase.getInstance().getConnection();
            PreparedStatement ps = cnx.prepareStatement(
                    "SELECT u.id_utilisateur AS id, CONCAT(u.nom, ' ', u.prenom) AS nom " +
                            "FROM livraisons lv " +
                            "JOIN utilisateurs u ON u.id_utilisateur = lv.livreur_id " +
                            "WHERE lv.ID_Livraison = ?"
            );
            ps.setInt(1, idLivraison);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                int    idUser = rs.getInt("id");
                String nom    = rs.getString("nom");

                Livreur found = livreurComboBox.getItems().stream()
                        .filter(l -> l.getId() == idUser)
                        .findFirst()
                        .orElse(null);

                if (found != null) {
                    livreurComboBox.setValue(found);
                } else {
                    Livreur nouveau = new Livreur(idUser, nom);
                    livreurComboBox.getItems().add(nouveau);
                    livreurComboBox.setValue(nouveau);
                }
            }
        } catch (Exception e) {
            System.out.println("Erreur auto-livreur: " + e.getMessage());
        }
    }

    // =====================================================
    // SET RECOMPENSE (mode modification)
    // =====================================================
    public void setRecompense(Recompense r) {
        this.recompense = r;
        typeField.setValue(r.getType());
        valeurField.setText(String.valueOf(r.getValeur()));
        descriptionField.setText(r.getDescription());
        seuilField.setText(String.valueOf(r.getSeuil()));

        if (r.getIdFacture() != null) {
            factureCombo.getItems().stream()
                    .filter(f -> f.getIdFacture() == r.getIdFacture())
                    .findFirst()
                    .ifPresent(f -> {
                        factureCombo.setValue(f);
                        chargerLivreurParFacture(f.getIdLivraison());
                    });
        }

        if (r.getIdLivreur() > 0) {
            livreurComboBox.getItems().stream()
                    .filter(l -> l.getId() == r.getIdLivreur())
                    .findFirst()
                    .ifPresent(livreurComboBox::setValue);
        }
    }

    // =====================================================
    // GÉNÉRER DESCRIPTION AVEC GEMINI AI
    // =====================================================
    @FXML
    private void genererDescriptionIA() {
        String nom    = typeField.getValue() != null ? typeField.getValue().trim() : "";
        String points = seuilField.getText().trim();

        if (nom.isEmpty()) {
            setLabelIA("⚠ Choisissez le type d'abord.", "#ef4444");
            return;
        }
        if (points.isEmpty()) {
            setLabelIA("⚠ Remplissez le seuil d'abord.", "#ef4444");
            return;
        }

        int pointsInt;
        try {
            pointsInt = Integer.parseInt(points);
        } catch (NumberFormatException e) {
            setLabelIA("⚠ Seuil invalide.", "#ef4444");
            return;
        }

        btnGenererIA.setDisable(true);
        btnGenererIA.setText("⏳");
        setLabelIA("Gemini génère la description...", "#667eea");

        final int pointsFinal = pointsInt;
        new Thread(() -> {
            String description = aiService.genererDescriptionRecompense(nom, pointsFinal);
            Platform.runLater(() -> {
                descriptionField.setText(description);
                btnGenererIA.setDisable(false);
                btnGenererIA.setText("✨ IA");
                setLabelIA("✅ Description générée par Gemini !", "#16a34a");
            });
        }).start();
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
        markField(typeField, false);
        markField(valeurField, false);
        markField(descriptionField, false);
        markField(seuilField, false);
        markField(factureCombo, false);
        markField(livreurComboBox, false);
    }
    private void setLabelIA(String message, String color) {
        labelIA.setText(message);
        labelIA.setStyle("-fx-text-fill: " + color + "; -fx-font-style: italic; -fx-font-size: 11px;");
    }

    // =====================================================
    // SAVE — avec notification mail
    // =====================================================
    @FXML
    public void enregistrer() {

        clearErrors();

        boolean hasError = false;

        if (typeField.getValue() == null || typeField.getValue().trim().isEmpty()) {
            markField(typeField, true);
            hasError = true;
        }

        if (valeurField.getText().trim().isEmpty()) {
            markField(valeurField, true);
            hasError = true;
        }

        if (descriptionField.getText().trim().isEmpty()) {
            markField(descriptionField, true);
            hasError = true;
        }

        if (seuilField.getText().trim().isEmpty()) {
            markField(seuilField, true);
            hasError = true;
        }

        if (factureCombo.getValue() == null) {
            markField(factureCombo, true);
            hasError = true;
        }

        if (livreurComboBox.getValue() == null) {
            markField(livreurComboBox, true);
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

            if (recompense == null)
                recompense = new Recompense();

            recompense.setType(typeField.getValue());
            recompense.setValeur(Double.parseDouble(valeurField.getText().trim()));
            recompense.setDescription(descriptionField.getText().trim());
            recompense.setSeuil(Integer.parseInt(seuilField.getText().trim()));

            Facture f = factureCombo.getValue();
            recompense.setIdFacture(f.getIdFacture());

            Livreur l = livreurComboBox.getValue();
            recompense.setIdLivreur(l.getId());

            if (recompense.getDateObtention() == null) {
                recompense.setDateObtention(new java.util.Date());
            }

            RecompenseService service = new RecompenseService();

            if (recompense.getIdRecompense() == 0) {
                service.ajouter(recompense);

                showNotification(
                        NotifType.SUCCESS,
                        "Récompense ajoutée",
                        "Ajout effectué avec succès."
                );

            } else {
                service.modifier(recompense);

                showNotification(
                        NotifType.SUCCESS,
                        "Récompense modifiée",
                        "Modification effectuée avec succès."
                );
            }

        } catch (NumberFormatException e) {

            markField(valeurField, true);
            markField(seuilField, true);

            showNotification(
                    NotifType.ERROR,
                    "Valeurs invalides",
                    "Valeur et seuil doivent être numériques."
            );
        }
    }
}