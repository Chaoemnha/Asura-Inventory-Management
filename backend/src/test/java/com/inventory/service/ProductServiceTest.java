package com.inventory.service;

import com.inventory.dto.ProductDTO;
import com.inventory.dto.Response;
import com.inventory.entity.Category;
import com.inventory.entity.Product;
import com.inventory.repository.CategoryRepository;
import com.inventory.repository.ProductRepository;
import com.inventory.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Sort;
import org.springframework.mock.web.MockMultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {
    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    @Qualifier("modelMapper")
    private ModelMapper modelMapper;
    private ProductDTO productDTO;
    private MockMultipartFile mockFile;
    private Product product;

    @BeforeEach
    void setUp() throws IllegalAccessException, NoSuchFieldException, IOException {
        modelMapper = new ModelMapper();
        //Hau het cac ham deu nhan DTO va file
        productDTO = ProductDTO.builder()
                .name("Asus")
                .sku("nl1")
                .price(BigDecimal.valueOf(10000000))
                .stockQuantity(2)
                .description("A laptop")
                .categoryId(1L)
                .id(1L)
                .build();
        product = modelMapper.map(productDTO, Product.class);
        FileInputStream inputStream = new FileInputStream(new File("D:/GitHub/InventoryManagement/backend/src/test/a.jpg"));
        //mock ho tro tao multipartFile cho test, may quá :)
        mockFile = new MockMultipartFile(
                "a","a.jpg", "image/jpg", inputStream
        );
        // Tiem modelMapper vao productService (co phuong thuc ser dung mapper nen p set ko thi null excep)
        Field field = ProductServiceImpl.class.getDeclaredField("modelMapper");
        field.setAccessible(true);
        field.set(productService, modelMapper);
    }

    @DisplayName("Testcase: kiem thu phuong thuc saveProduct")
    @Test
    public void testSaveProduct() {
        //Given phuong thuc will return: chi dinh kq tra ve
        Category category = Category.builder().id(1L).name("New laptop").build();
        given(categoryRepository.findById(productDTO.getCategoryId())).willReturn(Optional.of(category));
        //Sau là test tổng quan - return
        //Phần save ảnh cần ảnh tồn tại ở đường dẫn local, em tự tạo file ảnh test để tạo FileInputStream cho có đường dẫn truy cập ảnh
        Response response = productService.saveProduct(productDTO, mockFile);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @DisplayName("Testcase: kiem thu phuong thuc update")
    @Test
    public void testUpdateProduct() {
        productDTO.setName("laptop");
        given(productRepository.findById(productDTO.getId())).willReturn(Optional.of(product));
        Category category = Category.builder().id(1L).name("New laptop").build();
        given(categoryRepository.findById(productDTO.getCategoryId())).willReturn(Optional.of(category));
        //Bo qua phan test
        Response response = productService.updateProduct(productDTO, mockFile);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @DisplayName("Testcase: kiem thu phuong thuc getAll")
    @Test
    public void testGetAllProducts() {

        given(productRepository.findAll(Sort.by(Sort.Direction.DESC, "id"))).willReturn(List.of(product));
        //Test return kem productDTOS
        Response response = productService.getAllProducts();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response
                .getProducts()
                .getFirst()
                .getId()).isEqualTo(1L);
    }

    @DisplayName("Testcase: kiem thu phuong thuc getId")
    @Test
    public void testGetProductById() {
        given(productRepository.findById(1L)).willReturn(Optional.of(product));

        //Test return kem productDTO
        Response response = productService.getProductById(1L);
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getProduct().getId()).isEqualTo(1L);
    }


    @DisplayName("Testcase: kiem thu phuong thuc delete")
    @Test
    public void testDeleteProduct() {
        given(productRepository.findById(1L)).willReturn(Optional.of(product));
        Response response = productService.deleteProduct(1L);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @DisplayName("Testcase: kiem thu phuong thuc extract")
    @Test
    public void extractTextForEmbedding() {
        given(productRepository.findAll(Sort.by(Sort.Direction.DESC, "id"))).willReturn(List.of(product));
        List<String> res = productService.extractTextForEmbedding();
        assertThat(res.size()).isEqualTo(1);
    }
}
