package com.inventory.service.impl;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.dto.CategoryDTO;
import com.inventory.dto.Response;
import com.inventory.entity.Category;
import com.inventory.exceptions.InvalidCredentialsException;
import com.inventory.exceptions.NotFoundException;
import com.inventory.repository.CategoryRepository;
import com.inventory.service.CategoryService;
import com.inventory.utils.WebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    @Autowired
    @Qualifier("modelMapper")
    private final ModelMapper modelMapper;

    @Override
    public Response createCategory(CategoryDTO categoryDTO) throws JsonProcessingException {
        Optional<Category> category = categoryRepository.findByName(categoryDTO.getName());
        if (category.isPresent()) {
            throw new InvalidCredentialsException("Category already exists");
        }
        Category categoryToSave = modelMapper.map(categoryDTO, Category.class);
        Category result = categoryRepository.save(categoryToSave);
        Map<String, Object> message = new HashMap<>();
        message.put("type", "CATEGORY_ADDED");
        message.put("data", Map.of(
                "id", result.getId().toString(),
                "name", result.getName()
        ));

        ObjectMapper mapper = new ObjectMapper();
        String jsonString = mapper.writeValueAsString(message);
        WebSocketHandler.broadcastChanges(jsonString);
        return Response.builder()
                .status(200)
                .message("Category created successfully")
                .build();
    }

    @Override
    public Response getAllCategories() {

        List<Category> categories = categoryRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));

        List<CategoryDTO> categoryDTOS = modelMapper.map(categories, new TypeToken<List<CategoryDTO>>() {}.getType());

        return Response.builder()
                .status(200)
                .message("success")
                .categories(categoryDTOS)
                .build();
    }

    @Override
    public Response getCategoryById(Long id) {

        Category category = categoryRepository.findById(id)
                .orElseThrow(()-> new NotFoundException("Category Not Found"));
        CategoryDTO categoryDTO = modelMapper.map(category, CategoryDTO.class);

        return Response.builder()
                .status(200)
                .message("success")
                .category(categoryDTO)
                .build();
    }

    @Override
    public Response updateCategory(Long id, CategoryDTO categoryDTO) throws JsonProcessingException {

        Category existingCategory = categoryRepository.findById(id)
                .orElseThrow(()-> new NotFoundException("Category Not Found"));

        existingCategory.setName(categoryDTO.getName());
        categoryRepository.save(existingCategory);
        Map<String, Object> message = new HashMap<>();
        message.put("type", "CATEGORY_UPDATED");
        message.put("data", Map.of(
                "id", existingCategory.getId().toString(),
                "name", existingCategory.getName()
        ));

        ObjectMapper mapper = new ObjectMapper();
        String jsonString = mapper.writeValueAsString(message);
        WebSocketHandler.broadcastChanges(jsonString);
        return Response.builder()
                .status(200)
                .message("Category Successfully Updated")
                .build();

    }

    @Override
    public Response deleteCategory(Long id) throws JsonProcessingException {

         categoryRepository.findById(id)
                .orElseThrow(()-> new NotFoundException("Category Not Found"));

        categoryRepository.deleteById(id);
        Map<String, Object> message = new HashMap<>();
        message.put("type", "CATEGORY_DELETED");
        message.put("categoryId", id);
        ObjectMapper mapper = new ObjectMapper();
        String jsonString = mapper.writeValueAsString(message);
        WebSocketHandler.broadcastChanges(jsonString);
        return Response.builder()
                .status(200)
                .message("Category Successfully Deleted")
                .build();
    }

    @Override
    public List<String> extractTextForEmbedding() {
        List<Category> categories = categoryRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
        List<CategoryDTO> categoryDTOS = modelMapper.map(categories, new TypeToken<List<CategoryDTO>>() {}.getType());
        return categoryDTOS.stream().map(CategoryDTO::getTextForEmbedding).collect(Collectors.toList());
    }
}
