package com.inventory.repository;

import com.inventory.entity.Supplier;
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
public class SupplierRepositoryTest {
    @Autowired
    private SupplierRepository supplierRepository;

    private Supplier supplier;

    @BeforeEach
    void setUp() {
        supplier = new Supplier();
        supplier.setName("NCCThanhLong");
    }

    @Test
    @DisplayName("Luu va tim nha cung cap theo ID")
    void testSaveAndFindById(){
        Supplier saved = supplierRepository.save(supplier);

        Optional<Supplier> found = supplierRepository.findById(saved.getId());
        // Testcase: Kiểm tra xem nhà cung cấp đã được lưu và tìm lại bằng ID có tồn tại không
        assertThat(found).isPresent();
        // Testcase: Kiểm tra tên của nhà cung cấp tìm được có khớp với giá trị ban đầu không
        assertThat(found.get().getName()).isEqualTo("NCCThanhLong");
    }

    @Test
    @DisplayName("Luu va lay tat ca nha cung cap")
    void testSaveAndFindAll(){
        Supplier saved = supplierRepository.save(supplier);

        List<Supplier> suppliers = supplierRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
        // Testcase: Kiểm tra danh sách nhà cung cấp sau khi lưu có không rỗng không
        assertThat(suppliers).isNotEmpty();
        // Testcase: Kiểm tra nhà cung cấp đầu tiên trong danh sách có tên khớp với giá trị ban đầu không
        assertThat(suppliers.get(0).getName()).isEqualTo("NCCThanhLong");
    }

    @Test
    @DisplayName("Luu va xoa nha cung cap theo ID")
    void testSaveAndDeleteById(){
        Supplier saved = supplierRepository.save(supplier);

        supplierRepository.deleteById(saved.getId());
        Optional<Supplier> found = supplierRepository.findById(saved.getId());
        // Testcase: Kiểm tra xem nhà cung cấp đã bị xóa và không còn tồn tại không
        assertThat(found).isNotPresent();
    }
}
