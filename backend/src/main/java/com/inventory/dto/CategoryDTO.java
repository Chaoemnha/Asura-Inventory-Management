package com.inventory.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.inventory.service.EmbeddableText;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CategoryDTO implements EmbeddableText {

    private Long id;

    @NotBlank(message = "Name is required")
    private String name;

    @Override
    public String getTextForEmbedding() {
        return String.format("Category: %s", this.getName());
    }

}
