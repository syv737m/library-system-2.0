package repository;

import model.Book;
import model.Reservation;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository {
    void addReservation(int userId, int bookId);
    Optional<Reservation> findNextReservationForBook(int bookId);
    void deleteReservation(int reservationId);
    List<Reservation> findReservationsByBookId(int bookId);
    List<Book> findBooksReservedByUser(int userId);
}