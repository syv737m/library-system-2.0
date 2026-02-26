package repository;

import model.Category;
import java.util.List;
import java.util.Optional;

public interface CategoryRepository {
    void addCategory(String name);
    List<Category> getAllCategories();
    void deleteCategory(int id);
    void updateCategory(Category category);
    Optional<Category> findByName(String name);
}