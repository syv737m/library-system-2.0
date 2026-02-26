package service;

import model.Book;
import model.Reservation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import repository.BookRepository;
import repository.LoanRepository;
import repository.ReservationRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LibraryServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @InjectMocks
    private LibraryService libraryService;

    private Book availableBook;
    private Book loanedBook;
    private Book reservedBook;

    @BeforeEach
    void setUp() {
        availableBook = Book.builder().id(1).title("Available Book").status("AVAILABLE").build();
        loanedBook = Book.builder().id(2).title("Loaned Book").status("LOANED").build();
        reservedBook = Book.builder().id(3).title("Reserved Book").status("RESERVED").reservedForUserId(100).build();
    }

    @Test
    void rentBook_shouldSucceed_whenBookIsAvailable() {
        // Given
        when(bookRepository.findById(1)).thenReturn(Optional.of(availableBook));
        when(loanRepository.loanBook(1, 1)).thenReturn(true);

        // When
        boolean result = libraryService.rentBook(1, 1);

        // Then
        assertTrue(result);
        verify(loanRepository, times(1)).loanBook(1, 1);
    }

    @Test
    void rentBook_shouldFail_whenBookIsLoaned() {
        // Given
        when(bookRepository.findById(2)).thenReturn(Optional.of(loanedBook));

        // When
        boolean result = libraryService.rentBook(1, 2);

        // Then
        assertFalse(result);
        verify(loanRepository, never()).loanBook(anyInt(), anyInt());
    }

    @Test
    void rentBook_shouldFail_whenBookIsReservedForAnotherUser() {
        // Given
        when(bookRepository.findById(3)).thenReturn(Optional.of(reservedBook));

        // When
        boolean result = libraryService.rentBook(1, 3); // User 1 tries to rent a book reserved for user 100

        // Then
        assertFalse(result);
        verify(loanRepository, never()).loanBook(anyInt(), anyInt());
    }

    @Test
    void rentBook_shouldSucceed_whenBookIsReservedForTheCurrentUser() {
        // Given
        when(bookRepository.findById(3)).thenReturn(Optional.of(reservedBook));
        when(loanRepository.loanBook(100, 3)).thenReturn(true);

        // When
        boolean result = libraryService.rentBook(100, 3); // User 100 rents their reserved book

        // Then
        assertTrue(result);
        verify(loanRepository, times(1)).loanBook(100, 3);
    }

    @Test
    void returnBook_shouldMakeBookAvailable_whenNoReservationsExist() {
        // Given
        when(loanRepository.returnLoan(1, 1)).thenReturn(true);
        when(reservationRepository.findNextReservationForBook(1)).thenReturn(Optional.empty());

        // When
        boolean result = libraryService.returnBook(1, 1);

        // Then
        assertTrue(result);
        verify(bookRepository, times(1)).updateBookStatus(1, "AVAILABLE", null);
    }

    @Test
    void returnBook_shouldMakeBookReserved_whenReservationExists() {
        // Given
        int bookId = 2;
        int returningUserId = 1;
        int nextUserIdInQueue = 200;
        Reservation nextReservation = Reservation.builder().userId(nextUserIdInQueue).bookId(bookId).build();

        when(loanRepository.returnLoan(bookId, returningUserId)).thenReturn(true);
        when(reservationRepository.findNextReservationForBook(bookId)).thenReturn(Optional.of(nextReservation));

        // When
        boolean result = libraryService.returnBook(bookId, returningUserId);

        // Then
        assertTrue(result);
        verify(bookRepository, times(1)).updateBookStatus(bookId, "RESERVED", nextUserIdInQueue);
    }

    @Test
    void returnBook_shouldFail_whenUserDidNotLoanTheBook() {
        // Given
        when(loanRepository.returnLoan(1, 1)).thenReturn(false);

        // When
        boolean result = libraryService.returnBook(1, 1);

        // Then
        assertFalse(result);
        verify(bookRepository, never()).updateBookStatus(anyInt(), anyString(), any());
    }
}