package repository;

import config.DatabaseConfig;
import model.Loan;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class LoanRepositoryImpl implements LoanRepository {

    @Override
    public boolean loanBook(int userId, int bookId) {
        // Najpierw sprawdzamy dostępność (bezpiecznik)
        if (!isBookAvailable(bookId)) {
            System.out.println("Książka jest niedostępna.");
            return false;
        }

        String insertLoan = "INSERT INTO loans (user_id, book_id, loan_date) VALUES (?, ?, ?)";
        String updateBook = "UPDATE books SET status = 'LOANED' WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false); // START TRANSAKCJI

            try (PreparedStatement loanStmt = conn.prepareStatement(insertLoan);
                 PreparedStatement bookStmt = conn.prepareStatement(updateBook)) {

                // 1. Dodanie wypożyczenia
                loanStmt.setInt(1, userId);
                loanStmt.setInt(2, bookId);
                loanStmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
                loanStmt.executeUpdate();

                // 2. Zmiana statusu książki
                bookStmt.setInt(1, bookId);
                bookStmt.executeUpdate();

                conn.commit(); // ZATWIERDZENIE OBU OPERACJI
                return true;
            } catch (SQLException e) {
                conn.rollback(); // COFNIĘCIE W RAZIE BŁĘDU
                throw e;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean returnBook(int bookId, int userId) {
        String updateLoan = "UPDATE loans SET return_date = ? WHERE book_id = ? AND user_id = ? AND return_date IS NULL";
        String updateBook = "UPDATE books SET status = 'AVAILABLE' WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement loanStmt = conn.prepareStatement(updateLoan);
                 PreparedStatement bookStmt = conn.prepareStatement(updateBook)) {

                loanStmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
                loanStmt.setInt(2, bookId);
                loanStmt.setInt(3, userId);

                if (loanStmt.executeUpdate() == 0) {
                    conn.rollback();
                    return false;
                }

                bookStmt.setInt(1, bookId);
                bookStmt.executeUpdate();

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public List<Loan> getAllLoans() {
        return fetchLoans("SELECT * FROM loans");
    }

    @Override
    public List<Loan> getActiveLoansByUser(int userId) {
        return fetchLoans("SELECT * FROM loans WHERE user_id = " + userId + " AND return_date IS NULL");
    }

    // Prywatna metoda pomocnicza do mapowania rekordów
    private List<Loan> fetchLoans(String sql) {
        List<Loan> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(Loan.builder()
                        .id(rs.getInt("id"))
                        .userId(rs.getInt("user_id"))
                        .bookId(rs.getInt("book_id"))
                        .loanDate(rs.getTimestamp("loan_date").toLocalDateTime())
                        .returnDate(rs.getTimestamp("return_date") != null ?
                                rs.getTimestamp("return_date").toLocalDateTime() : null)
                        .build());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    private boolean isBookAvailable(int bookId) {
        String sql = "SELECT status FROM books WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, bookId);
            ResultSet rs = stmt.executeQuery();
            return rs.next() && "AVAILABLE".equals(rs.getString("status"));
        } catch (SQLException e) {
            return false;
        }
    }
}