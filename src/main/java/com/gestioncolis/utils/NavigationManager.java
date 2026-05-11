package com.gestioncolis.utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.animation.FadeTransition;
import javafx.util.Duration;
import java.io.IOException;

/**
 * Gestionnaire centralisé pour les navigations et retours entre vues.
 * Permet de revenir à la vue précédente facilement.
 */
public class NavigationManager {
    private static NavigationManager instance;
    
    // Historique des vues chargées
    private String previousViewPath;
    private Scene previousScene;
    private Stage currentStage;
    
    private NavigationManager() {
    }
    
    public static NavigationManager getInstance() {
        if (instance == null) {
            instance = new NavigationManager();
        }
        return instance;
    }
    
    /**
     * Sauvegarde la vue actuelle avant de naviguer vers une nouvelle
     */
    public void setPreviousScene(Scene scene) {
        this.previousScene = scene;
    }
    
    public void navigateTo(String fxmlPath, Stage stage, double width, double height, String title) {
        try {
            // Sauvegarder la scène actuelle
            if (stage.getScene() != null && stage.getScene().getRoot() != null) {
                previousScene = stage.getScene();
                previousViewPath = fxmlPath;
            }
            currentStage = stage;
            
            // Charger la nouvelle vue
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            javafx.scene.Parent view = loader.load();
            Scene scene = new Scene(view, width, height);
            
            // Animation de transition
            if (stage.getScene() != null && stage.getScene().getRoot() != null) {
                FadeTransition ft = new FadeTransition(Duration.millis(300), stage.getScene().getRoot());
                ft.setToValue(0);
                ft.setOnFinished(e -> {
                    stage.setScene(scene);
                    stage.setTitle(title);
                    
                    FadeTransition ftIn = new FadeTransition(Duration.millis(300), scene.getRoot());
                    ftIn.setFromValue(0);
                    ftIn.setToValue(1);
                    ftIn.play();
                });
                ft.play();
            } else {
                stage.setScene(scene);
                stage.setTitle(title);
            }
        } catch (IOException e) {
            System.err.println("Erreur navigation vers " + fxmlPath + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Revient à la vue précédente
     */
    public void goBack() {
        if (previousScene != null && currentStage != null) {
            try {
                FadeTransition ft = new FadeTransition(Duration.millis(300), currentStage.getScene().getRoot());
                ft.setToValue(0);
                ft.setOnFinished(e -> {
                    currentStage.setScene(previousScene);
                    currentStage.setTitle("TrackPack — Accueil");
                    currentStage.setMinWidth(1024);
                    currentStage.setMinHeight(700);
                    
                    FadeTransition ftIn = new FadeTransition(Duration.millis(300), previousScene.getRoot());
                    ftIn.setFromValue(0);
                    ftIn.setToValue(1);
                    ftIn.play();
                    
                    // Réinitialiser après succès
                    reset();
                });
                ft.play();
            } catch (Exception ex) {
                System.err.println("Erreur lors du retour: " + ex.getMessage());
                ex.printStackTrace();
            }
        } else {
            System.err.println("Impossible de revenir : pas de vue précédente (previousScene=" + previousScene + ", currentStage=" + currentStage + ")");
        }
    }
    
    public String getPreviousViewPath() {
        return previousViewPath;
    }
    
    public Scene getPreviousScene() {
        return previousScene;
    }
    
    public void reset() {
        previousViewPath = null;
        previousScene = null;
        currentStage = null;
    }
}

