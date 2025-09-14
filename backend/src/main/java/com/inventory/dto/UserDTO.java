package com.inventory.dto;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.inventory.enums.UserRole;
import com.inventory.service.EmbeddableText;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
public class UserDTO implements EmbeddableText {

    private Long id;

    private String name;

    private String email;

    @JsonIgnore
    private String password;

    private String phoneNumber;

    private UserRole role;

    private LocalDateTime createdAt;

    private List<TransactionDTO> transactions;

    private SupplierDTO supplier;

    @Override
    public String getTextForEmbedding() {
        return String.format("User: %s, Email: %s, PhoneNumber: %s, Role: %s", this.getName(),
                this.getEmail(), this.getPhoneNumber(), this.getRole().toString());
    }

}