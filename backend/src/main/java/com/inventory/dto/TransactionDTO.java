package com.inventory.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.inventory.enums.TransactionStatus;
import com.inventory.enums.TransactionType;
import com.inventory.service.EmbeddableText;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
public class TransactionDTO implements EmbeddableText {

    private Long id;

    private Integer totalProducts;

    private BigDecimal totalPrice;

    private TransactionType transactionType;

    private TransactionStatus status;

    private String description;

    private LocalDateTime updatedAt;

    private LocalDateTime createdAt;

    private UserDTO user;

    private ProductDTO product;

    private SupplierDTO supplier;

    @Override
    public String getTextForEmbedding() {
        return String.format("Transaction: %s, TotalProducts: %s, TotalPrice: %s, TransactionType: %s, Status: %s, UpdatedAt: %s, CreatedAt: %s",
                this.getId(), this.getTotalProducts(), this.getTotalPrice().toPlainString(), this.getTransactionType().toString(), this.getStatus(), this.getUpdatedAt(), this.getCreatedAt());
    }
}
