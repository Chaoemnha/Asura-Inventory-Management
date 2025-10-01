package com.inventory.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.inventory.enums.TransactionStatus;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionUpdateRequest {

    @Positive(message = "Product id is required")
    private Long productId;

    @Positive(message = "Quantity is required")
    private Integer quantity;

    private Long supplierId;

    private Long userId;

    private String description;

    private BigDecimal totalPrice;

    private TransactionStatus status;
}