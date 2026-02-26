package repository;

import model.Book;
import model.Loan;
import java.util.List;
import java.util.Map;

public interface LoanRepository {
    void createLoan(int userId, int bookId);
    boolean returnLoan(int bookId, int userId);
    List<Loan> getAllLoans();
    List<Loan> getActiveLoansByUser(int userId);
    List<Map.Entry<String, Long>> getMostLoanedBooks(int limit);
}