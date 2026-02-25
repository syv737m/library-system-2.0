package repository;

import config.DatabaseConfig;
import model.Book;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BookRepositoryImpl implements BookRepository {

    @Override
    public void addBook(Book book) {
        String sql = "INSERT INTO books (title, author, publication_year, isbn, category_id, status) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, book.getTitle());
            stmt.setString(2, book.getAuthor());
            stmt.setInt(3, book.getPublicationYear());
            stmt.setString(4, book.getIsbn());

            // Obsługa category_id (jeśli pole w modelu Book to String, parsujemy na int)
            stmt.setInt(5, Integer.parseInt(book.getCategory()));

            stmt.setString(6, book.getStatus() != null ? book.getStatus() : "AVAILABLE");

            stmt.executeUpdate();
            System.out.println("Dodano książkę: " + book.getTitle());
        } catch (SQLException | NumberFormatException e) {
            System.err.println("Błąd podczas dodawania książki: " + e.getMessage());
        }
    }

    @Override
    public List<Book> getAllBooks() {
        List<Book> books = new ArrayList<>();
        String sql = "SELECT * FROM books";

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                books.add(mapResultSetToBook(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return books;
    }

    @Override
    public List<Book> searchBooks(String query) {
        List<Book> books = new ArrayList<>();
        String sql = "SELECT * FROM books WHERE LOWER(title) LIKE LOWER(?) OR LOWER(author) LIKE LOWER(?)";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            String pattern = "%" + query + "%";
            stmt.setString(1, pattern);
            stmt.setString(2, pattern);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                books.add(mapResultSetToBook(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return books;
    }

    @Override
    public List<Book> getBooksByCategory(int categoryId) {
        List<Book> books = new ArrayList<>();
        String sql = "SELECT * FROM books WHERE category_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, categoryId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                books.add(mapResultSetToBook(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return books;
    }

    @Override
    public boolean deleteBook(int bookId) {
        // Najpierw sprawdzamy, czy książka nie jest wypożyczona (klucz obcy)
        String sql = "DELETE FROM books WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, bookId);
            int affected = stmt.executeUpdate();
            return affected > 0;
        } catch (SQLException e) {
            System.err.println("Nie można usunąć książki (prawdopodobnie ma historię wypożyczeń).");
            return false;
        }
    }

    @Override
    public int countAllBooks() {
        String sql = "SELECT COUNT(*) FROM books";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // Metoda pomocnicza do mapowania rekordów na obiekty Java
    private Book mapResultSetToBook(ResultSet rs) throws SQLException {
        return Book.builder()
                .id(rs.getInt("id"))
                .title(rs.getString("title"))
                .author(rs.getString("author"))
                .publicationYear(rs.getInt("publication_year"))
                .isbn(rs.getString("isbn"))
                .category(String.valueOf(rs.getInt("category_id"))) // Mapujemy ID kategorii na pole String w modelu
                .status(rs.getString("status"))
                .build();
    }
}