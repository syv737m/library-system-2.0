package repository;

import model.User;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class UserRepositoryImpl implements UserRepository {

    private final DataSource dataSource;

    public UserRepositoryImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void addUser(User user) {
        String sql = "INSERT INTO users (username, password, first_name, last_name, email, role) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user.getUsername());
            String hashedPw = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt());
            stmt.setString(2, hashedPw);
            stmt.setString(3, user.getFirstName());
            stmt.setString(4, user.getLastName());
            stmt.setString(5, user.getEmail());
            stmt.setString(6, user.getRole() != null ? user.getRole() : "USER");
            stmt.executeUpdate();
            System.out.println("Sukces: Użytkownik " + user.getUsername() + " został zarejestrowany.");
        } catch (SQLException e) {
            throw new RuntimeException("Błąd podczas dodawania użytkownika", e);
        }
    }

    @Override
    public Optional<User> findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                User user = new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("email"),
                        rs.getString("role")
                );
                return Optional.of(user);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Błąd podczas wyszukiwania użytkownika po nazwie", e);
        }
        return Optional.empty();
    }

    @Override
    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users";
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                users.add(new User(
                        rs.getInt("id"), rs.getString("username"), rs.getString("password"),
                        rs.getString("first_name"), rs.getString("last_name"),
                        rs.getString("email"), rs.getString("role")
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Błąd podczas pobierania wszystkich użytkowników", e);
        }
        return users;
    }

    @Override
    public boolean deleteUser(int userId) {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Błąd podczas usuwania użytkownika: Użytkownik może mieć powiązane wypożyczenia lub rezerwacje.");
            return false;
        }
    }

    @Override
    public int countActiveUsers() {
        String sql = "SELECT COUNT(DISTINCT user_id) FROM loans WHERE return_date IS NULL";
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Błąd podczas liczenia aktywnych użytkowników", e);
        }
        return 0;
    }
}