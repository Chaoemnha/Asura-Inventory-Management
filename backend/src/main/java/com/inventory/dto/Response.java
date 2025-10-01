package com.inventory.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.inventory.enums.UserRole;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Response {
    //generic
    private int status;
    private String message;
    
    // Generic data field for any type of data
    private Object data;
    
    //for login
    private String token;
    private UserRole role;
    private String expirationTime;

    //for pagination
    private Integer totalPages;
    private Long totalElements;
    private Integer currentPage;

    //data output optional
    private UserDTO user;
    private List<UserDTO> users;

    private SupplierDTO supplier;
    private List<SupplierDTO> suppliers;

    private CategoryDTO category;
    private List<CategoryDTO> categories;

    private ProductDTO product;
    private List<ProductDTO> products;

    private TransactionDTO transaction;
    private List<TransactionDTO> transactions;

    private ActivityReport activityReport;
    
    private List<Map<String, Object>> bestSellingProducts;

    private final LocalDateTime timestamp = LocalDateTime.now();
}
