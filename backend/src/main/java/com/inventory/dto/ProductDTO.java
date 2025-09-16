package com.inventory.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
public class ProductDTO {

    private Long id;

    private Long categoryId;

    private String name;

    private String sku;

    private BigDecimal price;

    private Integer stockQuantity;

    private String description;

    private String imageUrl;

    private LocalDateTime expiryDate;

    private  LocalDateTime updatedAt;

    private LocalDateTime createdAt;

}
