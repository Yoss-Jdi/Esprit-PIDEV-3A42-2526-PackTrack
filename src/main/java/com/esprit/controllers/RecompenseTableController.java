package com.esprit.controllers;

import com.esprit.entities.Recompense;
import com.esprit.services.RecompenseService;
import com.esprit.utils.MyDataBase;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.stage.Popup;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.stream.Collectors;

public class RecompenseTableController {

    // ================= TABLE =================
    @FXML private TableView<Recompense> recompenseTable;

    @FXML private TableColumn<Recompense, String>  typeColumn;
    @FXML private TableColumn<Recompense, Double>  valeurColumn;
    @FXML private TableColumn<Recompense, String>  descriptionColumn;
    @FXML private TableColumn<Recompense, Integer> seuilColumn;
    @FXML private TableColumn<Recompense, String>  dateColumn;
    @FXML private TableColumn<Recompense, String>  livreurColumn;
    @FXML private TableColumn<Recompense, String>  factureColumn;
    @FXML private TableColumn<Recompense, Void>    actionsColumn;

    // ================= UI =================
    @FXML private TextField        searchField;
    @FXML private ComboBox<String> triCombo;
    @FXML private Label            messageLabel;

    // ================= SERVICE =================
    private final RecompenseService service = new RecompenseService();
    private List<Recompense> allRecompenses;

    // ============================================================
    //  NOTIFICATION TOAST
    // ============================================================
    private enum NotifType { SUCCESS, ERROR, WARNING, INFO }

