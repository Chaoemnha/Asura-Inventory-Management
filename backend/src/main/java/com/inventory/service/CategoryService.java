package com.inventory.service;

import com.inventory.dto.CategoryDTO;
import com.inventory.dto.Response;

import java.util.List;

public interface CategoryService {
    Response createCategory(CategoryDTO categoryDTO);
    Response getAllCategories();
    Response getCategoryById(Long id);
    Response updateCategory(Long id, CategoryDTO categoryDTO);
    Response deleteCategory(Long id);
    List<String> extractTextForEmbedding();
}
