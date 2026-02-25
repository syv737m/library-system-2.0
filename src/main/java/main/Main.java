package main;

import config.DatabaseConfig;
import model.Book;
import model.Category;
import model.User;
import repository.*;
import service.AuthService;

import java.util.List;
import java.util.Scanner;

public class Main {
    // Wstrzykiwanie zależności (DI) zgodnie z wymaganiami zadania
    private static final UserRepository userRepo = new UserRepositoryImpl();
    private static final BookRepository bookRepo = new BookRepositoryImpl();
    private static final CategoryRepository catRepo = new CategoryRepositoryImpl();
    private static final LoanRepository loanRepo = new LoanRepositoryImpl();
    private static final AuthService authService = new AuthService(userRepo);

    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        // Inicjalizacja bazy danych i stworzenie admina na start
        DatabaseConfig.initializeDatabase();
        ensureAdminExists();

        System.out.println("==========================================");
        System.out.println("   TERMINALOWY MENEDŻER BIBLIOTEKI JDBC   ");
        System.out.println("==========================================");

        // Ekran logowania (wymagany system logowania)
        while (authService.getCurrentUser() == null) {
            handleLogin();
        }

        // Wejście do głównego menu po poprawnym zalogowaniu
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
        } else {
            System.out.println("BŁĄD: Niepoprawny login lub hasło.");
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
                case 4 -> handleLoan();
                case 5 -> handleReturn();
                case 6 -> { if(isAdmin) handleAddBook(); else accessDenied(); }
                case 7 -> { if(isAdmin) handleDeleteBook(); else accessDenied(); }
                case 8 -> { if(isAdmin) handleManageCategories(); else accessDenied(); }
                case 9 -> { if(isAdmin) handleStats(); else accessDenied(); }
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
        System.out.println("4. Wypożycz książkę");
        System.out.println("5. Moje wypożyczenia / Zwrot");

        if (authService.isAdmin()) {
            System.out.println("[ADMIN] 6. Dodaj książkę");
            System.out.println("[ADMIN] 7. Usuń książkę");
            System.out.println("[ADMIN] 8. Zarządzaj kategoriami");
            System.out.println("[ADMIN] 9. Statystyki");
        }
        System.out.println("0. Wyloguj i wyjdź");
        System.out.print("Wybór: ");
    }

    // --- FUNKCJONALNOŚCI ---

    private static void handleSearch() {
        System.out.print("Wyszukaj (tytuł lub autor): ");
        String query = scanner.nextLine();
        List<Book> result = bookRepo.searchBooks(query);
        if (result.isEmpty()) System.out.println("Nie znaleziono.");
        else result.forEach(System.out::println);
    }

    private static void handleCategoryFilter() {
        List<Category> cats = catRepo.getAllCategories();
        if (cats.isEmpty()) { System.out.println("Brak kategorii."); return; }

        cats.forEach(c -> System.out.println(c.getId() + ". " + c.getName()));
        System.out.print("Podaj ID kategorii: ");
        int id = readInt();
        bookRepo.getBooksByCategory(id).forEach(System.out::println);
    }

    private static void handleLoan() {
        System.out.print("Podaj ID książki do wypożyczenia: ");
        int bookId = readInt();
        if (loanRepo.loanBook(authService.getCurrentUser().getId(), bookId)) {
            System.out.println("Sukces: Wypożyczono książkę.");
        }
    }

    private static void handleReturn() {
        int userId = authService.getCurrentUser().getId();
        System.out.println("Twoje aktywne wypożyczenia:");
        loanRepo.getActiveLoansByUser(userId).forEach(System.out::println);

        System.out.print("Podaj ID książki do zwrotu: ");
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
                .category(String.valueOf(cId))
                .status("AVAILABLE").build();
        bookRepo.addBook(b);
    }

    private static void handleDeleteBook() {
        System.out.print("Podaj ID książki do usunięcia: ");
        int id = readInt();
        if (bookRepo.deleteBook(id)) System.out.println("Książka usunięta.");
    }

    private static void handleManageCategories() {
        System.out.println("1. Dodaj kategorię | 2. Usuń kategorię");
        int sub = readInt();
        if (sub == 1) {
            System.out.print("Nazwa kategorii: ");
            catRepo.addCategory(scanner.nextLine());
        } else if (sub == 2) {
            System.out.print("ID do usunięcia: ");
            catRepo.deleteCategory(readInt());
        }
    }

    private static void handleStats() {
        System.out.println("\n--- STATYSTYKI ---");
        System.out.println("Suma książek w systemie: " + bookRepo.countAllBooks());
        System.out.println("Historia wszystkich wypożyczeń: " + loanRepo.getAllLoans().size());
    }

    // --- POMOCNICZE ---

    private static void ensureAdminExists() {
        if (userRepo.findByUsername("admin").isEmpty()) {
            User admin = User.builder()
                    .username("admin").password("admin123") // Zostanie zahashowane w repo
                    .firstName("Admin").lastName("Systemowy")
                    .role("ADMIN").build();
            userRepo.addUser(admin);
            System.out.println("[System] Utworzono konto administratora: admin / admin123");
        }
    }

    private static int readInt() {
        try {
            int val = Integer.parseInt(scanner.nextLine());
            return val;
        } catch (Exception e) {
            return -1;
        }
    }

    private static void accessDenied() {
        System.out.println("BŁĄD: Brak uprawnień administratora!");
    }
}