    private void showNotification(NotifType type, String title, String message) {
        Stage owner = (Stage) recompenseTable.getScene().getWindow();

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
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.13), 14, 0, 0, 4);"
        );
        root.setPrefWidth(320);

        popup.getContent().add(root);
        popup.setAutoHide(true);

        double x = owner.getX() + owner.getWidth()  - 340;
        double y = owner.getY() + owner.getHeight()  - 120;
        popup.show(owner, x, y);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), root);
        fadeIn.setFromValue(0); fadeIn.setToValue(1); fadeIn.play();

        PauseTransition pause = new PauseTransition(Duration.seconds(3.5));
        pause.setOnFinished(e -> {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(300), root);
            fadeOut.setFromValue(1); fadeOut.setToValue(0);
            fadeOut.setOnFinished(ev -> popup.hide());
            fadeOut.play();
        });
        pause.play();
    }

    // ============================================================
    //  CONFIRMATION DIALOG
    // ============================================================
    private boolean showConfirmDialog(String titre, String ligne1, String ligne2) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.setResizable(false);

        Label iconLabel = new Label("!");
        iconLabel.setStyle(
                "-fx-background-color: #FAEEDA;" +
                        "-fx-background-radius: 50%;" +
                        "-fx-min-width: 44px; -fx-min-height: 44px;" +
                        "-fx-max-width: 44px;  -fx-max-height: 44px;" +
                        "-fx-alignment: center;" +
                        "-fx-font-size: 22px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #BA7517;"
        );

        Label titleLbl  = new Label(titre);
        titleLbl.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1a1a1a;");
        Label line1Lbl  = new Label(ligne1);
        line1Lbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #333333;");
        Label line2Lbl  = new Label(ligne2);
        line2Lbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #888888;");

        VBox textBox = new VBox(6, titleLbl, line1Lbl, line2Lbl);
        textBox.setAlignment(Pos.CENTER_LEFT);

        HBox header = new HBox(16, iconLabel, textBox);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20, 24, 12, 24));

        Separator sep = new Separator();

        final boolean[] confirmed = {false};

        Button btnAnnuler = new Button("Annuler");
        btnAnnuler.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: #d0d0d0; -fx-border-width: 0.5;" +
                        "-fx-border-radius: 6; -fx-background-radius: 6;" +
                        "-fx-font-size: 13px; -fx-text-fill: #333333;" +
                        "-fx-cursor: hand; -fx-pref-width: 110px; -fx-pref-height: 36px;"
        );
        btnAnnuler.setOnAction(e -> dialog.close());

        Button btnSupprimer = new Button("Oui, supprimer");
        btnSupprimer.setStyle(
                "-fx-background-color: #E24B4A; -fx-background-radius: 6; -fx-border-radius: 6;" +
                        "-fx-font-size: 13px; -fx-text-fill: white;" +
                        "-fx-cursor: hand; -fx-pref-width: 130px; -fx-pref-height: 36px;"
        );
        btnSupprimer.setOnAction(e -> { confirmed[0] = true; dialog.close(); });

        HBox buttons = new HBox(12, btnAnnuler, btnSupprimer);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        buttons.setPadding(new Insets(14, 24, 20, 24));

        VBox layout = new VBox(header, sep, buttons);
        layout.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: #e0e0e0; -fx-border-width: 0.5;" +
                        "-fx-border-radius: 12; -fx-background-radius: 12;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 20, 0, 0, 6);"
        );
        layout.setPrefWidth(380);

        Scene scene = new Scene(layout);
        scene.setFill(Color.TRANSPARENT);
        dialog.setScene(scene);

        Stage owner = (Stage) recompenseTable.getScene().getWindow();
        dialog.setX(owner.getX() + (owner.getWidth()  - 380) / 2);
        dialog.setY(owner.getY() + (owner.getHeight() - 160) / 2);

        dialog.showAndWait();
        return confirmed[0];
    }

    // ============================================================
    //  INIT
    // ============================================================
    @FXML
    public void initialize() {
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        valeurColumn.setCellValueFactory(new PropertyValueFactory<>("valeur"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        seuilColumn.setCellValueFactory(new PropertyValueFactory<>("seuil"));

        dateColumn.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getDateObtention() != null
                                ? data.getValue().getDateObtention().toString() : "-"
                )
        );

        livreurColumn.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        getNomLivreur(data.getValue().getIdLivreur())
                )
        );

        factureColumn.setCellValueFactory(data -> {
            Integer id = data.getValue().getIdFacture();
            return new javafx.beans.property.SimpleStringProperty(
                    id == null ? "-" : getNumeroFacture(id)
            );
        });

        triCombo.setItems(FXCollections.observableArrayList(
                "Valeur décroissante",
                "Valeur croissante",
                "Date récente",
                "Date ancienne",
                "Type A-Z"
        ));

        addActionButtons();
        actualiserTableau();
    }

    // ============================================================
    //  LOAD — relit toujours depuis la BDD
    // ============================================================
    @FXML
    public void actualiserTableau() {
        allRecompenses = service.afficherTous();
        recompenseTable.setItems(FXCollections.observableArrayList(allRecompenses));
        triCombo.setValue(null);
        if (messageLabel != null) messageLabel.setText("");
    }

    // ============================================================
    //  AJOUTER — ✅ FIX : rafraîchir le tableau après fermeture du dialog
    // ============================================================
    @FXML
    public void ajouterRecompense() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/esprit/recompense-dialog.fxml")
            );
            Parent root = loader.load();

            // ✅ Ne pas appeler setRecompense() → recompense restera null → mode AJOUT
            Stage stage = new Stage();
            stage.setTitle("Nouvelle Récompense");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait(); // ← bloque jusqu'à fermeture

            // ✅ Rafraîchir le tableau depuis la BDD après fermeture
            actualiserTableau();
            showNotification(NotifType.SUCCESS, "Ajouté", "La récompense a été enregistrée.");

        } catch (Exception e) {
            e.printStackTrace();
            showNotification(NotifType.ERROR, "Erreur", "Impossible d'ouvrir le formulaire d'ajout.");
        }
    }

    // ============================================================
    //  TRI
    // ============================================================
    @FXML
    public void trier() {
        String choix = triCombo.getValue();
        if (choix == null) return;

        List<Recompense> sorted = allRecompenses.stream()
                .sorted((r1, r2) -> switch (choix) {
                    case "Valeur décroissante" -> Double.compare(r2.getValeur(), r1.getValeur());
                    case "Valeur croissante"   -> Double.compare(r1.getValeur(), r2.getValeur());
                    case "Date récente"        -> r2.getDateObtention() != null && r1.getDateObtention() != null
                            ? r2.getDateObtention().compareTo(r1.getDateObtention()) : 0;
                    case "Date ancienne"       -> r1.getDateObtention() != null && r2.getDateObtention() != null
                            ? r1.getDateObtention().compareTo(r2.getDateObtention()) : 0;
                    case "Type A-Z"            -> r1.getType().compareToIgnoreCase(r2.getType());
                    default -> 0;
                })
                .collect(Collectors.toList());

        recompenseTable.setItems(FXCollections.observableArrayList(sorted));
    }

    // ============================================================
    //  TOP 5
    // ============================================================
    @FXML
    public void topRecompenses() {
        List<Recompense> top = allRecompenses.stream()
                .sorted((r1, r2) -> Double.compare(r2.getValeur(), r1.getValeur()))
                .limit(5)
                .collect(Collectors.toList());

        recompenseTable.setItems(FXCollections.observableArrayList(top));
        if (messageLabel != null) messageLabel.setText("Top 5 récompenses");
    }

    // ============================================================
    //  ACTION BUTTONS
    // ============================================================
    private void addActionButtons() {
        actionsColumn.setCellFactory(param -> new TableCell<>() {

            private final Button btnEdit   = new Button("Modifier");
            private final Button btnDelete = new Button("Supprimer");

            {
                btnEdit.setStyle(
                        "-fx-background-color: #f59e0b;" +
                                "-fx-text-fill: white;" +
                                "-fx-background-radius: 5;" +
                                "-fx-cursor: hand;"
                );
                btnDelete.setStyle(
                        "-fx-background-color: #ef4444;" +
                                "-fx-text-fill: white;" +
                                "-fx-background-radius: 5;" +
                                "-fx-cursor: hand;"
                );

                btnEdit.setOnAction(e -> {
                    Recompense r = getTableView().getItems().get(getIndex());
                    modifierRecompense(r);
                });

                btnDelete.setOnAction(e -> {
                    Recompense r = getTableView().getItems().get(getIndex());
                    supprimerRecompense(r);
                });
            }

            private final HBox box = new HBox(10, btnEdit, btnDelete);

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    // ============================================================
    //  DELETE
    // ============================================================
    private void supprimerRecompense(Recompense r) {
        boolean confirmed = showConfirmDialog(
                "Confirmer la suppression",
                "Voulez-vous supprimer cette récompense ?",
                "Type : " + r.getType() + "  |  Valeur : " + r.getValeur()
        );
        if (!confirmed) return;

        service.supprimer(r.getIdRecompense());
        actualiserTableau();
        showNotification(NotifType.SUCCESS, "Supprimé", "La récompense a été supprimée.");
    }

    // ============================================================
    //  MODIFY
    // ============================================================
    private void modifierRecompense(Recompense r) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/esprit/recompense-dialog.fxml")
            );
            Parent root = loader.load();
            RecompenseDialogController controller = loader.getController();
            controller.setRecompense(r);

            Stage stage = new Stage();
            stage.setTitle("Modifier Récompense");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();

            actualiserTableau();
            showNotification(NotifType.SUCCESS, "Modifié", "La récompense a été mise à jour.");

        } catch (Exception e) {
            e.printStackTrace();
            showNotification(NotifType.ERROR, "Erreur", "Impossible d'ouvrir le formulaire de modification.");
        }
    }

    // ============================================================
    //  SEARCH
    // ============================================================
    @FXML
    public void rechercher() {
        if (allRecompenses == null) return;

        String text = searchField.getText().toLowerCase().trim();

        if (text.isEmpty()) {
            recompenseTable.setItems(FXCollections.observableArrayList(allRecompenses));
            return;
        }

        List<Recompense> filtered = allRecompenses.stream()
                .filter(r ->
                        (r.getType()        != null && r.getType().toLowerCase().contains(text)) ||
                                (r.getDescription() != null && r.getDescription().toLowerCase().contains(text))
                )
                .collect(Collectors.toList());

        recompenseTable.setItems(FXCollections.observableArrayList(filtered));
    }

    // ============================================================
    //  DB HELPERS
    // ============================================================
    private String getNomLivreur(int idLivreur) {
        if (idLivreur <= 0) return "-";
        try {
            Connection cnx = MyDataBase.getInstance().getConnection();
            PreparedStatement ps = cnx.prepareStatement(
                    "SELECT nom, prenom FROM utilisateurs WHERE id_utilisateur=?"
            );
            ps.setInt(1, idLivreur);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return rs.getString("nom") + " " + rs.getString("prenom");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return "-";
    }

    private String getNumeroFacture(Integer id) {
        if (id == null) return "-";
        try {
            Connection cnx = MyDataBase.getInstance().getConnection();
            PreparedStatement ps = cnx.prepareStatement(
                    "SELECT numero FROM factures WHERE ID_Facture=?"
            );
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return rs.getString("numero");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return "-";
    }
}