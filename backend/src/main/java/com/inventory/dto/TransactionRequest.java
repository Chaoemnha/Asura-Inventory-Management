package com.inventory.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.inventory.enums.TransactionStatus;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionRequest {

    @Positive(message = "Cần nhập số id sản phẩm dương")
    private Long productId;

    @Positive(message = "Cần nhập số lượng dương")
    private Integer quantity;

    private Long supplierId;

    private Long userId;

    private TransactionStatus status;

    private String description;

    private BigDecimal price;

    private Long senderId;

    private LocalDateTime updatedAt;

    private LocalDateTime createdAt;
}
