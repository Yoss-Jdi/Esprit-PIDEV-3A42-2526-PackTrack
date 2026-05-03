module com.example.forumapp {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    // MODERATION: Module pour les requêtes HTTP (ModerationService)
    requires java.net.http;

    opens com.example.forumapp to javafx.fxml;
    opens com.example.forumapp.controllers to javafx.fxml;
    opens com.example.forumapp.entities to javafx.fxml;

    exports com.example.forumapp;
    exports com.example.forumapp.controllers;
    exports com.example.forumapp.entities;
    exports com.example.forumapp.services;
    exports com.example.forumapp.utils;
    exports com.example.forumapp.tests;
    requires org.apache.pdfbox;  // ← ajoute cette ligne
    requires org.json;
} 
