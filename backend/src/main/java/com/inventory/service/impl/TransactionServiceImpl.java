package com.inventory.service.impl;

import com.inventory.config.TransactionMapperConfig;
import com.inventory.dto.CategoryDTO;
import com.inventory.dto.Response;
import com.inventory.dto.TransactionDTO;
import com.inventory.dto.TransactionRequest;
import com.inventory.entity.Product;
import com.inventory.entity.Supplier;
import com.inventory.entity.Transaction;
import com.inventory.entity.User;
import com.inventory.enums.TransactionStatus;
import com.inventory.enums.TransactionType;
import com.inventory.exceptions.NameValueRequiredException;
import com.inventory.exceptions.NotFoundException;
import com.inventory.repository.ProductRepository;
import com.inventory.repository.SupplierRepository;
import com.inventory.repository.TransactionRepository;
import com.inventory.service.TransactionService;
import com.inventory.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    @Autowired
    @Qualifier("modelMapper")
    private final ModelMapper modelMapper;
    private final SupplierRepository supplierRepository;
    private final UserService userService;
    private final ProductRepository productRepository;
    @Autowired
    @Qualifier("transactionMapper")
    private final ModelMapper transactionMapper;

    @Override
    public Response restockInventory(TransactionRequest transactionRequest) {

        Long productId = transactionRequest.getProductId();
        Long supplierId = transactionRequest.getSupplierId();
        Integer quantity = transactionRequest.getQuantity();

        if (supplierId == null) throw new NameValueRequiredException("Supplier Id id Required");

        Product product = productRepository.findById(productId)
                .orElseThrow(()-> new NotFoundException("Product Not Found"));

        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(()-> new NotFoundException("Supplier Not Found"));

        User user = userService.getCurrentLoggedInUser();

        //update the stock quantity and re-save
        product.setStockQuantity(product.getStockQuantity() + quantity);
        productRepository.save(product);

        //create a transaction
        Transaction transaction = Transaction.builder()
                .transactionType(TransactionType.PURCHASE)
                .status(TransactionStatus.PENDING)
                .product(product)
                .user(user)
                .supplier(supplier)
                .totalProducts(quantity)
                .totalPrice(product.getPrice().multiply(BigDecimal.valueOf(quantity)))
                .description(transactionRequest.getDescription())
                .build();

        transactionRepository.save(transaction);

        return Response.builder()
                .status(200)
                .message("Transaction Made Successfully")
                .build();
    }

    @Override
    public Response sell(TransactionRequest transactionRequest) {

        Long productId = transactionRequest.getProductId();
        Integer quantity = transactionRequest.getQuantity();


        Product product = productRepository.findById(productId)
                .orElseThrow(()-> new NotFoundException("Product Not Found"));

        User user = userService.getCurrentLoggedInUser();

        //update the stock quantity and re-save
        product.setStockQuantity(product.getStockQuantity() - quantity);
        productRepository.save(product);

        //create a transaction
        Transaction transaction = Transaction.builder()
                .transactionType(TransactionType.SALE)
                .status(TransactionStatus.COMPLETED)
                .product(product)
                .user(user)
                .totalProducts(quantity)
                .totalPrice(product.getPrice().multiply(BigDecimal.valueOf(quantity)))
                .description(transactionRequest.getDescription())
                .build();

        transactionRepository.save(transaction);

        return Response.builder()
                .status(200)
                .message("Transaction Sold Successfully")
                .build();
    }

    @Override
    public Response returnToSupplier(TransactionRequest transactionRequest) {

        Long productId = transactionRequest.getProductId();
        Long supplierId = transactionRequest.getSupplierId();
        Integer quantity = transactionRequest.getQuantity();

        if (supplierId == null) throw new NameValueRequiredException("Supplier Id id Required");

        Product product = productRepository.findById(productId)
                .orElseThrow(()-> new NotFoundException("Product Not Found"));

        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(()-> new NotFoundException("Supplier Not Found"));

        User user = userService.getCurrentLoggedInUser();

        //update the stock quantity and re-save
        product.setStockQuantity(product.getStockQuantity() - quantity);
        productRepository.save(product);

        //create a transaction
        Transaction transaction = Transaction.builder()
                .transactionType(TransactionType.RETURN_TO_SUPPLIER)
                .status(TransactionStatus.PROCESSING)
                .product(product)
                .user(user)
                .supplier(supplier)
                .totalProducts(quantity)
                .totalPrice(BigDecimal.ZERO)
                .description(transactionRequest.getDescription())
                .build();

        transactionRepository.save(transaction);

        return Response.builder()
                .status(200)
                .message("Transaction Returned Successfully Initialized")
                .build();
    }

    @Override
    public Response getAllTransactions(int page, int size, String searchText) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<Transaction> transactionPage = transactionRepository.searchTransactions(searchText, pageable);
        List<TransactionDTO> transactionDTOS = modelMapper
                .map(transactionPage.getContent(), new TypeToken<List<TransactionDTO>>() {}.getType());
        transactionDTOS.forEach(transactionDTOItem -> {
            transactionDTOItem.setUser(null);
            transactionDTOItem.setProduct(null);
            transactionDTOItem.setSupplier(null);
        });
        return Response.builder()
                .status(200)
                .message("success")
                .transactions(transactionDTOS)
                .build();
    }

    @Override
    public Response getTransactionById(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(()-> new NotFoundException("Transaction Not Found"));

        TransactionDTO transactionDTO = modelMapper.map(transaction, TransactionDTO.class);
        transactionDTO.getUser().setTransactions(null);
        return Response.builder()
                .status(200)
                .message("success")
                .transaction(transactionDTO)
                .build();
    }

    @Override
    public List<TransactionDTO> getAllTransactionByType(List<TransactionType> transactionTypes) {
        List<Transaction> transactions = transactionRepository.findAllByTransactionTypes(transactionTypes);

        List<TransactionDTO> transactionDTOS = modelMapper
                .map(transactions, new TypeToken<List<TransactionDTO>>() {}.getType());
        return transactionDTOS;
    }


    @Override
    public Response getAllTransactionByMonthAndYear(int month, int year) {

       List<Transaction> transactions = transactionRepository.findAllByMonthAndYear(month, year);

        List<TransactionDTO> transactionDTOS = modelMapper
                .map(transactions, new TypeToken<List<TransactionDTO>>() {}.getType());
        transactionDTOS.forEach(transactionDTOItem -> {
            transactionDTOItem.setUser(null);
            transactionDTOItem.setProduct(null);
            transactionDTOItem.setSupplier(null);
        });

        return Response.builder()
                .status(200)
                .message("success")
                .transactions(transactionDTOS)
                .build();
    }

    @Override
    public Response updateTransactionStatus(Long transactionId, TransactionStatus transactionStatus) {

        Transaction existingTransaction = transactionRepository.findById(transactionId)
                .orElseThrow(()-> new NotFoundException("Transaction Not Found"));

        existingTransaction.setStatus(transactionStatus);
        existingTransaction.setUpdatedAt(LocalDateTime.now());

        transactionRepository.save(existingTransaction);

        return Response.builder()
                .status(200)
                .message("Transaction Status Successfully Updated")
                .build();
    }

    @Override
    public List<String> extractTextForEmbedding() {
        List<Transaction> transactions = transactionRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
        List<TransactionDTO> transactionDTOS = transactionMapper.map(transactions, new TypeToken<List<TransactionDTO>>() {}.getType());
        return transactionDTOS.stream().map(TransactionDTO::getTextForEmbedding).collect(Collectors.toList());
    }
}
