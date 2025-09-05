package com.inventory.repository;

import com.inventory.entity.Product;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class ProductRepositoryTest {
    @Autowired
    private ProductRepository productRepository;

    private Product product;

    @BeforeEach
    void setUp() {
        product = new Product();
        product.setName("MacBook M1");
    }

    @Test
    @DisplayName("Luu va tim san pham theo ID")
    void testSaveAndFindById(){
        Product saved = productRepository.save(product);

        Optional<Product> found = productRepository.findById(saved.getId());
        // Testcase: Kiểm tra xem sản phẩm đã được lưu và tìm lại bằng ID có tồn tại không
        assertThat(found).isPresent();
        // Testcase: Kiểm tra tên của sản phẩm tìm được có khớp với giá trị ban đầu không
        assertThat(found.get().getName()).isEqualTo("MacBook M1");
    }

    @Test
    @DisplayName("Luu va lay tat ca san pham")
    void testSaveAndFindAll(){
        Product saved = productRepository.save(product);

        List<Product> products = productRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
        // Testcase: Kiểm tra danh sách sản phẩm sau khi lưu có không rỗng không
        assertThat(products).isNotEmpty();
        // Testcase: Kiểm tra sản phẩm đầu tiên trong danh sách có tên khớp với giá trị ban đầu không
        assertThat(products.get(0).getName()).isEqualTo("MacBook M1");
    }

    @Test
    @DisplayName("Luu va xoa san pham theo ID")
    void testSaveAndDeleteById(){
        Product saved = productRepository.save(product);

        productRepository.deleteById(saved.getId());
        Optional<Product> found = productRepository.findById(saved.getId());
        // Testcase: Kiểm tra xem sản phẩm đã bị xóa và không còn tồn tại không
        assertThat(found).isNotPresent();
    }
}
