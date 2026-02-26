package repository;

import model.Book;
import model.SearchCriteria;

import java.util.List;
import java.util.Optional;

public interface BookRepository {
    void addBook(Book book);
    List<Book> getAllBooks();
    Optional<Book> findById(int id);
    List<Book> searchBooks(String query);
    List<Book> searchBooks(SearchCriteria criteria);
    int countBooks(SearchCriteria criteria);
    List<Book> getBooksByCategory(int categoryId);
    boolean deleteBook(int bookId);
    int countAllBooks();
    int countByStatus(String status);
}