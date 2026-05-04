package com.gestioncolis.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyConnection {
    private static MyConnection instance;
    private Connection connection;

    private static final String URL      = "jdbc:mysql://localhost:3306/trackpackdb";
    private static final String USER     = "root";
    private static final String PASSWORD = "";

    private MyConnection() {
        try {
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Connexion réussie ");
        } catch (SQLException e) {
            System.err.println("Erreur de connexion : " + e.getMessage());
        }
    }

    public static MyConnection getInstance() {
        if (instance == null) instance = new MyConnection();
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }
}