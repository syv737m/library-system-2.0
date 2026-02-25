package repository;

import config.DatabaseConfig;
import model.Category;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategoryRepositoryImpl implements CategoryRepository {
    @Override
    public void addCategory(String name) {
        String sql = "INSERT INTO categories (name) VALUES (?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public List<Category> getAllCategories() {
        List<Category> list = new ArrayList<>();
        String sql = "SELECT * FROM categories";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Category(rs.getInt("id"), rs.getString("name")));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    @Override
    public void deleteCategory(int id) {
        String sql = "DELETE FROM categories WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }
}