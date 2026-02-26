package config;

import model.Book;
import model.User;
import repository.BookRepository;
import repository.CategoryRepository;
import repository.LoanRepository;
import repository.UserRepository;
import service.LibraryService;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.Optional;

@Component
@DependsOn("dataSourceInitializer")
public class DataInitializer {

    private final UserRepository userRepo;
    private final BookRepository bookRepo;
    private final CategoryRepository catRepo;
    private final LibraryService libraryService;

    public DataInitializer(UserRepository userRepo, BookRepository bookRepo, CategoryRepository catRepo, LibraryService libraryService) {
        this.userRepo = userRepo;
        this.bookRepo = bookRepo;
        this.catRepo = catRepo;
        this.libraryService = libraryService;
    }

    @PostConstruct
    public void initialize() {
        ensureDefaultUsersExist();
        seedInitialData();
    }

    private void ensureDefaultUsersExist() {
        if (userRepo.findByUsername("admin").isEmpty()) {
            User admin = User.builder()
                    .username("admin").password("admin123")
                    .firstName("Admin").lastName("Systemowy")
                    .role("ADMIN").build();
            userRepo.addUser(admin);
            System.out.println("[System] Utworzono konto administratora: admin / admin123");
        }
        if (userRepo.findByUsername("user").isEmpty()) {
            User user = User.builder()
                    .username("user").password("user123")
                    .firstName("Jan").lastName("Kowalski")
                    .role("USER").build();
            userRepo.addUser(user);
            System.out.println("[System] Utworzono konto użytkownika: user / user123");
        }
        if (userRepo.findByUsername("user2").isEmpty()) {
            User user2 = User.builder()
                    .username("user2").password("user123")
                    .firstName("Anna").lastName("Nowak")
                    .role("USER").build();
            userRepo.addUser(user2);
            System.out.println("[System] Utworzono konto użytkownika: user2 / user123");
        }
    }

    private void seedInitialData() {
        if (bookRepo.countAllBooks() == 0) {
            System.out.println("[System] Wypełnianie bazy danych przykładowymi danymi...");

            catRepo.addCategory("Fantasy");
            catRepo.addCategory("Science Fiction");
            catRepo.addCategory("Kryminał");
            catRepo.addCategory("Historia");

            int fantasyId = catRepo.findByName("Fantasy").get().getId();
            int scifiId = catRepo.findByName("Science Fiction").get().getId();
            int crimeId = catRepo.findByName("Kryminał").get().getId();
            int historyId = catRepo.findByName("Historia").get().getId();

            bookRepo.addBook(Book.builder().title("Władca Pierścieni").author("J.R.R. Tolkien").publicationYear(1954).categoryId(fantasyId).status("AVAILABLE").build());
            bookRepo.addBook(Book.builder().title("Diuna").author("Frank Herbert").publicationYear(1965).categoryId(scifiId).status("AVAILABLE").build());
            bookRepo.addBook(Book.builder().title("Morderstwo w Orient Expressie").author("Agatha Christie").publicationYear(1934).categoryId(crimeId).status("AVAILABLE").build());
            bookRepo.addBook(Book.builder().title("Sapiens: Od zwierząt do bogów").author("Yuval Noah Harari").publicationYear(2011).categoryId(historyId).status("AVAILABLE").build());
            bookRepo.addBook(Book.builder().title("Hobbit, czyli tam i z powrotem").author("J.R.R. Tolkien").publicationYear(1937).categoryId(fantasyId).status("AVAILABLE").build());
            bookRepo.addBook(Book.builder().title("Folwark zwierzęcy").author("George Orwell").publicationYear(1945).categoryId(scifiId).status("AVAILABLE").build());

            Optional<User> user1 = userRepo.findByUsername("user");
            Optional<User> user2 = userRepo.findByUsername("user2");

            if (user1.isPresent() && user2.isPresent()) {
                System.out.println("[System] Tworzenie historii wypożyczeń...");
                libraryService.rentBook(user1.get().getId(), 1); libraryService.returnBook(1, user1.get().getId());
                libraryService.rentBook(user2.get().getId(), 1); libraryService.returnBook(1, user2.get().getId());
                libraryService.rentBook(user1.get().getId(), 1); libraryService.returnBook(1, user1.get().getId());
                libraryService.rentBook(user2.get().getId(), 2); libraryService.returnBook(2, user2.get().getId());
                libraryService.rentBook(user1.get().getId(), 2); libraryService.returnBook(2, user1.get().getId());
                libraryService.rentBook(user1.get().getId(), 5); libraryService.returnBook(5, user1.get().getId());
            }

            if (user2.isPresent()) {
                libraryService.rentBook(user2.get().getId(), 6);
                System.out.println("[System] Książka 'Folwark zwierzęcy' została wypożyczona przez user2.");
            }
        }
    }
}