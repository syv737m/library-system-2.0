package repository;

import model.User;
import java.util.List;
import java.util.Optional;

public interface UserRepository {
    void addUser(User user);
    List<User> getAllUsers();
    boolean deleteUser(int userId);
    Optional<User> findByUsername(String username);
}