package model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SearchCriteria {
    private String title;
    private String author;
    private Integer year;
    private Integer categoryId;

    private String sortBy;
    private String sortOrder;

    private int page;
    private int pageSize;
}