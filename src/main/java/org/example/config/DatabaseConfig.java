package org.example.config;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class DatabaseConfig {

    private static final Map<String, String> env = new HashMap<>();

    static {
        try {
            if (Files.exists(Paths.get(".env"))) {
                try (BufferedReader reader = new BufferedReader(new FileReader(".env"))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) {
                            continue;
                        }
                        int eqIdx = line.indexOf('=');
                        if (eqIdx > 0) {
                            String key = line.substring(0, eqIdx).trim();
                            String value = line.substring(eqIdx + 1).trim();
                            if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
                                value = value.substring(1, value.length() - 1);
                            } else if (value.startsWith("'") && value.endsWith("'") && value.length() >= 2) {
                                value = value.substring(1, value.length() - 1);
                            }
                            env.put(key, value);
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Erro ao carregar o arquivo .env: " + e.getMessage());
        }
    }

    private static String getEnv(String key) {
        if (env.containsKey(key)) {
            return env.get(key);
        }
        return System.getenv(key);
    }

    private static final String URL =
            "jdbc:postgresql://aws-0-us-east-2.pooler.supabase.com:5432/postgres";

    private static final String USER =
            "postgres.ktxrljtprwtsjtjhiduu";

    private static final String PASSWORD = getEnv("DATABASE_PASSWORD");

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}