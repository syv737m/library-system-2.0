package service;

import model.Book;
import model.Reservation;
import repository.BookRepository;
import repository.LoanRepository;
import repository.ReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

@Service
public class LibraryService {

    private final LoanRepository loanRepository;
    private final BookRepository bookRepository;
    private final ReservationRepository reservationRepository;

    public LibraryService(LoanRepository loanRepository, BookRepository bookRepository, ReservationRepository reservationRepository) {
        this.loanRepository = loanRepository;
        this.bookRepository = bookRepository;
        this.reservationRepository = reservationRepository;
    }

    @Transactional
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

        loanRepository.createLoan(userId, bookId);
        bookRepository.updateBookStatus(bookId, "LOANED", null);
        reservationRepository.findReservationsByBookId(bookId).stream()
                .filter(r -> r.getUserId() == userId)
                .findFirst()
                .ifPresent(r -> reservationRepository.deleteReservation(r.getId()));

        return true;
    }

    @Transactional
    public boolean returnBook(int bookId, int userId) {
        boolean updated = loanRepository.returnLoan(bookId, userId);
        if (!updated) {
            return false;
        }

        Optional<Reservation> nextReservation = reservationRepository.findNextReservationForBook(bookId);
        if (nextReservation.isPresent()) {
            bookRepository.updateBookStatus(bookId, "RESERVED", nextReservation.get().getUserId());
        } else {
            bookRepository.updateBookStatus(bookId, "AVAILABLE", null);
        }
        return true;
    }
}