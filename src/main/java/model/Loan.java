package model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Loan {
    private int id;
    private int userId;
    private int bookId;
    private LocalDateTime loanDate;
    private LocalDateTime returnDate;
}