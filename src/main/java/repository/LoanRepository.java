package repository;

import model.Loan;
import java.util.List;

public interface LoanRepository {
    boolean loanBook(int userId, int bookId);
    boolean returnBook(int bookId, int userId);
    List<Loan> getAllLoans();
    List<Loan> getActiveLoansByUser(int userId); // Potrzebne dla USERA
}