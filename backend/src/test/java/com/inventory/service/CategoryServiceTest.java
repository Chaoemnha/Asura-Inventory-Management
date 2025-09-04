package com.inventory.service;

import com.inventory.dto.CategoryDTO;
import com.inventory.dto.Response;
import com.inventory.entity.Category;
import com.inventory.repository.CategoryRepository;
import com.inventory.service.impl.CategoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Sort;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class CategoryServiceTest {
    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    @Qualifier("modelMapper")
    private ModelMapper modelMapper;
    private CategoryDTO categoryDTO;
    private Category category;

    @BeforeEach
    void setUp() throws IllegalAccessException, NoSuchFieldException {
        modelMapper = new ModelMapper();
        categoryDTO = CategoryDTO.builder().id(1L).name("electronics").build();
        category = modelMapper.map(categoryDTO, Category.class);
        // Tiem modelMapper vao categoryService (co phuong thuc ser dung mapper nen p set ko thi null excep)
        Field field = CategoryServiceImpl.class.getDeclaredField("modelMapper");
        field.setAccessible(true);
        field.set(categoryService, modelMapper);
    }

    @DisplayName("Testcase: kiem thu phuong thuc createCategory")
    @Test
    public void testCreateCategory() {
        //Given phuong thuc will return: chi dinh kq tra ve
        BDDMockito.given(categoryRepository.findByName(categoryDTO.getName())).willReturn(Optional.empty());
        //Không cần test modelMapper
        BDDMockito.given(categoryRepository.save(category)).willReturn(category);
        //Sau là test tổng quan - return
        Response response = categoryService.createCategory(categoryDTO);
        assertThat(response).isNotNull();
    }

    @DisplayName("Testcase: kiem thu phuong thuc getAll")
    @Test
    public void testGetAllCategories() {
        BDDMockito.given(categoryRepository.findAll(Sort.by(Sort.Direction.DESC, "id"))).willReturn(List.of(category));
        //Test return kem categoryDTOS
        Response response = categoryService.getAllCategories();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getCategories().getFirst().getId()).isEqualTo(1L);
    }

    @DisplayName("Testcase: kiem thu phuong thuc getId")
    @Test
    public void testGetCategoryById() {
        BDDMockito.given(categoryRepository.findById(1L)).willReturn(Optional.of(category));

        //Test return kem categoryDTO
        Response response = categoryService.getCategoryById(1L);
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getCategory().getId()).isEqualTo(1L);
    }

    @DisplayName("Testcase: kiem thu phuong thuc update")
    @Test
    public void testUpdateCategory() {
        categoryDTO.setName("laptop");
        BDDMockito.given(categoryRepository.findById(1L)).willReturn(Optional.of(category));
        BDDMockito.given(categoryRepository.save(category)).willReturn(category);
        Response response = categoryService.updateCategory(1L, categoryDTO);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @DisplayName("Testcase: kiem thu phuong thuc delete")
    @Test
    public void testDeleteCategory() {
        BDDMockito.given(categoryRepository.findById(1L)).willReturn(Optional.of(category));
        Response response = categoryService.deleteCategory(1L);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @DisplayName("Testcase: kiem thu phuong thuc extract")
    @Test
    public void extractTextForEmbedding() {
        BDDMockito.given(categoryRepository.findAll(Sort.by(Sort.Direction.DESC, "id"))).willReturn(List.of(category));
        List<String> res = categoryService.extractTextForEmbedding();
        assertThat(res.size()).isEqualTo(1);
    }
}
