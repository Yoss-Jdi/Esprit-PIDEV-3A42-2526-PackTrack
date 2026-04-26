package com.gestioncolis.utils;

import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import com.gestioncolis.entities.Utilisateurs;
import com.gestioncolis.services.UtilisateursServices;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class PasswordResetDialog {

    private static final Map<String, ResetRequest> resetRequests = new HashMap<>();

    static class ResetRequest {
        String code;
        long expiryTime;
        String email;

        ResetRequest(String email, String code) {
            this.email = email;
            this.code = code;
            this.expiryTime = System.currentTimeMillis() + 15 * 60 * 1000; // 15 minutes
        }

        boolean isValid() {
            return System.currentTimeMillis() < expiryTime;
        }
    }

    public static void show(Stage owner) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.TRANSPARENT);
        dialog.initOwner(owner);

        // Conteneur principal
        VBox mainContainer = new VBox(20);
        mainContainer.setStyle("-fx-background-color: white; -fx-background-radius: 28;");
        mainContainer.setPadding(new Insets(32, 36, 32, 36));
        mainContainer.setMaxWidth(480);
        mainContainer.setEffect(new DropShadow(25, Color.rgb(0, 0, 0, 0.2)));

        // Animation d'entrée
        mainContainer.setScaleX(0.9);
        mainContainer.setScaleY(0.9);
        mainContainer.setOpacity(0);

        // ===== ÉTAPE 1: Demande d'email =====
        VBox step1 = createEmailStep(mainContainer, dialog);

        // ===== ÉTAPE 2: Code de vérification =====
        VBox step2 = createCodeStep(mainContainer, dialog);

        // ===== ÉTAPE 3: Nouveau mot de passe =====
        VBox step3 = createNewPasswordStep(mainContainer, dialog);

        mainContainer.getChildren().addAll(step1, step2, step3);

        Scene scene = new Scene(mainContainer);
        scene.setFill(Color.TRANSPARENT);
        dialog.setScene(scene);

        // Animation d'apparition
        ScaleTransition st = new ScaleTransition(Duration.millis(200), mainContainer);
        st.setFromX(0.9);
        st.setFromY(0.9);
        st.setToX(1);
        st.setToY(1);

        FadeTransition ft = new FadeTransition(Duration.millis(200), mainContainer);
        ft.setFromValue(0);
        ft.setToValue(1);

        ParallelTransition pt = new ParallelTransition(st, ft);
        pt.play();

        dialog.showAndWait();
    }

    private static VBox createEmailStep(VBox mainContainer, Stage dialog) {
        VBox step = new VBox(20);
        step.setAlignment(Pos.CENTER);

        // Header
        StackPane iconContainer = new StackPane();
        iconContainer.setPrefSize(70, 70);
        iconContainer.setStyle("-fx-background-color: #eef2ff; -fx-background-radius: 35;");
        Text icon = new Text("📧");
        icon.setStyle("-fx-font-size: 32px;");
        iconContainer.getChildren().add(icon);

        Label title = new Label("Mot de passe oublié ?");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #1e1b4b;");

        Label subtitle = new Label("Entrez votre adresse email et nous vous enverrons\nun code de réinitialisation");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");
        subtitle.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        // Champ email
        VBox emailField = new VBox(6);
        Label emailLabel = new Label("Adresse email");
        emailLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #374151;");

        TextField emailInput = new TextField();
        emailInput.setPromptText("votre@email.com");
        emailInput.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #e2e8f0; -fx-border-radius: 12; -fx-background-radius: 12; -fx-padding: 12 16; -fx-font-size: 13px;");
        emailInput.getStyleClass().add("email-field");

        emailField.getChildren().addAll(emailLabel, emailInput);

        // Message d'erreur
        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 11px;");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        // Boutons
        HBox buttonBox = new HBox(12);
        buttonBox.setAlignment(Pos.CENTER);

        Button cancelBtn = createButton("Annuler", "#64748b", false);
        Button sendBtn = createButton("Envoyer le code", "#6366f1", true);

        cancelBtn.setOnAction(e -> dialog.close());
        sendBtn.setOnAction(e -> {
            String email = emailInput.getText().trim();
            if (email.isEmpty()) {
                showError(errorLabel, "Veuillez entrer votre adresse email");
                animateShakeNode(emailInput);
                return;
            }
            if (!email.matches("^[\\w.%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$")) {
                showError(errorLabel, "Format d'email invalide");
                animateShakeNode(emailInput);
                return;
            }

            // Vérifier si l'email existe
            sendBtn.setDisable(true);
            sendBtn.setText("⏳ Vérification...");

            new Thread(() -> {
                try {
                    UtilisateursServices service = new UtilisateursServices();
                    boolean exists = service.afficher().stream()
                            .anyMatch(u -> u.getEmail().equalsIgnoreCase(email));

                    javafx.application.Platform.runLater(() -> {
                        if (exists) {
                            // Générer et envoyer le code
                            String resetCode = EmailService.generateResetCode();
                            boolean sent = EmailService.sendResetCode(email, resetCode, email.split("@")[0]);

                            if (sent) {
                                // Stocker la demande
                                resetRequests.put(email, new ResetRequest(email, resetCode));

                                // Animation de succès
                                showSuccessAndTransition(mainContainer, step, () -> {
                                    VBox step2 = (VBox) mainContainer.getChildren().get(1);
                                    step2.setUserData(email); // Stocker l'email
                                    step2.setVisible(true);
                                    step2.setManaged(true);
                                });
                            } else {
                                showError(errorLabel, "Erreur d'envoi d'email. Réessayez plus tard.");
                                sendBtn.setDisable(false);
                                sendBtn.setText("Envoyer le code");
                            }
                        } else {
                            showError(errorLabel, "Aucun compte trouvé avec cet email");
                            sendBtn.setDisable(false);
                            sendBtn.setText("Envoyer le code");
                            animateShakeNode(emailInput);
                        }
                    });
                } catch (SQLException ex) {
                    javafx.application.Platform.runLater(() -> {
                        showError(errorLabel, "Erreur technique. Réessayez plus tard.");
                        sendBtn.setDisable(false);
                        sendBtn.setText("Envoyer le code");
                    });
                }
            }).start();
        });

        buttonBox.getChildren().addAll(cancelBtn, sendBtn);

        step.getChildren().addAll(iconContainer, title, subtitle, emailField, errorLabel, buttonBox);
        return step;
    }

    private static VBox createCodeStep(VBox mainContainer, Stage dialog) {
        VBox step = new VBox(20);
        step.setAlignment(Pos.CENTER);
        step.setVisible(false);
        step.setManaged(false);

        // Header
        StackPane iconContainer = new StackPane();
        iconContainer.setPrefSize(70, 70);
        iconContainer.setStyle("-fx-background-color: #fef3c7; -fx-background-radius: 35;");
        Text icon = new Text("🔐");
        icon.setStyle("-fx-font-size: 32px;");
        iconContainer.getChildren().add(icon);

        Label title = new Label("Code de vérification");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #1e1b4b;");

        Label subtitle = new Label("Nous avons envoyé un code à votre adresse email.\nEntrez-le ci-dessous pour continuer.");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");
        subtitle.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        // Champ code (6 digits)
        HBox codeFields = new HBox(10);
        codeFields.setAlignment(Pos.CENTER);
        TextField[] codeDigits = new TextField[6];

        for (int i = 0; i < 6; i++) {
            final int index = i;
            TextField digit = new TextField();
            digit.setPrefWidth(50);
            digit.setPrefHeight(60);
            digit.setAlignment(Pos.CENTER);
            digit.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-background-color: #f8fafc; -fx-border-color: #e2e8f0; -fx-border-radius: 12; -fx-background-radius: 12;");
            digit.textProperty().addListener((obs, old, val) -> {
                if (val != null && val.length() > 1) digit.setText(val.substring(0, 1));
                if (val != null && val.length() == 1 && index < 5) {
                    codeDigits[index + 1].requestFocus();
                }
                if ((val == null || val.isEmpty()) && index > 0) {
                    codeDigits[index - 1].requestFocus();
                }
            });
            codeDigits[i] = digit;
            codeFields.getChildren().add(digit);
        }

        // Message d'erreur
        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 11px;");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        // Timer
        Label timerLabel = new Label("Le code expire dans 15:00");
        timerLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #f59e0b;");

        // Boutons
        HBox buttonBox = new HBox(12);
        buttonBox.setAlignment(Pos.CENTER);

        Button backBtn = createButton("Retour", "#64748b", false);
        Button verifyBtn = createButton("Vérifier", "#6366f1", true);

        backBtn.setOnAction(e -> {
            step.setVisible(false);
            step.setManaged(false);
            mainContainer.getChildren().get(0).setVisible(true);
            mainContainer.getChildren().get(0).setManaged(true);
        });

        verifyBtn.setOnAction(e -> {
            StringBuilder code = new StringBuilder();
            for (TextField digit : codeDigits) {
                if (digit.getText().isEmpty()) {
                    showError(errorLabel, "Code incomplet");
                    return;
                }
                code.append(digit.getText());
            }

            String email = (String) step.getUserData();
            ResetRequest request = resetRequests.get(email);

            if (request == null || !request.isValid()) {
                showError(errorLabel, "Code expiré. Veuillez recommencer.");
                return;
            }

            if (request.code.equals(code.toString())) {
                // Code valide, passer à l'étape 3
                showSuccessAndTransition(mainContainer, step, () -> {
                    VBox step3 = (VBox) mainContainer.getChildren().get(2);
                    step3.setUserData(email);
                    step3.setVisible(true);
                    step3.setManaged(true);
                });
            } else {
                showError(errorLabel, "Code invalide");
                animateShakeNode(codeFields);
            }
        });

        buttonBox.getChildren().addAll(backBtn, verifyBtn);

        step.getChildren().addAll(iconContainer, title, subtitle, codeFields, errorLabel, timerLabel, buttonBox);

        // Démarrer le timer
        startTimer(timerLabel, () -> {
            errorLabel.setText("Le code a expiré. Veuillez recommencer.");
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
            verifyBtn.setDisable(true);
        });

        return step;
    }

    private static VBox createNewPasswordStep(VBox mainContainer, Stage dialog) {
        VBox step = new VBox(20);
        step.setAlignment(Pos.CENTER);
        step.setVisible(false);
        step.setManaged(false);

        // Header
        StackPane iconContainer = new StackPane();
        iconContainer.setPrefSize(70, 70);
        iconContainer.setStyle("-fx-background-color: #dcfce7; -fx-background-radius: 35;");
        Text icon = new Text("🔒");
        icon.setStyle("-fx-font-size: 32px;");
        iconContainer.getChildren().add(icon);

        Label title = new Label("Nouveau mot de passe");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #1e1b4b;");

        Label subtitle = new Label("Choisissez un mot de passe sécurisé pour votre compte");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");

        // Champ mot de passe
        VBox passField = new VBox(6);
        Label passLabel = new Label("Nouveau mot de passe");
        passLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #374151;");

        PasswordField passwordInput = new PasswordField();
        passwordInput.setPromptText("Minimum 6 caractères");
        passwordInput.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #e2e8f0; -fx-border-radius: 12; -fx-background-radius: 12; -fx-padding: 12 16; -fx-font-size: 13px;");

        // Force du mot de passe
        HBox strengthBox = new HBox(8);
        strengthBox.setAlignment(Pos.CENTER_LEFT);
        Rectangle[] strengthSegments = new Rectangle[4];
        for (int i = 0; i < 4; i++) {
            Rectangle rect = new Rectangle(70, 4);
            rect.setArcWidth(2);
            rect.setArcHeight(2);
            rect.setFill(Color.web("#e5e7eb"));
            strengthSegments[i] = rect;
            strengthBox.getChildren().add(rect);
        }

        passwordInput.textProperty().addListener((obs, old, val) -> {
            int score = calculateStrength(val);
            for (int i = 0; i < 4; i++) {
                Color color;
                if (i < score) {
                    if (score == 1) color = Color.web("#ef4444");
                    else if (score == 2) color = Color.web("#f59e0b");
                    else if (score == 3) color = Color.web("#6366f1");
                    else color = Color.web("#22c55e");
                } else {
                    color = Color.web("#e5e7eb");
                }
                strengthSegments[i].setFill(color);
            }
        });

        passField.getChildren().addAll(passLabel, passwordInput, strengthBox);

        // Confirmation
        VBox confirmField = new VBox(6);
        Label confirmLabel = new Label("Confirmer le mot de passe");
        confirmLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #374151;");

        PasswordField confirmInput = new PasswordField();
        confirmInput.setPromptText("Répéter le mot de passe");
        confirmInput.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #e2e8f0; -fx-border-radius: 12; -fx-background-radius: 12; -fx-padding: 12 16; -fx-font-size: 13px;");

        confirmField.getChildren().addAll(confirmLabel, confirmInput);

        // Message d'erreur
        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 11px;");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        // Boutons
        HBox buttonBox = new HBox(12);
        buttonBox.setAlignment(Pos.CENTER);

        Button backBtn = createButton("Retour", "#64748b", false);
        Button resetBtn = createButton("Réinitialiser", "#22c55e", true);

        backBtn.setOnAction(e -> {
            step.setVisible(false);
            step.setManaged(false);
            VBox step2 = (VBox) mainContainer.getChildren().get(1);
            step2.setVisible(true);
            step2.setManaged(true);
        });

        resetBtn.setOnAction(e -> {
            String password = passwordInput.getText();
            String confirm = confirmInput.getText();

            if (password == null || password.isEmpty()) {
                showError(errorLabel, "Veuillez entrer un mot de passe");
                animateShakeNode(passwordInput);
                return;
            }
            if (password.length() < 6) {
                showError(errorLabel, "Minimum 6 caractères");
                animateShakeNode(passwordInput);
                return;
            }
            if (!password.equals(confirm)) {
                showError(errorLabel, "Les mots de passe ne correspondent pas");
                animateShakeNode(confirmInput);
                return;
            }

            resetBtn.setDisable(true);
            resetBtn.setText("⏳ Mise à jour...");

            String email = (String) step.getUserData();

            new Thread(() -> {
                try {
                    UtilisateursServices service = new UtilisateursServices();
                    var users = service.afficher();
                    Utilisateurs target = users.stream()
                            .filter(u -> u.getEmail().equalsIgnoreCase(email))
                            .findFirst()
                            .orElse(null);

                    if (target != null) {
                        target.setMotDePasse(password);
                        service.modifier(target);

                        javafx.application.Platform.runLater(() -> {
                            // Animation de succès
                            VBox successContent = new VBox(15);
                            successContent.setAlignment(Pos.CENTER);
                            successContent.setPadding(new Insets(20));

                            Text successIcon = new Text("✅");
                            successIcon.setStyle("-fx-font-size: 48px;");

                            Label successTitle = new Label("Mot de passe réinitialisé !");
                            successTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #22c55e;");

                            Label successMsg = new Label("Votre mot de passe a été modifié avec succès.\nVous pouvez maintenant vous connecter.");
                            successMsg.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");
                            successMsg.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

                            Button closeBtn = createButton("Se connecter", "#22c55e", true);
                            closeBtn.setOnAction(ev -> dialog.close());

                            successContent.getChildren().addAll(successIcon, successTitle, successMsg, closeBtn);

                            mainContainer.getChildren().clear();
                            mainContainer.getChildren().add(successContent);

                            resetRequests.remove(email);
                        });
                    }
                } catch (SQLException ex) {
                    javafx.application.Platform.runLater(() -> {
                        showError(errorLabel, "Erreur technique. Réessayez plus tard.");
                        resetBtn.setDisable(false);
                        resetBtn.setText("Réinitialiser");
                    });
                }
            }).start();
        });

        buttonBox.getChildren().addAll(backBtn, resetBtn);

        step.getChildren().addAll(iconContainer, title, subtitle, passField, confirmField, errorLabel, buttonBox);
        return step;
    }

    private static int calculateStrength(String password) {
        if (password == null) return 0;
        int score = 0;
        if (password.length() >= 6) score++;
        if (password.length() >= 10) score++;
        if (password.matches(".*[A-Z].*") && password.matches(".*[a-z].*")) score++;
        if (password.matches(".*[0-9].*") || password.matches(".*[^a-zA-Z0-9].*")) score++;
        return Math.min(4, score);
    }

    private static Button createButton(String text, String color, boolean isPrimary) {
        Button btn = new Button(text);
        btn.setCursor(javafx.scene.Cursor.HAND);

        if (isPrimary) {
            btn.setStyle(
                    "-fx-background-color: " + color + ";" +
                            "-fx-text-fill: white;" +
                            "-fx-font-weight: bold;" +
                            "-fx-font-size: 13px;" +
                            "-fx-background-radius: 30;" +
                            "-fx-padding: 12 28 12 28;" +
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
                            "-fx-padding: 10.5 28 10.5 28;"
            );
        }

        addHoverEffect(btn, isPrimary, color);
        return btn;
    }

    private static void addHoverEffect(Button btn, boolean isPrimary, String color) {
        btn.setOnMouseEntered(e -> {
            if (isPrimary) {
                btn.setStyle(
                        "-fx-background-color: " + color + "dd;" +
                                "-fx-text-fill: white;" +
                                "-fx-font-weight: bold;" +
                                "-fx-font-size: 13px;" +
                                "-fx-background-radius: 30;" +
                                "-fx-padding: 12 28 12 28;" +
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
                                "-fx-padding: 10.5 28 10.5 28;"
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
                                "-fx-padding: 12 28 12 28;" +
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
                                "-fx-padding: 10.5 28 10.5 28;"
                );
            }
            ScaleTransition st = new ScaleTransition(Duration.millis(150), btn);
            st.setToX(1);
            st.setToY(1);
            st.play();
        });
    }

    private static void showError(Label label, String message) {
        label.setText(message);
        label.setVisible(true);
        label.setManaged(true);

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.seconds(3), e -> {
                    label.setVisible(false);
                    label.setManaged(false);
                })
        );
        timeline.play();
    }

    // Méthode corrigée pour accepter n'importe quel Node
    private static void animateShakeNode(Node node) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(50), node);
        tt.setByX(8);
        tt.setCycleCount(6);
        tt.setAutoReverse(true);
        tt.play();
    }

    private static void showSuccessAndTransition(VBox container, VBox currentStep, Runnable onSuccess) {
        // Animation de succès
        ScaleTransition st = new ScaleTransition(Duration.millis(150), currentStep);
        st.setToX(0.95);
        st.setToY(0.95);

        FadeTransition ft = new FadeTransition(Duration.millis(150), currentStep);
        ft.setToValue(0);

        ParallelTransition pt = new ParallelTransition(st, ft);
        pt.setOnFinished(e -> {
            currentStep.setVisible(false);
            currentStep.setManaged(false);
            onSuccess.run();

            // Animation d'entrée de la prochaine étape
            Node nextStep = container.getChildren().stream()
                    .filter(n -> n.isVisible())
                    .findFirst()
                    .orElse(null);
            if (nextStep != null) {
                nextStep.setScaleX(0.95);
                nextStep.setScaleY(0.95);
                nextStep.setOpacity(0);

                ScaleTransition stIn = new ScaleTransition(Duration.millis(200), nextStep);
                stIn.setToX(1);
                stIn.setToY(1);

                FadeTransition ftIn = new FadeTransition(Duration.millis(200), nextStep);
                ftIn.setToValue(1);

                new ParallelTransition(stIn, ftIn).play();
            }
        });
        pt.play();
    }

    private static void startTimer(Label timerLabel, Runnable onExpire) {
        Timer timer = new Timer(true);
        long startTime = System.currentTimeMillis();

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                long elapsed = System.currentTimeMillis() - startTime;
                long remaining = 15 * 60 * 1000 - elapsed;

                if (remaining <= 0) {
                    timer.cancel();
                    javafx.application.Platform.runLater(onExpire);
                } else {
                    long minutes = remaining / 60000;
                    long seconds = (remaining % 60000) / 1000;
                    javafx.application.Platform.runLater(() ->
                            timerLabel.setText(String.format("Le code expire dans %02d:%02d", minutes, seconds))
                    );
                }
            }
        }, 0, 1000);
    }
}