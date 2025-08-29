package com.inventory.repository;

import com.inventory.entity.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class CategoryRepositoryTest {
    @Autowired
    private CategoryRepository categoryRepository;

    private Category category;

    @BeforeEach
    void setUp() {
        category = new Category();
        category.setName("New laptop");
    }

    @Test
    @DisplayName("Luu va tim danh muc theo ID")
    void testSaveAndFindById(){
        Category saved = categoryRepository
                .save(category);

        Optional<Category> found = categoryRepository.findById(saved.getId());
        // Testcase: Kiểm tra xem danh mục đã được lưu và tìm lại bằng ID có tồn tại không
        assertThat(found).isPresent();
        // Testcase: Kiểm tra tên của danh mục tìm được có khớp với giá trị ban đầu không
        assertThat(found.get().getName()).isEqualTo("New laptop");
    }

    @Test
    @DisplayName("Luu va lay tat ca danh muc")
    void testSaveAndFindAll(){
        Category saved = categoryRepository.save(category);

        List<Category> categories = categoryRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
        // Testcase: Kiểm tra danh sách danh mục sau khi lưu có không rỗng không
        assertThat(categories).isNotEmpty();
        // Testcase: Kiểm tra danh mục đầu tiên trong danh sách có tên khớp với giá trị ban đầu không
        assertThat(categories.get(0).getName()).isEqualTo("New laptop");
    }

    @Test
    @DisplayName("Luu va xoa danh muc theo ID")
    void testSaveAndDeleteById(){
        Category saved = categoryRepository.save(category);

        categoryRepository.deleteById(saved.getId());
        Optional<Category> found = categoryRepository.findById(saved.getId());
        // Testcase: Kiểm tra xem danh mục đã bị xóa và không còn tồn tại không
        assertThat(found).isNotPresent();
    }

}
