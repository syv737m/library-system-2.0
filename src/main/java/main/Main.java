package main;

import config.DatabaseConfig;
import model.Book;
import model.Category;
import model.SearchCriteria;
import model.User;
import repository.*;
import service.AuthService;
import service.LibraryService;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;

public class Main {
    private static final UserRepository userRepo = new UserRepositoryImpl();
    private static final BookRepository bookRepo = new BookRepositoryImpl();
    private static final CategoryRepository catRepo = new CategoryRepositoryImpl();
    private static final LoanRepository loanRepo = new LoanRepositoryImpl();
    private static final ReservationRepository reservationRepo = new ReservationRepositoryImpl();
    private static final AuthService authService = new AuthService(userRepo);
    private static final LibraryService libraryService = new LibraryService(loanRepo, bookRepo);

    private static final Scanner scanner = new Scanner(System.in);
    private static final int PAGE_SIZE = 5;

    public static void main(String[] args) {
        DatabaseConfig.initializeDatabase();
        ensureDefaultUsersExist();
        seedInitialData();

        System.out.println("==========================================");
        System.out.println("   TERMINALOWY MENEDŻER BIBLIOTEKI JDBC   ");
        System.out.println("==========================================");

        while (authService.getCurrentUser() == null) {
            handleLogin();
        }

        runMainMenu();
    }

    private static void handleLogin() {
        System.out.println("\n>>> LOGOWANIE");
        System.out.print("Login: ");
        String login = scanner.nextLine();
        System.out.print("Hasło: ");
        String pass = scanner.nextLine();

        if (authService.login(login, pass)) {
            User user = authService.getCurrentUser();
            System.out.println("\nWitaj " + user.getFirstName() + "! Zalogowano jako: " + user.getRole());
            checkNotifications(user.getId());
        } else {
            System.out.println("BŁĄD: Niepoprawny login lub hasło.");
        }
    }

    private static void checkNotifications(int userId) {
        List<Book> reservedBooks = reservationRepo.findBooksReservedByUser(userId);
        if (!reservedBooks.isEmpty()) {
            System.out.println("\n--- POWIADOMIENIA ---");
            System.out.println("Następujące książki czekają na Ciebie do odbioru:");
            reservedBooks.forEach(book -> System.out.println("- " + book.getTitle()));
            System.out.println("---------------------\n");
        }
    }

    private static void runMainMenu() {
        boolean running = true;
        while (running) {
            User user = authService.getCurrentUser();
            boolean isAdmin = authService.isAdmin();

            printMenu(user.getRole());
            int choice = readInt();

            switch (choice) {
                case 1 -> bookRepo.getAllBooks().forEach(System.out::println);
                case 2 -> handleSearch();
                case 3 -> handleCategoryFilter();
                case 4 -> handleReserveBook();
                case 5 -> handleMyReservations();
                case 6 -> handleLoan();
                case 7 -> handleReturn();
                case 8 -> { if(isAdmin) handleAddBook(); else accessDenied(); }
                case 9 -> { if(isAdmin) handleDeleteBook(); else accessDenied(); }
                case 10 -> { if(isAdmin) handleManageCategories(); else accessDenied(); }
                case 11 -> { if(isAdmin) handleStats(); else accessDenied(); }
                case 0 -> {
                    authService.logout();
                    running = false;
                    System.out.println("Wylogowano pomyślnie.");
                }
                default -> System.out.println("Nieprawidłowa opcja.");
            }
        }
    }

    private static void printMenu(String role) {
        System.out.println("\n--- MENU GŁÓWNE (" + role + ") ---");
        System.out.println("1. Lista wszystkich książek");
        System.out.println("2. Szukaj książki");
        System.out.println("3. Filtruj po kategorii");
        System.out.println("4. Zarezerwuj książkę");
        System.out.println("5. Moje rezerwacje");
        System.out.println("6. Wypożycz książkę");
        System.out.println("7. Moje wypożyczenia / Zwrot");

        if (authService.isAdmin()) {
            System.out.println("[ADMIN] 8. Dodaj książkę");
            System.out.println("[ADMIN] 9. Usuń książkę");
            System.out.println("[ADMIN] 10. Zarządzaj kategoriami");
            System.out.println("[ADMIN] 11. Statystyki");
        }
        System.out.println("0. Wyloguj i wyjdź");
        System.out.print("Wybór: ");
    }

