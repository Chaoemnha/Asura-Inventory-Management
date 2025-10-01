package com.inventory.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.dto.*;
import com.inventory.entity.Product;
import com.inventory.entity.Supplier;
import com.inventory.entity.Transaction;
import com.inventory.entity.User;
import com.inventory.enums.TransactionStatus;
import com.inventory.enums.TransactionType;
import com.inventory.enums.UserRole;
import com.inventory.exceptions.NameValueRequiredException;
import com.inventory.exceptions.NotFoundException;
import com.inventory.repository.ProductRepository;
import com.inventory.repository.SupplierRepository;
import com.inventory.repository.TransactionRepository;
import com.inventory.service.DocxTemplateService;
import com.inventory.service.TransactionService;
import com.inventory.service.UserService;
import com.inventory.utils.WebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
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
    private final DocxTemplateService docxTemplateService;

    @Value("${app.qr.base-url:http://localhost:8080/api/transactions/{transaction_id}/{status}}")
    private String qrBaseUrl;

    @Override
    public Response restockInventory(TransactionRequest transactionRequest)  throws JsonProcessingException {

        Long productId = transactionRequest.getProductId();
        Long supplierId = transactionRequest.getSupplierId();
        Integer quantity = transactionRequest.getQuantity();

        if (supplierId == null) throw new NameValueRequiredException("Supplier Id id Required");

        Product product = productRepository.findById(productId)
                .orElseThrow(()-> new NotFoundException("Product Not Found"));

        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(()-> new NotFoundException("Supplier Not Found"));

        UserDTO user1 = userService.getCurrentLoggedInUser();
        User user = modelMapper.map(user1, User.class);
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

        Transaction result = transactionRepository.save(transaction);
        Map<String, Object> message = new HashMap<>();
        message.put("type", "TRANSACTION_PURCHASED_RENDER");
        message.put("data", Map.of(
                "id", result.getId().toString(),
                "transactionType", result.getTransactionType().toString(),
                "status", result.getStatus().toString(),
                "totalPrice", result.getTotalPrice().toString(),
                "totalProducts", result.getTotalProducts().toString(),
                "createdAt", result.getCreatedAt().toString()
        ));

        ObjectMapper mapper = new ObjectMapper();
        String jsonString = mapper.writeValueAsString(message);
        WebSocketHandler.broadcastChanges(jsonString);
        return Response.builder()
                .status(200)
                .message("Transaction Made Successfully")
                .build();
    }

    @Override
    public Response sell(TransactionRequest transactionRequest) throws JsonProcessingException{

        Long productId = transactionRequest.getProductId();
        Integer quantity = transactionRequest.getQuantity();


        Product product = productRepository.findById(productId)
                .orElseThrow(()-> new NotFoundException("Product Not Found"));

        UserDTO user1 = userService.getCurrentLoggedInUser();
        User user = modelMapper.map(user1, User.class);
        //update the stock quantity and re-save
        product.setStockQuantity(product.getStockQuantity() - quantity);
        productRepository.save(product);

        //create a transaction
        Transaction transaction = Transaction.builder()
                .transactionType(TransactionType.SALE)
                .status(TransactionStatus.PENDING)
                .product(product)
                .user(user)
                .totalProducts(quantity)
                .totalPrice(product.getPrice().multiply(BigDecimal.valueOf(quantity)))
                .description(transactionRequest.getDescription())
                .build();

        Transaction result = transactionRepository.save(transaction);
        Map<String, Object> message = new HashMap<>();
        message.put("type", "TRANSACTION_SOLD_RENDER");
        message.put("data", Map.of(
                "id", result.getId().toString(),
                "transactionType", result.getTransactionType().toString(),
                "status", result.getStatus().toString(),
                "totalPrice", result.getTotalPrice().toString(),
                "totalProducts", result.getTotalProducts().toString(),
                "createdAt", result.getCreatedAt().toString()
        ));

        ObjectMapper mapper = new ObjectMapper();
        String jsonString = mapper.writeValueAsString(message);
        WebSocketHandler.broadcastChanges(jsonString);

        return Response.builder()
                .status(200)
                .message("Transaction Sold Successfully")
                .build();
    }

    @Override
    public Response returnToSupplier(TransactionRequest transactionRequest) throws JsonProcessingException {

        Long productId = transactionRequest.getProductId();
        Long supplierId = transactionRequest.getSupplierId();
        Integer quantity = transactionRequest.getQuantity();

        if (supplierId == null) throw new NameValueRequiredException("Supplier Id id Required");

        Product product = productRepository.findById(productId)
                .orElseThrow(()-> new NotFoundException("Product Not Found"));

        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(()-> new NotFoundException("Supplier Not Found"));

        UserDTO user1 = userService.getCurrentLoggedInUser();
        User user = modelMapper.map(user1, User.class);
        //update the stock quantity and re-save
        product.setStockQuantity(product.getStockQuantity() - quantity);
        productRepository.save(product);

        //create a transaction
        Transaction transaction = Transaction.builder()
                .transactionType(TransactionType.RETURN_TO_SUPPLIER)
                .status(TransactionStatus.PENDING)
                .product(product)
                .user(user)
                .supplier(supplier)
                .totalProducts(quantity)
                .totalPrice(BigDecimal.ZERO)
                .description(transactionRequest.getDescription())
                .build();

        Transaction result = transactionRepository.save(transaction);
        Map<String, Object> message = new HashMap<>();
        message.put("type", "TRANSACTION_RETURNED_RENDER");
        message.put("data", Map.of(
                "id", result.getId().toString(),
                "transactionType", result.getTransactionType().toString(),
                "status", result.getStatus().toString(),
                "totalPrice", result.getTotalPrice().toString(),
                "totalProducts", result.getTotalProducts().toString(),
                "createdAt", result.getCreatedAt().toString()
        ));

        ObjectMapper mapper = new ObjectMapper();
        String jsonString = mapper.writeValueAsString(message);
        WebSocketHandler.broadcastChanges(jsonString);

        return Response.builder()
                .status(200)
                .message("Transaction Returned Successfully Initialized")
                .build();
    }

    @Override
    public Response getAllTransactions(int page, int size, String searchText, Long userId, Long supplierId) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        if(userId==-1) userId=null;
        if(supplierId==-1) supplierId=null;
        Page<Transaction> transactionPage = transactionRepository.searchTransactions(searchText, userId, supplierId, pageable);
        List<TransactionDTO> transactionDTOS = modelMapper
                .map(transactionPage.getContent(), new TypeToken<List<TransactionDTO>>() {}.getType());
        transactionDTOS.forEach(transactionDTOItem -> {
            transactionDTOItem.getUser().setTransactions(null);
            transactionDTOItem.getUser().setSupplier(null);
            transactionDTOItem.setProduct(null);
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
    public Response updateTransactionStatus(Long transactionId, TransactionStatus transactionStatus)  throws JsonProcessingException{

        Transaction existingTransaction = transactionRepository.findById(transactionId)
                .orElseThrow(()-> new NotFoundException("Transaction Not Found"));

        existingTransaction.setStatus(transactionStatus);
        existingTransaction.setUpdatedAt(LocalDateTime.now());

        Transaction result = transactionRepository.save(existingTransaction);
        Map<String, Object> message = new HashMap<>();
        message.put("type", "TRANSACTION_UPDATED_RENDER");
        message.put("data", Map.of(
                "id", result.getId().toString(),
                "transactionType", result.getTransactionType().toString(),
                "status", result.getStatus().toString(),
                "totalPrice", result.getTotalPrice().toString(),
                "totalProducts", result.getTotalProducts().toString(),
                "createdAt", result.getCreatedAt().toString()
        ));

        ObjectMapper mapper = new ObjectMapper();
        String jsonString = mapper.writeValueAsString(message);
        WebSocketHandler.broadcastChanges(jsonString);

        return Response.builder()
                .status(200)
                .message("Transaction Status Successfully Updated")
                .build();
    }

    @Override
    public Response updateTransactionStatusViaQR(Long transactionId, TransactionStatus status)  throws JsonProcessingException{
        Transaction existingTransaction = transactionRepository.findById(transactionId)
                .orElseThrow(()-> new NotFoundException("Transaction Not Found"));
        UserDTO user = userService.getCurrentLoggedInUser();
        if(existingTransaction.getStatus() == TransactionStatus.PENDING) {
            if(existingTransaction.getUser()!=null&&existingTransaction.getUser().getRole()!= UserRole.STOCKSTAFF) {
                if(existingTransaction.getUser().getId()==user.getId()) {
                    existingTransaction.setStatus(status);
                    existingTransaction.setUpdatedAt(LocalDateTime.now());
                }
            }
            else if(existingTransaction.getUser()!=null){
                existingTransaction.setStatus(status);
                existingTransaction.setUpdatedAt(LocalDateTime.now());

            }
            else return Response.builder().status(401).message("User account or transaction error").build();
        }

        Transaction result = transactionRepository.save(existingTransaction);
        Map<String, Object> message = new HashMap<>();
        message.put("type", "TRANSACTION_UPDATED_RENDER");
        message.put("data", Map.of(
                "id", result.getId().toString(),
                "transactionType", result.getTransactionType().toString(),
                "status", result.getStatus().toString(),
                "totalPrice", result.getTotalPrice().toString(),
                "totalProducts", result.getTotalProducts().toString(),
                "createdAt", result.getCreatedAt().toString()
        ));

        ObjectMapper mapper = new ObjectMapper();
        String jsonString = mapper.writeValueAsString(message);
        WebSocketHandler.broadcastChanges(jsonString);
        return Response.builder()
                .status(200)
                .message("Transaction Status Successfully Updated")
                .build();
    }

    @Override
    public File generateInvoicePdfWithQR(Long transactionId, TransactionStatus transactionStatus ) throws Exception {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new NotFoundException("Transaction Not Found"));

        // 3. Chuẩn bị dữ liệu cho template
        Map<String, String> textValues = new HashMap<>();
        TransactionType transactionType = transaction.getTransactionType();
        if(transactionType.equals(TransactionType.PURCHASE)||transactionType.equals(TransactionType.RETURN_TO_SUPPLIER)){
            textValues.put("supplier_name", String.valueOf(transaction.getSupplier().getName()));
            textValues.put("supplier_address", String.valueOf(transaction.getSupplier().getAddress()));
            textValues.put("supplier_phone", String.valueOf(transaction.getSupplier().getPhone()));
            textValues.put("supplier_email", String.valueOf(transaction.getSupplier().getEmail()));
            textValues.put("user_phoneNumber", transaction.getUser().getPhoneNumber());
            textValues.put("user_name", transaction.getUser().getName());
        }
        else if(transactionType.equals(TransactionType.SALE)){
            textValues.put("user_name", String.valueOf(transaction.getSupplier().getName()));
            textValues.put("user_address", transaction.getSupplier().getAddress());
            textValues.put("user_phoneNumber", transaction.getSupplier().getPhone());
            textValues.put("user_email", transaction.getSupplier().getEmail());
        }
        textValues.put("update_status", TransactionStatus.COMPLETED.toString());
        textValues.put("transaction_id", String.valueOf(transaction.getId()));
        textValues.put("transaction_totalProducts", String.valueOf(transaction.getTotalProducts()));
        textValues.put("transaction_description", transaction.getDescription());
        textValues.put("transaction_createdAt", transaction.getCreatedAt().toString());
        textValues.put("transaction_updatedAt", transaction.getUpdatedAt() != null ? transaction.getUpdatedAt().toString() : "");
        textValues.put("product_name", transaction.getProduct().getName());
        textValues.put("product_sku", transaction.getProduct().getSku());
        textValues.put("transaction_totalPrice", transaction.getTotalPrice().toString());
        textValues.put("product_price", transaction.getProduct().getPrice().toString());
        textValues.put("product_description", transaction.getProduct().getDescription());
        // 4. Sinh PDF từ template
        if(transaction.getTransactionType()==TransactionType.PURCHASE)
            return docxTemplateService.generatePdf("purchase.docx", textValues, Collections.emptyMap());
        if(transaction.getTransactionType()==TransactionType.SALE)
            return docxTemplateService.generatePdf("sale.docx", textValues, Collections.emptyMap());
        if(transaction.getTransactionType()==TransactionType.RETURN_TO_SUPPLIER)
            return docxTemplateService.generatePdf("return_to_supplier.docx", textValues, Collections.emptyMap());
        throw new NotFoundException("Unsupported transaction type: " + transaction.getTransactionType());    }

}
