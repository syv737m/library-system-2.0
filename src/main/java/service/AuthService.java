package service;

import model.User;
import org.mindrot.jbcrypt.BCrypt;
import repository.UserRepository;
import java.util.Optional;

public class AuthService {
    private final UserRepository userRepository;
    private User currentUser;

    // Dependency Injection przez konstruktor
    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean login(String username, String password) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // Sprawdzanie zahashowanego hasła
            if (BCrypt.checkpw(password, user.getPassword())) {
                this.currentUser = user;
                return true;
            }
        }
        return false;
    }

    public void logout() {
        this.currentUser = null;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isAdmin() {
        return currentUser != null && "ADMIN".equals(currentUser.getRole());
    }
}