    private static void handleSearch() {
        System.out.println("\n--- WYSZUKIWANIE ---");
        System.out.println("1. Proste (po tytule lub autorze)");
        System.out.println("2. Zaawansowane (wiele kryteriów, sortowanie, paginacja)");
        System.out.print("Wybór: ");
        int choice = readInt();

        if (choice == 1) {
            System.out.print("Wpisz frazę: ");
            String query = scanner.nextLine();
            List<Book> result = bookRepo.searchBooks(query);
            if (result.isEmpty()) System.out.println("Nie znaleziono książek pasujących do zapytania.");
            else result.forEach(System.out::println);
        } else if (choice == 2) {
            handleAdvancedSearch();
        }
    }

    private static void handleAdvancedSearch() {
        System.out.println("\n--- WYSZUKIWANIE ZAAWANSOWANE ---");
        System.out.println("Wprowadź kryteria (zostaw puste, by pominąć).");
        System.out.print("Tytuł: ");
        String title = scanner.nextLine();
        System.out.print("Autor: ");
        String author = scanner.nextLine();
        System.out.print("Rok wydania: ");
        String yearStr = scanner.nextLine();
        System.out.println("Kategoria:");
        catRepo.getAllCategories().forEach(System.out::println);
        System.out.print("Wybierz ID kategorii: ");
        String categoryIdStr = scanner.nextLine();

        System.out.println("\nSortuj według:");
        System.out.println("1. Tytułu | 2. Autora | 3. Roku wydania (domyślnie)");
        String sortBy = switch (readInt()) {
            case 1 -> "title";
            case 2 -> "author";
            default -> "publication_year";
        };
        System.out.println("1. Rosnąco (ASC) | 2. Malejąco (DESC) (domyślnie)");
        String sortOrder = (readInt() == 1) ? "ASC" : "DESC";

        SearchCriteria.SearchCriteriaBuilder builder = SearchCriteria.builder()
                .sortBy(sortBy).sortOrder(sortOrder).pageSize(PAGE_SIZE);
        if (!title.isEmpty()) builder.title(title);
        if (!author.isEmpty()) builder.author(author);
        if (!yearStr.isEmpty()) builder.year(Integer.parseInt(yearStr));
        if (!categoryIdStr.isEmpty()) builder.categoryId(Integer.parseInt(categoryIdStr));

        SearchCriteria initialCriteria = builder.build();
        int totalResults = bookRepo.countBooks(initialCriteria);
        if (totalResults == 0) {
            System.out.println("Nie znaleziono książek pasujących do podanych kryteriów.");
            return;
        }

        int totalPages = (int) Math.ceil((double) totalResults / PAGE_SIZE);
        int currentPage = 1;
        boolean searching = true;

        while(searching) {
            builder.page(currentPage);
            SearchCriteria currentCriteria = builder.build();
            List<Book> books = bookRepo.searchBooks(currentCriteria);

            System.out.println("\n--- WYNIKI WYSZUKIWANIA (Strona " + currentPage + "/" + totalPages + ") ---");
            books.forEach(System.out::println);
            System.out.println("----------------------------------------------------");
            System.out.println("Znaleziono: " + totalResults + " książek.");

            System.out.println("\nOpcje: [N]astępna strona | [P]oprzednia strona | [Z]akończ");
            String nav = scanner.nextLine().toUpperCase();
            switch (nav) {
                case "N" -> { if (currentPage < totalPages) currentPage++; }
                case "P" -> { if (currentPage > 1) currentPage--; }
                case "Z" -> searching = false;
            }
        }
    }

    private static void handleCategoryFilter() {
        List<Category> cats = catRepo.getAllCategories();
        if (cats.isEmpty()) { System.out.println("Brak kategorii."); return; }

        cats.forEach(c -> System.out.println(c.getId() + ". " + c.getName()));
        System.out.print("Podaj ID kategorii: ");
        int id = readInt();
        bookRepo.getBooksByCategory(id).forEach(System.out::println);
    }

