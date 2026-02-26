package service;

import model.Book;
import repository.BookRepository;
import repository.LoanRepository;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

@Service
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
        if ("LOANED".equals(book.getStatus())) {
            System.out.println("Książka jest już wypożyczona. Możesz ją zarezerwować.");
            return false;
        }

        if ("RESERVED".equals(book.getStatus()) && !Objects.equals(book.getReservedForUserId(), userId)) {
            System.out.println("Książka jest zarezerwowana dla innego użytkownika.");
            return false;
        }

        return loanRepository.loanBook(userId, bookId);
    }
}