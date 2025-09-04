package com.inventory.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.inventory.service.EmbeddableText;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
public class SupplierDTO implements EmbeddableText {

    private Long id;

    @NotBlank(message = "Name is required")
    private String name;

    private String email;

    private String phone;

    private String address;

    @Override
    public String getTextForEmbedding() {
        return String.format("Supplier: %s, Email: %s, Phone: %s, Address: %s", this.getName(),
                this.getEmail(), this.getPhone(), this.getAddress());
    }
}
