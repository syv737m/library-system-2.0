package config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConfig {

    // URL do bazy plikowej H2.
    // "./library_db" stworzy plik bazy w głównym katalogu projektu.
    // ";AUTO_SERVER=TRUE" pozwala na podłączenie się do bazy z zewnątrz (np. IntelliJ) gdy aplikacja działa.
    private static final String URL = "jdbc:h2:./library_db;AUTO_SERVER=TRUE";
    private static final String USER = "sa";
    private static final String PASSWORD = "";

    // Metoda zwracająca połączenie - kluczowa dla całego JDBC
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    // Metoda inicjalizująca strukturę bazy danych
    public static void initializeDatabase() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // Odczytujemy plik schema.sql
            // Ścieżka zakłada strukturę Maven: src/main/resources/schema.sql
            String sql = new String(Files.readAllBytes(Paths.get("src/main/resources/schema.sql")));

            // Wykonujemy skrypt
            stmt.execute(sql);
            System.out.println("Baza danych została pomyślnie zainicjalizowana.");

        } catch (SQLException | IOException e) {
            e.printStackTrace();
            System.err.println("Błąd podczas inicjalizacji bazy danych: " + e.getMessage());
        }
    }
}