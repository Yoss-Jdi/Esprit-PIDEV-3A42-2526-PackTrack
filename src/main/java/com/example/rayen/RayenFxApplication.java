package com.example.rayen;

import com.example.rayen.services.Servicetechnicien;
import com.example.rayen.services.OllamaService;
import com.example.rayen.services.Servicevehicule;
import javafx.application.Application;
import javafx.stage.Stage;

import java.io.IOException;

public class RayenFxApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        Servicevehicule servicevehicule = new Servicevehicule();
        Servicetechnicien servicetechnicien = new Servicetechnicien(servicevehicule);
        OllamaService ollamaService = new OllamaService();
        SceneNavigator sceneNavigator = new SceneNavigator(stage, servicevehicule, servicetechnicien, ollamaService);
        sceneNavigator.showDisplayView();
    }
}
