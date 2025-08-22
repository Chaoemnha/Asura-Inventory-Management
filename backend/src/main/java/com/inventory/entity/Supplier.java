package com.inventory.entity;

import com.inventory.service.EmbeddableText;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "suppliers")
public class Supplier implements EmbeddableText {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
