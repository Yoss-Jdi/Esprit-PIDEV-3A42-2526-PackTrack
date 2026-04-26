package com.gestioncolis.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDataBase {

    private static final String URL      = "jdbc:mysql://localhost:3306/trackpackdb";
    private static final String USERNAME = "root";
    private static final String PWD      = "";

    private Connection conx;
    private static MyDataBase instance;

    private MyDataBase() {
        try {
            conx = DriverManager.getConnection(URL, USERNAME, PWD);
            System.out.println("✅ Connexion à la base de données réussie !");
        } catch (SQLException e) {
            System.err.println("❌ Erreur de connexion : " + e.getMessage());
        }
    }

    public static MyDataBase getInstance() {
        if (instance == null) {
            instance = new MyDataBase();
        }
        return instance;
    }

    public Connection getConx() {
        return conx;
    }
}