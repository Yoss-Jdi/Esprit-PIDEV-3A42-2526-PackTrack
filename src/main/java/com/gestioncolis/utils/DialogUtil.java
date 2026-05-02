package com.gestioncolis.utils;

import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.util.Optional;
import java.util.function.Consumer;

public class DialogUtil {

    /**
     * Affiche une boîte de dialogue moderne pour confirmation de suppression
     */
    public static boolean showDeleteConfirmation(String userName, String userInfo) {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.TRANSPARENT);

        // Création du contenu personnalisé
        VBox content = createDialogContent(
                "🗑️",
                "Supprimer l'utilisateur",
                "Êtes-vous sûr de vouloir supprimer définitivement ce compte ?",
                "Utilisateur : " + userName,
                userInfo,
                "Cette action est irréversible et toutes les données associées seront perdues.",
                "#ef4444"
        );

        // Boutons
        HBox buttonBox = createButtonRow(
                "Annuler", "#64748b", false,
                "Supprimer", "#ef4444", true,
                dialog
        );

        content.getChildren().add(buttonBox);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setStyle("-fx-background-color: transparent; -fx-padding: 0;");

        // Animation d'entrée
        content.setScaleX(0.9);
        content.setScaleY(0.9);
        content.setOpacity(0);

        Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
        stage.getScene().setFill(Color.TRANSPARENT);

        // Animation d'apparition
        ScaleTransition st = new ScaleTransition(Duration.millis(200), content);
        st.setFromX(0.9);
        st.setFromY(0.9);
        st.setToX(1);
        st.setToY(1);

        FadeTransition ft = new FadeTransition(Duration.millis(200), content);
        ft.setFromValue(0);
        ft.setToValue(1);

        ParallelTransition pt = new ParallelTransition(st, ft);
        pt.play();

        dialog.setResultConverter(button -> {
            ButtonBar.ButtonData buttonData = button == null ? null : button.getButtonData();
            return buttonData == ButtonBar.ButtonData.OK_DONE;
        });

