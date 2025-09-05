package com.inventory.service;

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

    @Qualifier("transactionMapper")
    private ModelMapper transactionMapper;

    private Transaction transaction;
    private Product product;
    private Supplier supplier;
    private User user;
    private TransactionRequest transactionRequest;
    private List<TransactionType>  transactionTypeList;
    private TransactionStatus  transactionStatus;


    @BeforeEach
    void setUp() throws IllegalAccessException, NoSuchFieldException {
        modelMapper = new ModelMapper();
        product = modelMapper.map(ProductDTO.builder().name("Asus").sku("nl1").price(BigDecimal.valueOf(10000000)).stockQuantity(2).description("A laptop").categoryId(1L).id(1L).build(), Product.class);;
        supplier = modelMapper.map(SupplierDTO.builder().id(2L).name("NCC Thang Long").email("thanglong@yahoo.com").phone("039584728").address("Ha Noi").build(), Supplier.class);
        user = modelMapper.map(UserDTO.builder().id(3L).name("Luuanz").email("luuanz@yahoo.com").password("039584728").phoneNumber("0498582").role(UserRole.MANAGER).build(), User.class);
        transactionRequest = new TransactionRequest(1L, 2, 2L, "buy a router");
        transaction = modelMapper.map(Transaction.builder().id(4L).transactionType(TransactionType.SALE).status(TransactionStatus.COMPLETED).product(product).user(user).totalProducts(transactionRequest.getQuantity()).totalPrice(product.getPrice().multiply(BigDecimal.valueOf(transactionRequest.getQuantity()))).description(transactionRequest.getDescription()).build(), Transaction.class);
        // Tiem modelMapper vao transactionService (co phuong thuc ser dung mapper nen p set ko thi null excep)
        Field field = TransactionServiceImpl.class.getDeclaredField("modelMapper");
        field.setAccessible(true);
        field.set(transactionService, modelMapper);

    }

    @DisplayName("Testcase: kiem thu phuong thuc restockInventory (Nhap hang tu ncc)")
    @Test
    public void testRestockInventory() {
        //Given phuong thuc will return: chi dinh kq tra ve
        BDDMockito.given(supplierRepository.findById(2L)).willReturn(Optional.of(supplier));
        BDDMockito.given(productRepository.findById(1L)).willReturn(Optional.of(product));
        BDDMockito.given(userService.getCurrentLoggedInUser()).willReturn(user);
        //Sau là test tổng quan - return
        Response response = transactionService.restockInventory(transactionRequest);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @DisplayName("Testcase: kiem thu phuong thuc sell (Xuat hang cho user)")
    @Test
    public void testSell() {
        //Given phuong thuc will return: chi dinh kq tra ve
        BDDMockito.given(productRepository.findById(1L)).willReturn(Optional.of(product));
        BDDMockito.given(userService.getCurrentLoggedInUser()).willReturn(user);
        //Sau là test tổng quan - return
        Response response = transactionService.sell(transactionRequest);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @DisplayName("Testcase: kiem thu phuong thuc returnToSupplier (Tra hang ve cho nha cung cap)")
    @Test
    public void testReturnToSupplier() {
        //Given phuong thuc will return: chi dinh kq tra ve
        BDDMockito.given(productRepository.findById(1L)).willReturn(Optional.of(product));
        BDDMockito.given(supplierRepository.findById(2L)).willReturn(Optional.of(supplier));
        BDDMockito.given(userService.getCurrentLoggedInUser()).willReturn(user);
        //Sau là test tổng quan - return
        Response response = transactionService.returnToSupplier(transactionRequest);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @DisplayName("Testcase: kiem thu phuong thuc getAll (Lay tat ca hoa don (da phan trang)")
    @Test
    public void testGetAllTransactions() {
        //May qua tim duoc cach khoi tao Page<T>
        BDDMockito.given(transactionRepository.searchTransactions(null, PageRequest.of(1, 1, Sort.by(Sort.Direction.DESC, "id")))).willReturn(new PageImpl<>(List.of(transaction)));
        //BDDMockito.given(transactionRepository.searchTransactions(null, PageRequest.of(1, 1, Sort.by(Sort.Direction.DESC, "id")))).willReturn();
        //Test return kem transactionDTOS
        Response response = transactionService.getAllTransactions(1,1,null);
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
    public void testGetAllTransactionByType() {
        BDDMockito.given(transactionRepository.findAllByTransactionTypes(List.of(TransactionType.SALE))).willReturn(List.of(transaction));
        List<TransactionDTO> transactionDTOS = transactionService.getAllTransactionByType(List.of(TransactionType.SALE));
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
    public void updateTransactionStatus() {
        transaction.setStatus(TransactionStatus.CANCELED);
        BDDMockito.given(transactionRepository.findById(4L)).willReturn(Optional.of(transaction));
        Response response = transactionService.updateTransactionStatus(transaction.getId(), transaction.getStatus());
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @DisplayName("Testcase: kiem thu phuong thuc extract")
    @Test
    public void extractTextForEmbedding() throws NoSuchFieldException, IllegalAccessException {
        Field field = TransactionServiceImpl.class.getDeclaredField("transactionMapper");
        field.setAccessible(true);
        field.set(transactionService, modelMapper);
        BDDMockito.given(transactionRepository.findAll(Sort.by(Sort.Direction.DESC, "id"))).willReturn(List.of(transaction));
        List<String> res = transactionService.extractTextForEmbedding();
        assertThat(res.size()).isEqualTo(1);
    }
}
