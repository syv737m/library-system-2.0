package repository;

import model.Book;
import model.SearchCriteria;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class BookRepositoryImpl implements BookRepository {

    private final DataSource dataSource;

    public BookRepositoryImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void addBook(Book book) {
        String sql = "INSERT INTO books (title, author, publication_year, isbn, category_id, status) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, book.getTitle());
            stmt.setString(2, book.getAuthor());
            stmt.setInt(3, book.getPublicationYear());
            stmt.setString(4, book.getIsbn());
            stmt.setInt(5, book.getCategoryId());
            stmt.setString(6, book.getStatus() != null ? book.getStatus() : "AVAILABLE");
            stmt.executeUpdate();
            System.out.println("Dodano książkę: " + book.getTitle());
        } catch (SQLException e) {
            throw new RuntimeException("Błąd podczas dodawania książki", e);
        }
    }

    @Override
    public List<Book> getAllBooks() {
        List<Book> books = new ArrayList<>();
        String sql = "SELECT * FROM books WHERE is_deleted = FALSE";
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                books.add(mapResultSetToBook(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Błąd podczas pobierania wszystkich książek", e);
        }
        return books;
    }

    @Override
    public Optional<Book> findById(int id) {
        String sql = "SELECT * FROM books WHERE id = ? AND is_deleted = FALSE";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapResultSetToBook(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Błąd podczas wyszukiwania książki po ID", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Book> searchBooks(String query) {
        List<Book> books = new ArrayList<>();
        String sql = "SELECT * FROM books WHERE (LOWER(title) LIKE LOWER(?) OR LOWER(author) LIKE LOWER(?)) AND is_deleted = FALSE";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            String pattern = "%" + query + "%";
            stmt.setString(1, pattern);
            stmt.setString(2, pattern);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                books.add(mapResultSetToBook(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Błąd podczas prostego wyszukiwania książek", e);
        }
        return books;
    }

    @Override
    public List<Book> searchBooks(SearchCriteria criteria) {
        List<Book> books = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM books");
        sql.append(buildWhereClause(criteria, params));

        if (criteria.getSortBy() != null && !criteria.getSortBy().isEmpty()) {
            sql.append(" ORDER BY ").append(criteria.getSortBy());
            if (criteria.getSortOrder() != null && !criteria.getSortOrder().isEmpty()) {
                sql.append(" ").append(criteria.getSortOrder());
            }
        }

        if (criteria.getPageSize() > 0) {
            sql.append(" LIMIT ? OFFSET ?");
            params.add(criteria.getPageSize());
            params.add((criteria.getPage() - 1) * criteria.getPageSize());
        }

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            setStatementParams(stmt, params);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                books.add(mapResultSetToBook(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Błąd podczas zaawansowanego wyszukiwania książek", e);
        }
        return books;
    }

    @Override
    public int countBooks(SearchCriteria criteria) {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM books");
        sql.append(buildWhereClause(criteria, params));

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            setStatementParams(stmt, params);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Błąd podczas liczenia książek", e);
        }
        return 0;
    }

    private String buildWhereClause(SearchCriteria criteria, List<Object> params) {
        StringBuilder where = new StringBuilder(" WHERE is_deleted = FALSE");
        if (criteria.getTitle() != null && !criteria.getTitle().isEmpty()) {
            where.append(" AND LOWER(title) LIKE LOWER(?)");
            params.add("%" + criteria.getTitle() + "%");
        }
        if (criteria.getAuthor() != null && !criteria.getAuthor().isEmpty()) {
            where.append(" AND LOWER(author) LIKE LOWER(?)");
            params.add("%" + criteria.getAuthor() + "%");
        }
        if (criteria.getYear() != null) {
            where.append(" AND publication_year = ?");
            params.add(criteria.getYear());
        }
        if (criteria.getCategoryId() != null) {
            where.append(" AND category_id = ?");
            params.add(criteria.getCategoryId());
        }
        return where.toString();
    }

    private void setStatementParams(PreparedStatement stmt, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            stmt.setObject(i + 1, params.get(i));
        }
    }

    @Override
    public List<Book> getBooksByCategory(int categoryId) {
        List<Book> books = new ArrayList<>();
        String sql = "SELECT * FROM books WHERE category_id = ? AND is_deleted = FALSE";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, categoryId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                books.add(mapResultSetToBook(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Błąd podczas pobierania książek po kategorii", e);
        }
        return books;
    }

    @Override
    public boolean deleteBook(int bookId) {
        String sql = "UPDATE books SET is_deleted = TRUE WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, bookId);
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Błąd podczas usuwania książki", e);
        }
    }

    @Override
    public int countAllBooks() {
        String sql = "SELECT COUNT(*) FROM books WHERE is_deleted = FALSE";
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Błąd podczas liczenia wszystkich książek", e);
        }
        return 0;
    }

    @Override
    public int countByStatus(String status) {
        String sql = "SELECT COUNT(*) FROM books WHERE status = ? AND is_deleted = FALSE";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Błąd podczas liczenia książek po statusie", e);
        }
        return 0;
    }

    private Book mapResultSetToBook(ResultSet rs) throws SQLException {
        return Book.builder()
                .id(rs.getInt("id"))
                .title(rs.getString("title"))
                .author(rs.getString("author"))
                .publicationYear(rs.getInt("publication_year"))
                .isbn(rs.getString("isbn"))
                .categoryId(rs.getInt("category_id"))
                .status(rs.getString("status"))
                .reservedForUserId((Integer) rs.getObject("reserved_for_user_id"))
                .isDeleted(rs.getBoolean("is_deleted"))
                .build();
    }
}