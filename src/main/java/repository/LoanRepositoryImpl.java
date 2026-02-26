package repository;

import model.Loan;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.AbstractMap;
import java.util.Map;

@Repository
public class LoanRepositoryImpl implements LoanRepository {

    private final DataSource dataSource;

    public LoanRepositoryImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void createLoan(int userId, int bookId) {
        String sql = "INSERT INTO loans (user_id, book_id, loan_date) VALUES (?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, bookId);
            stmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Błąd podczas tworzenia wypożyczenia", e);
        }
    }

    @Override
    public boolean returnLoan(int bookId, int userId) {
        String sql = "UPDATE loans SET return_date = ? WHERE book_id = ? AND user_id = ? AND return_date IS NULL";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setInt(2, bookId);
            stmt.setInt(3, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Błąd podczas aktualizacji zwrotu wypożyczenia", e);
        }
    }

    @Override
    public List<Loan> getAllLoans() {
        return fetchLoans("SELECT * FROM loans");
    }

    @Override
    public List<Loan> getActiveLoansByUser(int userId) {
        String sql = "SELECT * FROM loans WHERE user_id = ? AND return_date IS NULL";
        return fetchLoans(sql, userId);
    }

    @Override
    public List<Map.Entry<String, Long>> getMostLoanedBooks(int limit) {
        List<Map.Entry<String, Long>> topBooks = new ArrayList<>();
        String sql = "SELECT b.title, COUNT(l.book_id) AS loan_count " +
                     "FROM loans l " +
                     "JOIN books b ON l.book_id = b.id " +
                     "GROUP BY l.book_id, b.title " +
                     "ORDER BY loan_count DESC " +
                     "LIMIT ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String title = rs.getString("title");
                long count = rs.getLong("loan_count");
                topBooks.add(new AbstractMap.SimpleEntry<>(title, count));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Błąd podczas pobierania najpopularniejszych książek", e);
        }
        return topBooks;
    }

    private List<Loan> fetchLoans(String sql) {
        List<Loan> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSetToLoan(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Błąd podczas pobierania wypożyczeń", e);
        }
        return list;
    }

    private List<Loan> fetchLoans(String sql, int userId) {
        List<Loan> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(mapResultSetToLoan(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Błąd podczas pobierania wypożyczeń użytkownika", e);
        }
        return list;
    }

    private Loan mapResultSetToLoan(ResultSet rs) throws SQLException {
        return Loan.builder()
                .id(rs.getInt("id"))
                .userId(rs.getInt("user_id"))
                .bookId(rs.getInt("book_id"))
                .loanDate(rs.getTimestamp("loan_date").toLocalDateTime())
                .returnDate(rs.getTimestamp("return_date") != null ?
                        rs.getTimestamp("return_date").toLocalDateTime() : null)
                .build();
    }
}