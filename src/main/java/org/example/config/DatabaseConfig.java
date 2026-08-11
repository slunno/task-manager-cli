package org.example.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConfig {

    private static final String URL =
            "jdbc:postgresql://aws-0-us-east-2.pooler.supabase.com:5432/postgres";

    private static final String USER =
            "postgres.ktxrljtprwtsjtjhiduu";

    private static final String PASSWORD =
            System.getenv("DATABASE_PASSWORD");

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}