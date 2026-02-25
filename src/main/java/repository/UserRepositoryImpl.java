package repository;

import config.DatabaseConfig;
import model.User;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserRepositoryImpl implements UserRepository {

    @Override
    public void addUser(User user) {
        String sql = "INSERT INTO users (username, password, first_name, last_name, email, role) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user.getUsername());
            // HASZOWANIE HASŁA przed zapisem do bazy
            String hashedPw = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt());
            stmt.setString(2, hashedPw);

            stmt.setString(3, user.getFirstName());
            stmt.setString(4, user.getLastName());
            stmt.setString(5, user.getEmail());
            stmt.setString(6, user.getRole() != null ? user.getRole() : "USER");

            stmt.executeUpdate();
            System.out.println("Sukces: Użytkownik " + user.getUsername() + " został zarejestrowany.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Optional<User> findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                User user = new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password"), // To jest hash
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("email"),
                        rs.getString("role")
                );
                return Optional.of(user);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                users.add(new User(
                        rs.getInt("id"), rs.getString("username"), rs.getString("password"),
                        rs.getString("first_name"), rs.getString("last_name"),
                        rs.getString("email"), rs.getString("role")
                ));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return users;
    }

    @Override
    public boolean deleteUser(int userId) {
        // ... (tutaj kod usuwania taki jak miałeś wcześniej, ale wewnątrz impl)
        return false; // uproszczenie na potrzeby przykładu
    }
}