        Optional<Boolean> result = dialog.showAndWait();
        return result.isPresent() && result.get();
    }

    /**
     * Affiche une boîte de dialogue moderne pour confirmation d'export
     */
    public static boolean showExportConfirmation(String exportType, int count, String details) {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.TRANSPARENT);

        String icon = exportType.equals("PDF") ? "📄" : "📊";
        String color = exportType.equals("PDF") ? "#ef4444" : "#10b981";
        String title = "Exporter en " + exportType;
        String actionText = "Exporter " + exportType;

        VBox content = createDialogContent(
                icon,
                title,
                "Confirmation d'export",
                "Vous allez exporter " + count + " utilisateur(s)",
                details,
                "Le fichier sera enregistré sur votre disque.",
                color
        );

        HBox buttonBox = createButtonRow(
                "Annuler", "#64748b", false,
                actionText, color, true,
                dialog
        );

        content.getChildren().add(buttonBox);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setStyle("-fx-background-color: transparent; -fx-padding: 0;");

        // Animation d'entrée
        content.setScaleX(0.9);
        content.setScaleY(0.9);
        content.setOpacity(0);

        Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
        stage.getScene().setFill(Color.TRANSPARENT);

        ScaleTransition st = new ScaleTransition(Duration.millis(200), content);
        st.setFromX(0.9);
        st.setFromY(0.9);
        st.setToX(1);
        st.setToY(1);

        FadeTransition ft = new FadeTransition(Duration.millis(200), content);
        ft.setFromValue(0);
        ft.setToValue(1);

        ParallelTransition pt = new ParallelTransition(st, ft);
        pt.play();

        dialog.setResultConverter(button -> {
            ButtonBar.ButtonData buttonData = button == null ? null : button.getButtonData();
            return buttonData == ButtonBar.ButtonData.OK_DONE;
        });

        Optional<Boolean> result = dialog.showAndWait();
        return result.isPresent() && result.get();
    }

    /**
     * Crée le contenu principal du dialogue
     */
    private static VBox createDialogContent(String icon, String title, String subtitle,
                                            String mainInfo, String secondaryInfo,
                                            String warning, String accentColor) {
        VBox content = new VBox(20);
        content.setStyle("-fx-background-color: white; -fx-background-radius: 24;");
        content.setPadding(new Insets(28, 32, 28, 32));
        content.setMaxWidth(420);
        content.setEffect(new DropShadow(20, Color.rgb(0, 0, 0, 0.15)));

        // Cercle icône animé
        StackPane iconContainer = new StackPane();
        iconContainer.setPrefSize(64, 64);
        iconContainer.setStyle("-fx-background-color: " + accentColor + "15; -fx-background-radius: 32;");

        Text iconText = new Text(icon);
        iconText.setStyle("-fx-font-size: 32px;");
        iconContainer.getChildren().add(iconText);

        // Animation de pulsation sur l'icône
        ScaleTransition pulse = new ScaleTransition(Duration.millis(1500), iconContainer);
        pulse.setFromX(1);
        pulse.setFromY(1);
        pulse.setToX(1.08);
        pulse.setToY(1.08);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.setAutoReverse(true);
        pulse.play();

        // Titre
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        titleLabel.setWrapText(true);

        // Sous-titre
        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");
        subtitleLabel.setWrapText(true);

        // Séparateur
        Separator separator = new Separator();
        separator.setStyle("-fx-background-color: #f1f5f9;");

        // Carte d'information principale
        VBox infoCard = new VBox(8);
        infoCard.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 16;");
        infoCard.setPadding(new Insets(16));

        Label mainInfoLabel = new Label(mainInfo);
        mainInfoLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");

        Label secondaryInfoLabel = new Label(secondaryInfo);
        secondaryInfoLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569;");
        secondaryInfoLabel.setWrapText(true);

        infoCard.getChildren().addAll(mainInfoLabel, secondaryInfoLabel);

        // Avertissement
        HBox warningBox = new HBox(10);
        warningBox.setAlignment(Pos.CENTER_LEFT);
        warningBox.setStyle("-fx-background-color: #fef2f2; -fx-background-radius: 12;");
        warningBox.setPadding(new Insets(12, 16, 12, 16));

        Text warningIcon = new Text("⚠️");
        warningIcon.setStyle("-fx-font-size: 14px;");

        Label warningLabel = new Label(warning);
        warningLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #dc2626;");
        warningLabel.setWrapText(true);
        HBox.setHgrow(warningLabel, Priority.ALWAYS);

        warningBox.getChildren().addAll(warningIcon, warningLabel);

        // Layout principal
        VBox textBox = new VBox(12);
        textBox.setAlignment(Pos.CENTER_LEFT);
        textBox.getChildren().addAll(iconContainer, titleLabel, subtitleLabel, separator, infoCard, warningBox);

        content.getChildren().add(textBox);
        return content;
    }

    /**
     * Crée la ligne de boutons avec animations
     */
    private static HBox createButtonRow(String cancelText, String cancelColor,
                                        boolean cancelPrimary, String confirmText,
                                        String confirmColor, boolean confirmPrimary,
                                        Dialog<Boolean> dialog) {
        HBox buttonBox = new HBox(16);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(8, 0, 0, 0));

        // Bouton Annuler
        Button cancelBtn = createModernButton(cancelText, cancelColor, !cancelPrimary);
        cancelBtn.setPrefWidth(130);
        cancelBtn.setOnAction(e -> {
            animateButtonClick(cancelBtn);
            dialog.setResult(false);
            dialog.close();
        });

        // Bouton Confirmer
        Button confirmBtn = createModernButton(confirmText, confirmColor, confirmPrimary);
        confirmBtn.setPrefWidth(130);
        confirmBtn.setOnAction(e -> {
            animateButtonClick(confirmBtn);
            dialog.setResult(true);
            dialog.close();
        });

        buttonBox.getChildren().addAll(cancelBtn, confirmBtn);
        return buttonBox;
    }

    /**
     * Crée un bouton moderne avec style
     */
    private static Button createModernButton(String text, String color, boolean isPrimary) {
        Button btn = new Button(text);
        btn.setCursor(javafx.scene.Cursor.HAND);

        if (isPrimary) {
            btn.setStyle(
                    "-fx-background-color: " + color + ";" +
                            "-fx-text-fill: white;" +
                            "-fx-font-weight: bold;" +
                            "-fx-font-size: 13px;" +
                            "-fx-background-radius: 30;" +
                            "-fx-padding: 12 24 12 24;" +
                            "-fx-effect: dropshadow(gaussian, " + color + "40, 10, 0, 0, 3);"
            );
        } else {
            btn.setStyle(
                    "-fx-background-color: transparent;" +
                            "-fx-text-fill: " + color + ";" +
                            "-fx-font-weight: bold;" +
                            "-fx-font-size: 13px;" +
                            "-fx-background-radius: 30;" +
                            "-fx-border-color: " + color + ";" +
                            "-fx-border-width: 1.5;" +
                            "-fx-border-radius: 30;" +
                            "-fx-padding: 10.5 24 10.5 24;"
            );
        }

        // Animation hover
        btn.setOnMouseEntered(e -> {
            if (isPrimary) {
                btn.setStyle(
                        "-fx-background-color: " + color + "dd;" +
                                "-fx-text-fill: white;" +
                                "-fx-font-weight: bold;" +
                                "-fx-font-size: 13px;" +
                                "-fx-background-radius: 30;" +
                                "-fx-padding: 12 24 12 24;" +
                                "-fx-effect: dropshadow(gaussian, " + color + "60, 15, 0, 0, 5);"
                );
            } else {
                btn.setStyle(
                        "-fx-background-color: " + color + "10;" +
                                "-fx-text-fill: " + color + ";" +
                                "-fx-font-weight: bold;" +
                                "-fx-font-size: 13px;" +
                                "-fx-background-radius: 30;" +
                                "-fx-border-color: " + color + ";" +
                                "-fx-border-width: 1.5;" +
                                "-fx-border-radius: 30;" +
                                "-fx-padding: 10.5 24 10.5 24;"
                );
            }
            ScaleTransition st = new ScaleTransition(Duration.millis(150), btn);
            st.setToX(1.02);
            st.setToY(1.02);
            st.play();
        });

        btn.setOnMouseExited(e -> {
            if (isPrimary) {
                btn.setStyle(
                        "-fx-background-color: " + color + ";" +
                                "-fx-text-fill: white;" +
                                "-fx-font-weight: bold;" +
                                "-fx-font-size: 13px;" +
                                "-fx-background-radius: 30;" +
                                "-fx-padding: 12 24 12 24;" +
                                "-fx-effect: dropshadow(gaussian, " + color + "40, 10, 0, 0, 3);"
                );
            } else {
                btn.setStyle(
                        "-fx-background-color: transparent;" +
                                "-fx-text-fill: " + color + ";" +
                                "-fx-font-weight: bold;" +
                                "-fx-font-size: 13px;" +
                                "-fx-background-radius: 30;" +
                                "-fx-border-color: " + color + ";" +
                                "-fx-border-width: 1.5;" +
                                "-fx-border-radius: 30;" +
                                "-fx-padding: 10.5 24 10.5 24;"
                );
            }
            ScaleTransition st = new ScaleTransition(Duration.millis(150), btn);
            st.setToX(1);
            st.setToY(1);
            st.play();
        });

        return btn;
    }

    /**
     * Animation au clic du bouton
     */
    private static void animateButtonClick(Button btn) {
        ScaleTransition st = new ScaleTransition(Duration.millis(100), btn);
        st.setToX(0.96);
        st.setToY(0.96);
        st.setAutoReverse(true);
        st.setCycleCount(2);
        st.play();
    }
}