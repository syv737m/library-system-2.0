package repository;

import model.Book;
import model.Reservation;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class ReservationRepositoryImpl implements ReservationRepository {

    private final DataSource dataSource;

    public ReservationRepositoryImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void addReservation(int userId, int bookId) {
        String sql = "INSERT INTO reservations (user_id, book_id) VALUES (?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, bookId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Błąd podczas dodawania rezerwacji", e);
        }
    }

    @Override
    public Optional<Reservation> findNextReservationForBook(int bookId) {
        String sql = "SELECT * FROM reservations WHERE book_id = ? ORDER BY reservation_date ASC LIMIT 1";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, bookId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(new Reservation(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getInt("book_id"),
                        rs.getTimestamp("reservation_date").toLocalDateTime()
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Błąd podczas wyszukiwania następnej rezerwacji", e);
        }
        return Optional.empty();
    }

    @Override
    public void deleteReservation(int reservationId) {
        String sql = "DELETE FROM reservations WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, reservationId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Błąd podczas usuwania rezerwacji", e);
        }
    }

    @Override
    public List<Reservation> findReservationsByBookId(int bookId) {
        List<Reservation> reservations = new ArrayList<>();
        String sql = "SELECT * FROM reservations WHERE book_id = ? ORDER BY reservation_date ASC";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, bookId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                reservations.add(new Reservation(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getInt("book_id"),
                        rs.getTimestamp("reservation_date").toLocalDateTime()
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Błąd podczas wyszukiwania rezerwacji po ID książki", e);
        }
        return reservations;
    }

    @Override
    public List<Book> findBooksReservedByUser(int userId) {
        List<Book> books = new ArrayList<>();
        String sql = "SELECT * FROM books WHERE status = 'RESERVED' AND reserved_for_user_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                books.add(Book.builder()
                        .id(rs.getInt("id"))
                        .title(rs.getString("title"))
                        .author(rs.getString("author"))
                        .publicationYear(rs.getInt("publication_year"))
                        .isbn(rs.getString("isbn"))
                        .categoryId(rs.getInt("category_id"))
                        .status(rs.getString("status"))
                        .reservedForUserId((Integer) rs.getObject("reserved_for_user_id"))
                        .isDeleted(rs.getBoolean("is_deleted"))
                        .build());
            }
        } catch (SQLException e) {
            throw new RuntimeException("Błąd podczas wyszukiwania książek zarezerwowanych przez użytkownika", e);
        }
        return books;
    }
}