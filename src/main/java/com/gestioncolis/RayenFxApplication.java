package com.gestioncolis;

import com.gestioncolis.services.OllamaService;
import com.gestioncolis.services.Servicetechnicien;
import com.gestioncolis.services.Servicevehicule;
import javafx.application.Application;
import javafx.stage.Stage;

import java.io.IOException;

public class RayenFxApplication extends Application {
    @Override
    public void start(Stage stage) {
        Servicevehicule servicevehicule = new Servicevehicule();
        Servicetechnicien servicetechnicien = new Servicetechnicien(servicevehicule);
        OllamaService ollamaService = new OllamaService();
        SceneNavigator sceneNavigator = new SceneNavigator(stage, servicevehicule, servicetechnicien, ollamaService);
        try {
            sceneNavigator.showDisplayView();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
