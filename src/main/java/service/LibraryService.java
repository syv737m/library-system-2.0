package service;

import model.Book;
import repository.BookRepository;
import repository.LoanRepository;

import java.util.Optional;

public class LibraryService {

    private final LoanRepository loanRepository;
    private final BookRepository bookRepository;

    public LibraryService(LoanRepository loanRepository, BookRepository bookRepository) {
        this.loanRepository = loanRepository;
        this.bookRepository = bookRepository;
    }

    public boolean rentBook(int userId, int bookId) {
        Optional<Book> bookOpt = bookRepository.findById(bookId);

        if (bookOpt.isEmpty()) {
            System.out.println("Książka o podanym ID nie istnieje.");
            return false;
        }

        Book book = bookOpt.get();
        if (book.getStatus().equals("LOANED")) {
            System.out.println("Książka jest już wypożyczona. Możesz ją zarezerwować.");
            return false;
        }

        if (book.getStatus().equals("RESERVED") && !book.getReservedForUserId().equals(userId)) {
            System.out.println("Książka jest zarezerwowana dla innego użytkownika.");
            return false;
        }

        // Jeśli doszliśmy tutaj, książka jest 'AVAILABLE' lub 'RESERVED' dla tego usera
        return loanRepository.loanBook(userId, bookId);
    }
}