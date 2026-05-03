package com.example.forumapp.controllers;

import com.example.forumapp.entities.User;
import com.example.forumapp.services.AuthService;
import com.example.forumapp.utils.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    private final AuthService authService;
    private final SceneManager sceneManager;

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;

    public LoginController(AuthService authService, SceneManager sceneManager) {
        this.authService = authService;
        this.sceneManager = sceneManager;
    }

    @FXML
    private void handleLogin() {
        try {
            User user = authService.login(emailField.getText(), passwordField.getText());
            if (user.isAdmin()) {
                sceneManager.showAdminDashboard();
            } else {
                sceneManager.showUserDashboard();
            }
        } catch (RuntimeException e) {
            statusLabel.setText(e.getMessage());
        }
    }

    @FXML
    private void fillAdminCredentials() {
        emailField.setText("admin@test.com");
        passwordField.setText("admin123");
    }

    @FXML
    private void fillUserCredentials() {
        emailField.setText("user@test.com");
        passwordField.setText("user123");
    }
}