    private static void handleReserveBook() {
        System.out.print("Podaj ID książki, którą chcesz zarezerwować: ");
        int bookId = readInt();
        Optional<Book> bookOpt = bookRepo.findById(bookId);
        if (bookOpt.isEmpty()) {
            System.out.println("Książka o podanym ID nie istnieje.");
            return;
        }
        if (!bookOpt.get().getStatus().equals("LOANED")) {
            System.out.println("Można rezerwować tylko wypożyczone książki.");
            return;
        }
        reservationRepo.addReservation(authService.getCurrentUser().getId(), bookId);
        System.out.println("Książka została zarezerwowana. Zostaniesz powiadomiony, gdy będzie dostępna.");
    }

    private static void handleMyReservations() {
        System.out.println("\n--- KSIĄŻKI OCZEKUJĄCE NA ODBIÓR ---");
        List<Book> reservedBooks = reservationRepo.findBooksReservedByUser(authService.getCurrentUser().getId());
        if (reservedBooks.isEmpty()) {
            System.out.println("Brak książek oczekujących na odbiór.");
        } else {
            reservedBooks.forEach(System.out::println);
        }
    }

    private static void handleLoan() {
        System.out.print("Podaj ID książki do wypożyczenia: ");
        int bookId = readInt();
        if (libraryService.rentBook(authService.getCurrentUser().getId(), bookId)) {
            System.out.println("Sukces: Wypożyczono książkę.");
        }
    }

    private static void handleReturn() {
        int userId = authService.getCurrentUser().getId();
        System.out.println("Twoje aktywne wypożyczenia:");
        loanRepo.getActiveLoansByUser(userId).forEach(System.out::println);

        System.out.print("\nPodaj ID książki do zwrotu: ");
        int bookId = readInt();
        if (loanRepo.returnBook(bookId, userId)) {
            System.out.println("Sukces: Zwrócono książkę.");
        } else {
            System.out.println("Błąd: Nie masz wypożyczonej tej książki.");
        }
    }

    private static void handleAddBook() {
        System.out.print("Tytuł: "); String t = scanner.nextLine();
        System.out.print("Autor: "); String a = scanner.nextLine();
        System.out.print("Rok: "); int y = readInt();
        System.out.println("Wybierz ID kategorii:");
        catRepo.getAllCategories().forEach(System.out::println);
        int cId = readInt();

        Book b = Book.builder()
                .title(t).author(a).publicationYear(y)
                .categoryId(cId)
                .status("AVAILABLE").build();
        bookRepo.addBook(b);
    }

    private static void handleDeleteBook() {
        System.out.print("Podaj ID książki do usunięcia: ");
        int id = readInt();
        if (bookRepo.deleteBook(id)) System.out.println("Książka usunięta.");
    }

    private static void handleManageCategories() {
        System.out.println("\n--- ZARZĄDZANIE KATEGORIAMI ---");
        catRepo.getAllCategories().forEach(System.out::println);
        System.out.println("\n1. Dodaj kategorię | 2. Edytuj kategorię | 3. Usuń kategorię");
        System.out.print("Wybór: ");
        int sub = readInt();
        if (sub == 1) {
            System.out.print("Nazwa nowej kategorii: ");
            catRepo.addCategory(scanner.nextLine());
            System.out.println("Dodano kategorię.");
        } else if (sub == 2) {
            System.out.print("Podaj ID kategorii do edycji: ");
            int catId = readInt();
            System.out.print("Podaj nową nazwę: ");
            String newName = scanner.nextLine();
            catRepo.updateCategory(new Category(catId, newName));
            System.out.println("Zaktualizowano kategorię.");
        } else if (sub == 3) {
            System.out.print("Podaj ID kategorii do usunięcia: ");
            catRepo.deleteCategory(readInt());
            System.out.println("Usunięto kategorię.");
        }
    }

