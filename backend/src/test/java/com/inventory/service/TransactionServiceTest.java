package com.inventory.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.inventory.dto.*;
import com.inventory.entity.Product;
import com.inventory.entity.Supplier;
import com.inventory.entity.Transaction;
import com.inventory.entity.User;
import com.inventory.enums.TransactionStatus;
import com.inventory.enums.TransactionType;
import com.inventory.enums.UserRole;
import com.inventory.repository.ProductRepository;
import com.inventory.repository.SupplierRepository;
import com.inventory.repository.TransactionRepository;
import com.inventory.repository.UserRepository;
import com.inventory.service.impl.TransactionServiceImpl;
import com.inventory.service.impl.UserServiceImpl;
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
import org.springframework.data.domain.*;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class TransactionServiceTest {
    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserServiceImpl userService;

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    Pageable pageable;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    @Qualifier("modelMapper")
    private ModelMapper modelMapper;

    private Transaction transaction;
    private Product product;
    private Supplier supplier;
    private User user;
    private UserDTO userDTO;
    private TransactionRequest transactionRequest;
    private List<TransactionType>  transactionTypeList;
    private TransactionStatus  transactionStatus;


    @BeforeEach
    void setUp() throws IllegalAccessException, NoSuchFieldException {
        modelMapper = new ModelMapper();
        product = modelMapper.map(ProductDTO.builder().name("Asus").sku("nl1").price(BigDecimal.valueOf(10000000)).stockQuantity(2).description("A laptop").categoryId(1L).id(1L).build(), Product.class);;
        supplier = modelMapper.map(SupplierDTO.builder().id(2L).name("NCC Thang Long").email("thanglong@yahoo.com").phone("039584728").address("Ha Noi").build(), Supplier.class);
        userDTO = UserDTO.builder().id(3L).name("Luuanz").email("luuanz@yahoo.com").password("039584728").phoneNumber("0498582").role(UserRole.STOCKSTAFF).build();
        user = modelMapper.map(userDTO, User.class);
        transactionRequest = new TransactionRequest(1L, 2, 2L, "buy a router", BigDecimal.valueOf(179000));
        transaction = modelMapper.map(Transaction.builder().id(4L).transactionType(TransactionType.SALE).status(TransactionStatus.COMPLETED).product(product).user(user).totalProducts(transactionRequest.getQuantity()).totalPrice(product.getPrice().multiply(BigDecimal.valueOf(transactionRequest.getQuantity()))).description(transactionRequest.getDescription()).build(), Transaction.class);
        // Tiem modelMapper vao transactionService (co phuong thuc ser dung mapper nen p set ko thi null excep)
        Field field = TransactionServiceImpl.class.getDeclaredField("modelMapper");
        field.setAccessible(true);
        field.set(transactionService, modelMapper);

    }

    @DisplayName("Testcase: kiem thu phuong thuc restockInventory (Nhap hang tu ncc)")
    @Test
    public void testRestockInventory() throws JsonProcessingException {
        //Given phuong thuc will return: chi dinh kq tra ve
        BDDMockito.given(supplierRepository.findById(2L)).willReturn(Optional.of(supplier));
        BDDMockito.given(productRepository.findById(1L)).willReturn(Optional.of(product));
        BDDMockito.given(userService.getCurrentLoggedInUser()).willReturn(userDTO);
        //Sau là test tổng quan - return
        Response response = transactionService.restockInventory(transactionRequest);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @DisplayName("Testcase: kiem thu phuong thuc sell (Xuat hang cho user)")
    @Test
    public void testSell() throws JsonProcessingException {
        //Given phuong thuc will return: chi dinh kq tra ve
        BDDMockito.given(productRepository.findById(1L)).willReturn(Optional.of(product));
        BDDMockito.given(userService.getCurrentLoggedInUser()).willReturn(userDTO);
        //Sau là test tổng quan - return
        Response response = transactionService.sell(transactionRequest);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @DisplayName("Testcase: kiem thu phuong thuc returnToSupplier (Tra hang ve cho nha cung cap)")
    @Test
    public void testReturnToSupplier() throws JsonProcessingException {
        //Given phuong thuc will return: chi dinh kq tra ve
        BDDMockito.given(productRepository.findById(1L)).willReturn(Optional.of(product));
        BDDMockito.given(supplierRepository.findById(2L)).willReturn(Optional.of(supplier));
        BDDMockito.given(userService.getCurrentLoggedInUser()).willReturn(userDTO);
        //Sau là test tổng quan - return
        Response response = transactionService.returnToSupplier(transactionRequest);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @DisplayName("Testcase: kiem thu phuong thuc getAll (Lay tat ca hoa don (da phan trang)")
    @Test
    public void testGetAllTransactions() {
        //May qua tim duoc cach khoi tao Page<T>
        BDDMockito.given(transactionRepository.searchTransactions(TransactionType.SALE, TransactionStatus.COMPLETED, null, -1L, -1L,null, null, PageRequest.of(1, 1, Sort.by(Sort.Direction.DESC, "id")))).willReturn(new PageImpl<>(List.of(transaction)));
        //BDDMockito.given(transactionRepository.searchTransactions(null, PageRequest.of(1, 1, Sort.by(Sort.Direction.DESC, "id")))).willReturn();
        //Test return kem transactionDTOS
        Response response = transactionService.getAllTransactions(0,2, "SALE", "COMPLETED", null, -1L, null, null);
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getTransactions().getFirst().getId()).isEqualTo(4L);
    }

    @DisplayName("Testcase: kiem thu phuong thuc getId")
    @Test
    public void testGetTransactionById() {
        BDDMockito.given(transactionRepository.findById(1L)).willReturn(Optional.of(transaction));

        //Test return kem transactionDTO
        Response response = transactionService.getTransactionById(1L);
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getTransaction().getId()).isEqualTo(4L);
    }

    @DisplayName("Testcase: kiem thu phuong thuc getAllTransactionByType (Lay theo nhap, xuat, tra hang, hoac ca 3")
    @Test
    public void testGetAllTransactionByConditions() {
        BDDMockito.given(transactionRepository.findAllByTransactionConditions(TransactionType.SALE, TransactionStatus.COMPLETED, null, null, null)).willReturn(List.of(transaction));
        List<TransactionDTO> transactionDTOS = transactionService.getAllTransactionByCondition("SALE", "COMPLETED", null, null, null);
        assertThat(transactionDTOS.getFirst().getId()).isEqualTo(4L);
    }

    @DisplayName("Testcase: kiem thu phuong thuc getAllTransactionByMonthAndYear (Lay theo thang, nam)")
    @Test
    public void getAllTransactionByMonthAndYear() {
        BDDMockito.given(transactionRepository.findAllByMonthAndYear(9, 2025)).willReturn(List.of(transaction));
        Response response = transactionService.getAllTransactionByMonthAndYear(LocalDateTime.now().getMonth().getValue(), LocalDateTime.now().getYear());
        assertThat(response.getTransactions().getFirst().getId()).isEqualTo(4L);
    }

    @DisplayName("Testcase: kiem thu phuong thuc updateTransactionStatus (Cap trang thai hoa don)")
    @Test
    public void updateTransactionStatus() throws JsonProcessingException {
        transaction.setStatus(TransactionStatus.CANCELED);
        BDDMockito.given(transactionRepository.findById(4L)).willReturn(Optional.of(transaction));
        Response response = transactionService.updateTransactionStatus(transaction.getId(), transaction.getStatus());
        assertThat(response.getStatus()).isEqualTo(200);
    }
}
