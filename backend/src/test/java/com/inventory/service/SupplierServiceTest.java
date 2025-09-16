package com.inventory.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.inventory.dto.Response;
import com.inventory.dto.SupplierDTO;
import com.inventory.entity.Supplier;
import com.inventory.repository.SupplierRepository;
import com.inventory.service.impl.SupplierServiceImpl;
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
public class SupplierServiceTest {
    @Mock
    private SupplierRepository supplierRepository;

    @InjectMocks
    private SupplierServiceImpl supplierService;

    @Qualifier("modelMapper")
    private ModelMapper modelMapper;
    private SupplierDTO supplierDTO;
    private Supplier supplier;

    @BeforeEach
    void setUp() throws IllegalAccessException, NoSuchFieldException {
        modelMapper = new ModelMapper();
        supplierDTO = SupplierDTO.builder().id(1L).name("NCC Thang Long").email("thanglong@yahoo.com").phone("039584728").address("Ha Noi").build();
        supplier = modelMapper.map(supplierDTO, Supplier.class);
        // Tiem modelMapper vao supplierService (co phuong thuc ser dung mapper nen p set ko thi null excep)
        Field field = SupplierServiceImpl.class.getDeclaredField("modelMapper");
        field.setAccessible(true);
        field.set(supplierService, modelMapper);
    }

    @DisplayName("Testcase: kiem thu phuong thuc addSupplier")
    @Test
    public void testAddSupplier() throws JsonProcessingException {
        //Given phuong thuc will return: chi dinh kq tra ve
        BDDMockito.given(supplierRepository.findByName(supplierDTO.getName())).willReturn(Optional.empty());
        BDDMockito.given(supplierRepository.findByEmail(supplierDTO.getEmail())).willReturn(Optional.empty());
        BDDMockito.given(supplierRepository.findByPhone(supplierDTO.getPhone())).willReturn(Optional.empty());
        //Không cần test modelMapper
        BDDMockito.given(supplierRepository.save(supplier)).willReturn(supplier);
        //Sau là test tổng quan - return
        Response response = supplierService.addSupplier(supplierDTO);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @DisplayName("Testcase: kiem thu phuong thuc getAll")
    @Test
    public void testGetAllSuppliers() {
        BDDMockito.given(supplierRepository.findAll(Sort.by(Sort.Direction.DESC, "id"))).willReturn(List.of(supplier));
        //Test return kem supplierDTOS
        Response response = supplierService.getAllSuppliers();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getSuppliers().getFirst().getId()).isEqualTo(1L);
    }

    @DisplayName("Testcase: kiem thu phuong thuc getId")
    @Test
    public void testGetSupplierById() {
        BDDMockito.given(supplierRepository.findById(1L)).willReturn(Optional.of(supplier));

        //Test return kem supplierDTO
        Response response = supplierService.getSupplierById(1L);
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getSupplier().getId()).isEqualTo(1L);
    }

    @DisplayName("Testcase: kiem thu phuong thuc update")
    @Test
    public void testUpdateSupplier() throws JsonProcessingException {
        supplierDTO.setName("NCC Cau Giay");
        BDDMockito.given(supplierRepository.findById(1L)).willReturn(Optional.of(supplier));
        BDDMockito.given(supplierRepository.save(supplier)).willReturn(supplier);
        Response response = supplierService.updateSupplier(1L, supplierDTO);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @DisplayName("Testcase: kiem thu phuong thuc delete")
    @Test
    public void testDeleteSupplier() throws JsonProcessingException {
        BDDMockito.given(supplierRepository.findById(1L)).willReturn(Optional.of(supplier));
        Response response = supplierService.deleteSupplier(1L);
        assertThat(response.getStatus()).isEqualTo(200);
    }
}
