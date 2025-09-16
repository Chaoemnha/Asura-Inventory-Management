package com.inventory.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.inventory.dto.CategoryDTO;
import com.inventory.dto.Response;

import java.util.List;

public interface CategoryService {
    Response createCategory(CategoryDTO categoryDTO) throws JsonProcessingException;
    Response getAllCategories();
    Response getCategoryById(Long id);
    Response updateCategory(Long id, CategoryDTO categoryDTO) throws JsonProcessingException;
    Response deleteCategory(Long id) throws JsonProcessingException;
}
