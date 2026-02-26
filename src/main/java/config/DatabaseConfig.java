package config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

public class DatabaseConfig {

    // URL do bazy plikowej H2, która zostanie utworzona w głównym katalogu projektu.
    // Opcja AUTO_SERVER=TRUE pozwala na dostęp do bazy z zewnętrznych narzędzi, gdy aplikacja jest uruchomiona.
    private static final String URL = "jdbc:h2:./library_db;AUTO_SERVER=TRUE";
    private static final String USER = "sa";
    private static final String PASSWORD = "";

    // Zwraca nowe połączenie do bazy danych.
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    // Inicjalizuje bazę danych, tworząc tabele na podstawie pliku schema.sql.
    public static void initializeDatabase() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // Ładowanie pliku `schema.sql` jako zasobu z classpath.
            InputStream inputStream = DatabaseConfig.class.getClassLoader().getResourceAsStream("schema.sql");
            if (inputStream == null) {
                throw new IOException("Nie można znaleźć pliku schema.sql w classpath.");
            }

            String sql;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                sql = reader.lines().collect(Collectors.joining("\n"));
            }

            stmt.execute(sql);
            System.out.println("Baza danych została pomyślnie zainicjalizowana.");

        } catch (SQLException | IOException e) {
            e.printStackTrace();
            System.err.println("Błąd podczas inicjalizacji bazy danych: " + e.getMessage());
        }
    }
}