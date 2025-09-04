package com.inventory.service;

import com.inventory.repository.ProductRepository;
import com.inventory.repository.TransactionRepository;
import com.inventory.service.impl.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
@ExtendWith(MockitoExtension.class)
public class RagDataExtractionServiceTest {
    @InjectMocks
    private RagDataExtractionService ragDataExtractionService;
    @Mock
    private CategoryServiceImpl categoryServiceImpl;
    @Mock
    private ProductServiceImpl productServiceImpl;
    @Mock
    private TransactionServiceImpl transactionServiceImpl;
    @Mock
    private SupplierServiceImpl supplierServiceImpl;
    @Mock
    private UserServiceImpl userServiceImpl;
    @BeforeEach
    void setUp() {

    }
    //Thuc ra chang can phai test vi no chi la phuong thuc add List nguyen thuy :(
    @DisplayName("Testcase: kiem thu phuong thuc extractAllDataForEmbedding(giai nen tat ca du lieu ra 1 text pvu embed)")
    @Test
    void extractAllDataForEmbeddingTest(){
        BDDMockito.given(categoryServiceImpl.extractTextForEmbedding()).willReturn(List.of("category"));
        BDDMockito.given(userServiceImpl.extractTextForEmbedding()).willReturn(List.of("user"));
        BDDMockito.given(productServiceImpl.extractTextForEmbedding()).willReturn(List.of("product"));
        BDDMockito.given(supplierServiceImpl.extractTextForEmbedding()).willReturn(List.of("supplier"));
        BDDMockito.given(transactionServiceImpl.extractTextForEmbedding()).willReturn(List.of("transaction"));
        List<String> expectedResult = ragDataExtractionService.extractAllDataForEmbedding();
        assertThat(expectedResult).isEqualTo(List.of("category","user","product","supplier","transaction"));
    }
}
