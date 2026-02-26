package model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Book {
    private int id;
    private String title;
    private String author;
    private int publicationYear;
    private String isbn;
    private int categoryId;
    private String status;
    private Integer reservedForUserId;
    private boolean isDeleted;
}