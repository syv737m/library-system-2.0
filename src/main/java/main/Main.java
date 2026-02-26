package main;

import config.AppConfig;
import model.Book;
import model.Category;
import model.SearchCriteria;
import model.User;
import repository.*;
import service.AuthService;
import service.LibraryService;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;

public class Main {

    private static final Scanner scanner = new Scanner(System.in);
    private static final int PAGE_SIZE = 5;

    public static void main(String[] args) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);

        BookRepository bookRepo = context.getBean(BookRepository.class);
        CategoryRepository catRepo = context.getBean(CategoryRepository.class);
        LoanRepository loanRepo = context.getBean(LoanRepository.class);
        ReservationRepository reservationRepo = context.getBean(ReservationRepository.class);
        AuthService authService = context.getBean(AuthService.class);
        LibraryService libraryService = context.getBean(LibraryService.class);
        UserRepository userRepo = context.getBean(UserRepository.class);

        System.out.println("==========================================");
        System.out.println("   TERMINALOWY MENEDŻER BIBLIOTEKI JDBC   ");
        System.out.println("==========================================");

        while (authService.getCurrentUser() == null) {
            handleLogin(authService, reservationRepo);
        }

        runMainMenu(authService, libraryService, bookRepo, catRepo, loanRepo, reservationRepo, userRepo);

        context.close();
    }

    private static void handleLogin(AuthService authService, ReservationRepository reservationRepo) {
        System.out.println("\n>>> LOGOWANIE");
        System.out.print("Login: ");
        String login = scanner.nextLine();
        System.out.print("Hasło: ");
        String pass = scanner.nextLine();

        if (authService.login(login, pass)) {
            User user = authService.getCurrentUser();
            System.out.println("\nWitaj " + user.getFirstName() + "! Zalogowano jako: " + user.getRole());
            checkNotifications(reservationRepo, user.getId());
        } else {
            System.out.println("BŁĄD: Niepoprawny login lub hasło.");
        }
    }

    private static void checkNotifications(ReservationRepository reservationRepo, int userId) {
        List<Book> reservedBooks = reservationRepo.findBooksReservedByUser(userId);
        if (!reservedBooks.isEmpty()) {
            System.out.println("\n--- POWIADOMIENIA ---");
            System.out.println("Następujące książki czekają na Ciebie do odbioru:");
            reservedBooks.forEach(book -> System.out.println("- " + book.getTitle()));
            System.out.println("---------------------\n");
        }
    }

    private static void runMainMenu(AuthService authService, LibraryService libraryService, BookRepository bookRepo, CategoryRepository catRepo, LoanRepository loanRepo, ReservationRepository reservationRepo, UserRepository userRepo) {
        boolean running = true;
        while (running) {
            User user = authService.getCurrentUser();
            boolean isAdmin = authService.isAdmin();

            printMenu(user.getRole());
            int choice = readInt();

            switch (choice) {
                case 1 -> bookRepo.getAllBooks().forEach(System.out::println);
                case 2 -> handleSearch(bookRepo, catRepo);
                case 3 -> handleCategoryFilter(bookRepo, catRepo);
                case 4 -> handleReserveBook(authService, bookRepo, reservationRepo);
                case 5 -> handleMyReservations(reservationRepo, authService);
                case 6 -> handleLoan(libraryService, authService);
                case 7 -> handleReturn(libraryService, loanRepo, authService);
                case 8 -> { if(isAdmin) handleAddBook(bookRepo, catRepo); else accessDenied(); }
                case 9 -> { if(isAdmin) handleDeleteBook(bookRepo); else accessDenied(); }
                case 10 -> { if(isAdmin) handleManageCategories(catRepo); else accessDenied(); }
                case 11 -> { if(isAdmin) handleStats(bookRepo, loanRepo, userRepo); else accessDenied(); }
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

        if ("ADMIN".equals(role)) {
            System.out.println("[ADMIN] 8. Dodaj książkę");
            System.out.println("[ADMIN] 9. Usuń książkę");
            System.out.println("[ADMIN] 10. Zarządzaj kategoriami");
            System.out.println("[ADMIN] 11. Statystyki");
        }
        System.out.println("0. Wyloguj i wyjdź");
        System.out.print("Wybór: ");
    }

    private static void handleSearch(BookRepository bookRepo, CategoryRepository catRepo) {
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
            handleAdvancedSearch(bookRepo, catRepo);
        }
    }

    private static void handleAdvancedSearch(BookRepository bookRepo, CategoryRepository catRepo) {
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

    private static void handleCategoryFilter(BookRepository bookRepo, CategoryRepository catRepo) {
        List<Category> cats = catRepo.getAllCategories();
        if (cats.isEmpty()) { System.out.println("Brak kategorii."); return; }

        cats.forEach(c -> System.out.println(c.getId() + ". " + c.getName()));
        System.out.print("Podaj ID kategorii: ");
        int id = readInt();
        bookRepo.getBooksByCategory(id).forEach(System.out::println);
    }

    private static void handleReserveBook(AuthService authService, BookRepository bookRepo, ReservationRepository reservationRepo) {
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

    private static void handleMyReservations(ReservationRepository reservationRepo, AuthService authService) {
        System.out.println("\n--- KSIĄŻKI OCZEKUJĄCE NA ODBIÓR ---");
        List<Book> reservedBooks = reservationRepo.findBooksReservedByUser(authService.getCurrentUser().getId());
        if (reservedBooks.isEmpty()) {
            System.out.println("Brak książek oczekujących na odbiór.");
        } else {
            reservedBooks.forEach(System.out::println);
        }
    }

    private static void handleLoan(LibraryService libraryService, AuthService authService) {
        System.out.print("Podaj ID książki do wypożyczenia: ");
        int bookId = readInt();
        if (libraryService.rentBook(authService.getCurrentUser().getId(), bookId)) {
            System.out.println("Sukces: Wypożyczono książkę.");
        }
    }

    private static void handleReturn(LibraryService libraryService, LoanRepository loanRepo, AuthService authService) {
        int userId = authService.getCurrentUser().getId();
        System.out.println("Twoje aktywne wypożyczenia:");
        loanRepo.getActiveLoansByUser(userId).forEach(System.out::println);

        System.out.print("\nPodaj ID książki do zwrotu: ");
        int bookId = readInt();
        if (libraryService.returnBook(bookId, userId)) {
            System.out.println("Sukces: Zwrócono książkę.");
        } else {
            System.out.println("Błąd: Nie masz wypożyczonej tej książki.");
        }
    }

    private static void handleAddBook(BookRepository bookRepo, CategoryRepository catRepo) {
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

    private static void handleDeleteBook(BookRepository bookRepo) {
        System.out.print("Podaj ID książki do usunięcia: ");
        int id = readInt();
        if (bookRepo.deleteBook(id)) System.out.println("Książka usunięta.");
    }

    private static void handleManageCategories(CategoryRepository catRepo) {
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

    private static void handleStats(BookRepository bookRepo, LoanRepository loanRepo, UserRepository userRepo) {
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