package com.example.rendezvousmedicaux;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBconnection {
        private static final String url="jdbc:postgresql://localhost:5432/rendezvousMedicaux";
        private static String username = "postgres";
        private static String password = "sajda";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }
}
