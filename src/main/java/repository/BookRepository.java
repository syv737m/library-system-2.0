package repository;

import model.Book;
import java.util.List;

public interface BookRepository {
    void addBook(Book book);
    List<Book> getAllBooks();
    List<Book> searchBooks(String query);
    List<Book> getBooksByCategory(int categoryId);
    boolean deleteBook(int bookId);
    int countAllBooks(); // Statystyka dla Admina
}