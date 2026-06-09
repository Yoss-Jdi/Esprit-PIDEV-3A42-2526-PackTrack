package com.esprit.utils;

import java.sql.Connection;        // ✅ import ajouté
import java.sql.DriverManager;
import java.sql.SQLException;
public class MyDataBase {

    private final String USERNAME = "root";
    private final String URL = "jdbc:mysql://localhost:3306/TrackPackDB";
    private final String PASSWORD = "0000";

    private Connection connection;  // ✅ import résolu

    private static MyDataBase instance;  // ✅ Singleton

    private MyDataBase() {
        try {
            connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            System.out.println("✅ Connection established !");
        } catch (SQLException e) {
            System.out.println("❌ Error: " + e.getMessage());
        }
    }

    // ✅ Une seule instance dans tout le projet
    public static MyDataBase getInstance() {
        if (instance == null) {
            instance = new MyDataBase();
        }
        return instance;
    }

    public Connection getConnection() {
        return connection;

    }
}