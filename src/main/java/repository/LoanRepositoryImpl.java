package repository;

import config.DatabaseConfig;
import model.Book;
import model.Loan;
import model.Reservation;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Optional;

public class LoanRepositoryImpl implements LoanRepository {

    private final ReservationRepository reservationRepository = new ReservationRepositoryImpl();
    private final BookRepository bookRepository = new BookRepositoryImpl();

    @Override
    public boolean loanBook(int userId, int bookId) {
        Optional<Book> bookOpt = bookRepository.findById(bookId);
        if (bookOpt.isEmpty()) {
            System.out.println("Książka o podanym ID nie istnieje.");
            return false;
        }

        Book book = bookOpt.get();
        if (book.getStatus().equals("LOANED")) {
            System.out.println("Książka jest już wypożyczona.");
            return false;
        }

        if (book.getStatus().equals("RESERVED") && book.getReservedForUserId() != userId) {
            System.out.println("Książka jest zarezerwowana dla innego użytkownika.");
            return false;
        }

        String insertLoan = "INSERT INTO loans (user_id, book_id, loan_date) VALUES (?, ?, ?)";
        String updateBook = "UPDATE books SET status = 'LOANED', reserved_for_user_id = NULL WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement loanStmt = conn.prepareStatement(insertLoan);
                 PreparedStatement bookStmt = conn.prepareStatement(updateBook)) {

                loanStmt.setInt(1, userId);
                loanStmt.setInt(2, bookId);
                loanStmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
                loanStmt.executeUpdate();

                bookStmt.setInt(1, bookId);
                bookStmt.executeUpdate();

                if (book.getStatus().equals("RESERVED")) {
                    List<Reservation> reservations = reservationRepository.findReservationsByBookId(bookId);
                    reservations.stream()
                        .filter(r -> r.getUserId() == userId)
                        .findFirst()
                        .ifPresent(r -> reservationRepository.deleteReservation(r.getId()));
                }

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
    public boolean returnBook(int bookId, int userId) {
        String updateLoan = "UPDATE loans SET return_date = ? WHERE book_id = ? AND user_id = ? AND return_date IS NULL";

        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement loanStmt = conn.prepareStatement(updateLoan)) {
                loanStmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
                loanStmt.setInt(2, bookId);
                loanStmt.setInt(3, userId);

                if (loanStmt.executeUpdate() == 0) {
                    conn.rollback();
                    return false;
                }

                Optional<Reservation> nextReservation = reservationRepository.findNextReservationForBook(bookId);
                if (nextReservation.isPresent()) {
                    String updateBookSql = "UPDATE books SET status = 'RESERVED', reserved_for_user_id = ? WHERE id = ?";
                    try (PreparedStatement bookStmt = conn.prepareStatement(updateBookSql)) {
                        bookStmt.setInt(1, nextReservation.get().getUserId());
                        bookStmt.setInt(2, bookId);
                        bookStmt.executeUpdate();
                    }
                } else {
                    String updateBookSql = "UPDATE books SET status = 'AVAILABLE', reserved_for_user_id = NULL WHERE id = ?";
                    try (PreparedStatement bookStmt = conn.prepareStatement(updateBookSql)) {
                        bookStmt.setInt(1, bookId);
                        bookStmt.executeUpdate();
                    }
                }

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

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String title = rs.getString("title");
                long count = rs.getLong("loan_count");
                topBooks.add(new AbstractMap.SimpleEntry<>(title, count));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return topBooks;
    }

    private List<Loan> fetchLoans(String sql) {
        List<Loan> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(mapResultSetToLoan(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    private List<Loan> fetchLoans(String sql, int userId) {
        List<Loan> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(mapResultSetToLoan(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
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