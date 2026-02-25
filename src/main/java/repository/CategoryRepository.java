package repository;

import model.Category;
import java.util.List;

public interface CategoryRepository {
    void addCategory(String name);
    List<Category> getAllCategories();
    void deleteCategory(int id);
}