    private static void handleStats() {
        System.out.println("\n--- STATYSTYKI ---");
        System.out.println("Liczba wszystkich książek: " + bookRepo.countAllBooks());
        System.out.println("Liczba wypożyczonych książek: " + bookRepo.countByStatus("LOANED"));
        System.out.println("Liczba aktywnych użytkowników: " + userRepo.countActiveUsers());
        System.out.println("\nNajpopularniejsze książki (TOP 5):");
        List<Map.Entry<String, Long>> topBooks = loanRepo.getMostLoanedBooks(5);
        if (topBooks.isEmpty()) {
            System.out.println("Brak danych o wypożyczeniach.");
        } else {
            topBooks.forEach(entry -> System.out.println("- \"" + entry.getKey() + "\" (" + entry.getValue() + " wypożyczeń)"));
        }
    }

    private static void ensureDefaultUsersExist() {
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

    private static void seedInitialData() {
        if (bookRepo.countAllBooks() == 0) {
            System.out.println("[System] Wypełnianie bazy danych przykładowymi danymi...");

            // Dodaj kategorie
            catRepo.addCategory("Fantasy");
            catRepo.addCategory("Science Fiction");
            catRepo.addCategory("Kryminał");
            catRepo.addCategory("Historia");

            // Pobierz ID kategorii po nazwie
            int fantasyId = catRepo.findByName("Fantasy").get().getId();
            int scifiId = catRepo.findByName("Science Fiction").get().getId();
            int crimeId = catRepo.findByName("Kryminał").get().getId();
            int historyId = catRepo.findByName("Historia").get().getId();

            // Dodaj książki
            bookRepo.addBook(Book.builder().title("Władca Pierścieni").author("J.R.R. Tolkien").publicationYear(1954).categoryId(fantasyId).status("AVAILABLE").build());
            bookRepo.addBook(Book.builder().title("Diuna").author("Frank Herbert").publicationYear(1965).categoryId(scifiId).status("AVAILABLE").build());
            bookRepo.addBook(Book.builder().title("Morderstwo w Orient Expressie").author("Agatha Christie").publicationYear(1934).categoryId(crimeId).status("AVAILABLE").build());
            bookRepo.addBook(Book.builder().title("Sapiens: Od zwierząt do bogów").author("Yuval Noah Harari").publicationYear(2011).categoryId(historyId).status("AVAILABLE").build());
            bookRepo.addBook(Book.builder().title("Hobbit, czyli tam i z powrotem").author("J.R.R. Tolkien").publicationYear(1937).categoryId(fantasyId).status("AVAILABLE").build());
            bookRepo.addBook(Book.builder().title("Folwark zwierzęcy").author("George Orwell").publicationYear(1945).categoryId(scifiId).status("AVAILABLE").build());

            // Pobierz użytkowników
            Optional<User> user1 = userRepo.findByUsername("user");
            Optional<User> user2 = userRepo.findByUsername("user2");

            // Stwórz historię wypożyczeń
            if (user1.isPresent() && user2.isPresent()) {
                System.out.println("[System] Tworzenie historii wypożyczeń...");
                // Władca Pierścieni (3x), Diuna (2x), Hobbit (1x)
                loanRepo.loanBook(user1.get().getId(), 1); loanRepo.returnBook(1, user1.get().getId());
                loanRepo.loanBook(user2.get().getId(), 1); loanRepo.returnBook(1, user2.get().getId());
                loanRepo.loanBook(user1.get().getId(), 1); loanRepo.returnBook(1, user1.get().getId());

                loanRepo.loanBook(user2.get().getId(), 2); loanRepo.returnBook(2, user2.get().getId());
                loanRepo.loanBook(user1.get().getId(), 2); loanRepo.returnBook(2, user1.get().getId());

                loanRepo.loanBook(user1.get().getId(), 5); loanRepo.returnBook(5, user1.get().getId());
            }

            // Wypożycz książkę dla user2
            if (user2.isPresent()) {
                loanRepo.loanBook(user2.get().getId(), 6); // Wypożycz "Folwark zwierzęcy"
                System.out.println("[System] Książka 'Folwark zwierzęcy' została wypożyczona przez user2.");
            }
        }
    }

    private static int readInt() {
        try {
            return Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static void accessDenied() {
        System.out.println("BŁĄD: Brak uprawnień administratora!");
    }
}