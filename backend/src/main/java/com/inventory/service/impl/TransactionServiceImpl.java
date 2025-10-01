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
import com.inventory.exceptions.InvalidCredentialsException;
import com.inventory.exceptions.NameValueRequiredException;
import com.inventory.exceptions.NotFoundException;
import com.inventory.repository.ProductRepository;
import com.inventory.repository.SupplierRepository;
import com.inventory.repository.TransactionRepository;
import com.inventory.repository.UserRepository;
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
    @Autowired
    private UserRepository userRepository;

    @Override
    public Response restockInventory(TransactionRequest transactionRequest) throws JsonProcessingException {

        Long productId = transactionRequest.getProductId();
        Long supplierId = transactionRequest.getSupplierId();
        Integer quantity = transactionRequest.getQuantity();
        TransactionStatus status = null;
        User sender = null;
        LocalDateTime updatedAt = null;
        LocalDateTime createdAt = null;
        if(transactionRequest.getStatus() !=null) {
            status = transactionRequest.getStatus();
        }
        if(transactionRequest.getSenderId()!=null) {
            sender = userRepository.findById(transactionRequest.getSenderId()).orElseThrow(() -> new NotFoundException("Sender Not Found"));
        }
        if(transactionRequest.getUpdatedAt()!=null) {
            updatedAt = transactionRequest.getUpdatedAt();
        }
        if(transactionRequest.getCreatedAt()!=null) {
            createdAt = transactionRequest.getCreatedAt();
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy sản phẩm"));

        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy nhà cung cấp"));

        UserDTO user1 = userService.getCurrentLoggedInUser();
        User user = modelMapper.map(user1, User.class);

        Transaction transaction = Transaction.builder()
                .transactionType(TransactionType.PURCHASE)
                .product(product)
                .user(user)
                .supplier(supplier)
                .totalProducts(quantity)
                .totalPrice(transactionRequest.getPrice().multiply(BigDecimal.valueOf(quantity)))
                .status(status!=null?status:TransactionStatus.SENDER_DECIDING)
                .description(transactionRequest.getDescription())
                .sender(sender)
                .updatedAt(updatedAt)
                .createdAt(createdAt != null ? createdAt : LocalDateTime.now())
                .build();

        Transaction result = transactionRepository.save(transaction);
        Map<String, Object> message = new HashMap<>();
        message.put("type", "TRANSACTION_PURCHASED_RENDER");
        message.put("data", Map.of(
                "id", result.getId(),
                "product",Product.builder().name(result.getProduct().getName()).build(),
                "transactionType", result.getTransactionType().toString(),
                "status", result.getStatus().toString(),
                "totalPrice", result.getTotalPrice(),
                "totalProducts", result.getTotalProducts(),
                "createdAt", result.getCreatedAt().toString()
        ));

        ObjectMapper mapper = new ObjectMapper();
        String jsonString = mapper.writeValueAsString(message);
        WebSocketHandler.broadcastChanges(jsonString);
        return Response.builder()
                .status(200)
                .message("Tạo giao dịch thành công")
                .build();
    }

    @Override
    public Response sell(TransactionRequest transactionRequest) throws JsonProcessingException {

        Long productId = transactionRequest.getProductId();
        Integer quantity = transactionRequest.getQuantity();
        UserDTO user1 = userService.getCurrentLoggedInUser();
        TransactionStatus status = null;
        User sender = null;
        LocalDateTime updatedAt = null;
        LocalDateTime createdAt = null;
        if(transactionRequest.getStatus() !=null) {
            status = transactionRequest.getStatus();
        }
        if(transactionRequest.getSenderId()!=null) {
            sender = userRepository.findById(transactionRequest.getSenderId()).orElseThrow(() -> new NotFoundException("Sender Not Found"));
        }
        if(transactionRequest.getUpdatedAt()!=null) {
            updatedAt = transactionRequest.getUpdatedAt();
        }
        if(transactionRequest.getCreatedAt()!=null) {
            createdAt = transactionRequest.getCreatedAt();
        }
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy sản phẩm"));
        if(product.getStockQuantity()<1) throw new NotFoundException("Sản phẩm đã hết hàng");
        Supplier supplier = supplierRepository.findById(7L)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy nhà cung cấp"));
        if(quantity>product.getStockQuantity()||quantity<1) throw  new InvalidCredentialsException("Số lượng phải nằm trong khoảng 1 đến "+product.getStockQuantity());
        productRepository.save(product);
        Transaction transaction = new Transaction();
        //Kiem tra la khach hang de quyet dinh nguoi gui
        if (user1.getRole() == UserRole.CUSTOMER) {
            transaction = Transaction.builder()
                    .transactionType(TransactionType.SALE)
                    .status(TransactionStatus.SENDER_DECIDING)
                    .product(product)
                    .supplier(supplier)
                    .user(modelMapper.map(user1, User.class))
                    .totalProducts(quantity)
                    .totalPrice(product.getPrice().multiply(BigDecimal.valueOf(quantity)))
                    .description(transactionRequest.getDescription())
                    .build();
        }
        //Nguoc lai set nguoi gui luon
        else {
            transaction = Transaction.builder()
                    .transactionType(TransactionType.SALE)
                    .product(product)
                    .supplier(supplier)
                    .sender(modelMapper.map(user1, User.class))
                    .user(modelMapper.map(userService.getUserById(transactionRequest.getUserId()), User.class))
                    .totalProducts(quantity)
                    .totalPrice(product.getPrice().multiply(BigDecimal.valueOf(quantity)))
                    .description(transactionRequest.getDescription())
                    .status(status!=null?status:TransactionStatus.ADMIN_DECIDING)
                    .description(transactionRequest.getDescription())
                    .sender(sender)
                    .updatedAt(updatedAt)
                    .createdAt(createdAt != null ? createdAt : LocalDateTime.now())
                    .build();
        }

        Transaction result = transactionRepository.save(transaction);
        Map<String, Object> message = new HashMap<>();
        message.put("type", "TRANSACTION_SOLD_RENDER");
        message.put("data", Map.of(
                "id", result.getId(),
                "product",Product.builder().name(result.getProduct().getName()).build(),
                "transactionType", result.getTransactionType().toString(),
                "status", result.getStatus().toString(),
                "totalPrice", result.getTotalPrice(),
                "totalProducts", result.getTotalProducts(),
                "createdAt", result.getCreatedAt().toString()
        ));

        ObjectMapper mapper = new ObjectMapper();
        String jsonString = mapper.writeValueAsString(message);
        WebSocketHandler.broadcastChanges(jsonString);

        return Response.builder()
                .status(200)
                .message("Tạo giao dịch thành công")
                .build();
    }

    @Override
    public Response returnToSupplier(TransactionRequest transactionRequest) throws JsonProcessingException {

        Long productId = transactionRequest.getProductId();
        Long supplierId = transactionRequest.getSupplierId();
        Integer quantity = transactionRequest.getQuantity();
        TransactionStatus status = null;
        User sender = null;
        LocalDateTime updatedAt = null;
        LocalDateTime createdAt = null;
        if(transactionRequest.getStatus() !=null) {
            status = transactionRequest.getStatus();
        }
        if(transactionRequest.getSenderId()!=null) {
            sender = userRepository.findById(transactionRequest.getSenderId()).orElseThrow(() -> new NotFoundException("Sender Not Found"));
        }
        if(transactionRequest.getUpdatedAt()!=null) {
            updatedAt = transactionRequest.getUpdatedAt();
        }
        if(transactionRequest.getCreatedAt()!=null) {
            createdAt = transactionRequest.getCreatedAt();
        }

        if (supplierId == null) throw new NameValueRequiredException("Hãy nhập id nhà cung cấp");

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy sản phẩm"));

        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy nhà cung cấp"));

        UserDTO user1 = userService.getCurrentLoggedInUser();
        User user = modelMapper.map(user1, User.class);
        //update the stock quantity and re-save

        //create a transaction
        Transaction transaction = Transaction.builder()
                .transactionType(TransactionType.RETURN_TO_SUPPLIER)
                .product(product)
                .user(user)
                .supplier(supplier)
                .totalProducts(quantity)
                .totalPrice(transactionRequest.getPrice().multiply(BigDecimal.valueOf(quantity)))
                .description(transactionRequest.getDescription())
                .status(status!=null?status:TransactionStatus.SENDER_DECIDING)
                .description(transactionRequest.getDescription())
                .sender(sender)
                .updatedAt(updatedAt)
                .createdAt(createdAt != null ? createdAt : LocalDateTime.now())
                .build();

        Transaction result = transactionRepository.save(transaction);
        Map<String, Object> message = new HashMap<>();
        message.put("type", "TRANSACTION_RETURNED_RENDER");
        message.put("data", Map.of(
                "id", result.getId(),
                "product",Product.builder().name(result.getProduct().getName()).build(),
                "transactionType", result.getTransactionType().toString(),
                "status", result.getStatus().toString(),
                "totalPrice", result.getTotalPrice(),
                "totalProducts", result.getTotalProducts(),
                "createdAt", result.getCreatedAt().toString()
        ));

        ObjectMapper mapper = new ObjectMapper();
        String jsonString = mapper.writeValueAsString(message);
        WebSocketHandler.broadcastChanges(jsonString);

        return Response.builder()
                .status(200)
                .message("Tạo giao dịch hoàn thành công")
                .build();
    }

    @Override
    public Response getAllTransactions(int page, int size, String searchType, String searchStatus,
                                       String searchProductName, Long userId,
                                       String searchFromDate, String searchToDate) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Long supplierId = -1L;
        // Handle -1 values as null
        if (userId != null && userId == -1) {
            userId = null;
        }
        else{
            UserDTO user = userService.getUserById(userId);
            if (user == null) throw new NotFoundException("Không tìm thấy người dùng");
            if (user.getRole() == UserRole.STOCKSTAFF||user.getRole() == UserRole.ADMIN||user.getRole() == UserRole.SUPPLIER)
                supplierId = user.getSupplier().getId();
        }

        // Parse enum values
        TransactionType transactionType = null;
        if (searchType != null && !searchType.isEmpty()) {
            try {
                transactionType = TransactionType.valueOf(searchType.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Invalid enum value, keep as null
            }
        }

        TransactionStatus status = null;
        if (searchStatus != null && !searchStatus.isEmpty()) {
            try {
                status = TransactionStatus.valueOf(searchStatus.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Invalid enum value, keep as null
            }
        }

        // Parse date strings to LocalDateTime
        LocalDateTime fromDate = null;
        LocalDateTime toDate = null;

        if (searchFromDate != null && !searchFromDate.isEmpty()) {
            try {
                fromDate = LocalDateTime.parse(searchFromDate);
            } catch (Exception e) {
                // Invalid date format, keep as null
                log.warn("Định dạng fromDate không hợp lệ: " + searchFromDate);
            }
        }

        if (searchToDate != null && !searchToDate.isEmpty()) {
            try {
                toDate = LocalDateTime.parse(searchToDate);
            } catch (Exception e) {
                // Invalid date format, keep as null
                log.warn("Định dạng toDate không hợp lệ: " + searchToDate);
            }
        }

        Page<Transaction> transactionPage = transactionRepository.searchTransactions(transactionType, status, searchProductName, userId, supplierId, fromDate, toDate, pageable);

        List<TransactionDTO> transactionDTOS = modelMapper
                .map(transactionPage.getContent(), new TypeToken<List<TransactionDTO>>() {
                }.getType());

        transactionDTOS.forEach(transactionDTOItem -> {
            if (transactionDTOItem.getUser() != null) {
                transactionDTOItem.getUser().setTransactions(null);
                transactionDTOItem.getUser().setSupplier(null);
            }
            if (transactionDTOItem.getSender() != null) {
                transactionDTOItem.getSender().setSupplier(null);
                transactionDTOItem.getSender().setTransactions(null);
            }
            ProductDTO productDTO = ProductDTO.builder().name(transactionDTOItem.getProduct().getName()).build();
            transactionDTOItem.setProduct(productDTO);
        });

        return Response.builder()
                .status(200)
                .message("success")
                .transactions(transactionDTOS)
                .totalElements(transactionPage.getTotalElements())
                .totalPages(transactionPage.getTotalPages())
                .currentPage(page)
                .build();
    }

    @Override
    public Response getTransactionById(Long id) {
        UserDTO user = userService.getCurrentLoggedInUser();
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy giao dịch"));
        if(user.getRole()!=UserRole.ADMIN&&(transaction.getTransactionType()!=TransactionType.RETURN_TO_SUPPLIER&&transaction.getStatus()==TransactionStatus.SENDER_DECIDING&&transaction.getSupplier().getId()!=user.getSupplier().getId())&&(transaction.getSender().getId()!=user.getId()&&transaction.getUser().getId()!=user.getId()))
            throw new NotFoundException("Giao dịch này không phận sự đến bạn");
        TransactionDTO transactionDTO = modelMapper.map(transaction, TransactionDTO.class);
        transactionDTO.getUser().setTransactions(null);
        if (transactionDTO.getSender() != null) {
            transactionDTO.getSender().setSupplier(null);
            transactionDTO.getSender().setTransactions(null);
        }
        return Response.builder()
                .status(200)
                .message("success")
                .transaction(transactionDTO)
                .build();
    }

    @Override
    public List<TransactionDTO> getAllTransactionByCondition(String searchType, String searchStatus, String searchProductName, String searchFromDate, String searchToDate) {
        TransactionType transactionType = null;
        if (searchType != null && !searchType.isEmpty()) {
            try {
                transactionType = TransactionType.valueOf(searchType.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Invalid enum value, keep as null
            }
        }

        TransactionStatus status = null;
        if (searchStatus != null && !searchStatus.isEmpty()) {
            try {
                status = TransactionStatus.valueOf(searchStatus.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Invalid enum value, keep as null
            }
        }

        // Parse date strings to LocalDateTime
        LocalDateTime fromDate = null;
        LocalDateTime toDate = null;

        if (searchFromDate != null && !searchFromDate.isEmpty()) {
            try {
                fromDate = LocalDateTime.parse(searchFromDate);
            } catch (Exception e) {
                // Invalid date format, keep as null
                log.warn("Định dạng fromDate không hợp lệ: " + searchFromDate);
            }
        }

        if (searchToDate != null && !searchToDate.isEmpty()) {
            try {
                toDate = LocalDateTime.parse(searchToDate);
            } catch (Exception e) {
                // Invalid date format, keep as null
                log.warn("Định dạng toDate không hợp lệ: " + searchToDate);
            }
        }
        List<Transaction> transactions = transactionRepository.findAllByTransactionConditions(transactionType, status, searchProductName, fromDate, toDate);
        List<TransactionDTO> transactionDTOS = modelMapper
                .map(transactions, new TypeToken<List<TransactionDTO>>() {
                }.getType());
        transactionDTOS.forEach(transactionDTOItem -> {
            if (transactionDTOItem.getUser() != null) {
                transactionDTOItem.getUser().setTransactions(null);
                transactionDTOItem.getUser().setSupplier(null);
            }
            if (transactionDTOItem.getSender() != null) {
                transactionDTOItem.getSender().setSupplier(null);
                transactionDTOItem.getSender().setTransactions(null);
            }
            UserDTO userDTO = UserDTO.builder().name(transactionDTOItem.getUser().getName()).build();
            SupplierDTO supplierDTO = SupplierDTO.builder().name(transactionDTOItem.getSupplier().getName()).build();
            ProductDTO productDTO = ProductDTO.builder().name(transactionDTOItem.getProduct().getName()).build();
            transactionDTOItem.setProduct(productDTO);
            transactionDTOItem.setUser(userDTO);
            transactionDTOItem.setSupplier(supplierDTO);
        });
        return transactionDTOS;
    }


    @Override
    public Response getAllTransactionByMonthAndYear(int month, int year) {

        List<Transaction> transactions = transactionRepository.findAllByMonthAndYear(month, year);

        List<TransactionDTO> transactionDTOS = modelMapper
                .map(transactions, new TypeToken<List<TransactionDTO>>() {
                }.getType());
        transactionDTOS.forEach(transactionDTOItem -> {
            transactionDTOItem.setUser(null);
            transactionDTOItem.setProduct(null);
            transactionDTOItem.setSupplier(null);
            transactionDTOItem.setSender(null);
        });

        return Response.builder()
                .status(200)
                .message("success")
                .transactions(transactionDTOS)
                .build();
    }

    @Override
    public Response updateTransactionStatus(Long transactionId, TransactionStatus transactionStatus) throws JsonProcessingException {

        Transaction existingTransaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy giao dịch"));
        if(transactionStatus==TransactionStatus.ADMIN_DECIDING&&(existingTransaction.getTransactionType() == TransactionType.PURCHASE||existingTransaction.getTransactionType() == TransactionType.SALE)){
            UserDTO userDTO = userService.getCurrentLoggedInUser();
            User user = modelMapper.map(userDTO, User.class);
            user.setTransactions(null);
            user.setSupplier(null);
            existingTransaction.setSender(user);
        }
        User user1 = modelMapper.map(userService.getCurrentLoggedInUser(), User.class);
        user1.setTransactions(null);
        user1.setSupplier(null);
        existingTransaction.setStatus(transactionStatus);
        existingTransaction.setUpdatedAt(LocalDateTime.now());
        if ((existingTransaction.getTransactionType() == TransactionType.PURCHASE||existingTransaction.getTransactionType() == TransactionType.RETURN_TO_SUPPLIER)&& transactionStatus == TransactionStatus.COMPLETED) {
            Product product = productRepository.findById(existingTransaction.getProduct().getId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy sản phẩm"));
            product.setStockQuantity(product.getStockQuantity() + existingTransaction.getTotalProducts());
            productRepository.save(product);
        }
        if (((existingTransaction.getTransactionType() == TransactionType.SALE)||(existingTransaction.getTransactionType() == TransactionType.RETURN_TO_SUPPLIER&&existingTransaction.getSender().getRole()==UserRole.SUPPLIER))&& transactionStatus == TransactionStatus.PENDING) {
            Product product = productRepository.findById(existingTransaction.getProduct().getId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy sản phẩm"));
            product.setStockQuantity(product.getStockQuantity() - existingTransaction.getTotalProducts());
            product.setUpdatedAt(LocalDateTime.now());
            productRepository.save(product);
        }
        Transaction result = transactionRepository.save(existingTransaction);
        Map<String, Object> message = new HashMap<>();
        message.put("type", "TRANSACTION_UPDATED_RENDER");
        message.put("data", Map.of(
                "id", result.getId(),
                "product",Product.builder().name(result.getProduct().getName()).build(),
                "transactionType", result.getTransactionType().toString(),
                "status", result.getStatus().toString(),
                "totalPrice", result.getTotalPrice(),
                "totalProducts", result.getTotalProducts(),
                "createdAt", result.getCreatedAt().toString()
        ));

        ObjectMapper mapper = new ObjectMapper();
        String jsonString = mapper.writeValueAsString(message);
        WebSocketHandler.broadcastChanges(jsonString);

        return Response.builder()
                .status(200)
                .message("Cập nhập giao dịch thành công")
                .build();
    }

    @Override
    public Response updateTransactionStatusViaQR(Long transactionId, TransactionStatus status) throws JsonProcessingException {
        Transaction existingTransaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy giao dịch"));
        UserDTO user = userService.getCurrentLoggedInUser();
        if (existingTransaction.getStatus() == TransactionStatus.PENDING) {
            if (existingTransaction.getUser() != null && existingTransaction.getUser().getRole() != UserRole.STOCKSTAFF) {
                if (existingTransaction.getUser().getId().equals(user.getId())) {
                    existingTransaction.setStatus(status);
                    existingTransaction.setUpdatedAt(LocalDateTime.now());
                }
            } else if (existingTransaction.getUser() != null) {
                existingTransaction.setStatus(status);
                existingTransaction.setUpdatedAt(LocalDateTime.now());

            } else return Response.builder().status(401).message("Tải khoản người dùng hoặc giao dịch lỗi").build();
        }
        if ((existingTransaction.getTransactionType() == TransactionType.PURCHASE||existingTransaction.getTransactionType() == TransactionType.RETURN_TO_SUPPLIER)&&status == TransactionStatus.COMPLETED ) {
            Product product = productRepository.findById(existingTransaction.getProduct().getId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy sản phẩm"));
            product.setStockQuantity(product.getStockQuantity() + existingTransaction.getTotalProducts());
            productRepository.save(product);
        }
        Transaction result = transactionRepository.save(existingTransaction);
        Map<String, Object> message = new HashMap<>();
        message.put("type", "TRANSACTION_UPDATED_RENDER");
        message.put("data", Map.of(
                "id", result.getId(),
                "product",Product.builder().name(result.getProduct().getName()).build(),
                "transactionType", result.getTransactionType().toString(),
                "status", result.getStatus().toString(),
                "totalPrice", result.getTotalPrice(),
                "totalProducts", result.getTotalProducts(),
                "createdAt", result.getCreatedAt().toString()
        ));

        ObjectMapper mapper = new ObjectMapper();
        String jsonString = mapper.writeValueAsString(message);
        WebSocketHandler.broadcastChanges(jsonString);
        return Response.builder()
                .status(200)
                .message("Cập nhập giao dịch thành công")
                .build();
    }

    @Override
    public File generateInvoicePdfWithQR(Long transactionId, TransactionStatus transactionStatus) throws Exception {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy giao dịch"));

        // 3. Chuẩn bị dữ liệu cho template
        Map<String, String> textValues = new HashMap<>();
        TransactionType transactionType = transaction.getTransactionType();
            textValues.put("supplier_name", String.valueOf(transaction.getSupplier().getName()));
            textValues.put("supplier_address", String.valueOf(transaction.getSupplier().getAddress()));
            textValues.put("supplier_phone", String.valueOf(transaction.getSupplier().getPhone()));
            textValues.put("supplier_email", String.valueOf(transaction.getSupplier().getEmail()));
            textValues.put("user_phoneNumber", transaction.getUser().getPhoneNumber());
            if(transaction.getUser().getSupplier()!=null) {
                textValues.put("user_supplier_name", transaction.getUser().getSupplier().getName());
            textValues.put("user_supplier_address", transaction.getUser().getSupplier().getAddress());
            textValues.put("user_supplier_phone", transaction.getUser().getSupplier().getPhone());
            textValues.put("user_supplier_email", transaction.getUser().getSupplier().getEmail());
            }
            textValues.put("user_name", transaction.getUser().getName());
            textValues.put("sender_name", transaction.getSender().getName());
            textValues.put("sender_phoneNumber", transaction.getSender().getPhoneNumber());
            textValues.put("user_address", transaction.getSupplier().getAddress());
            textValues.put("user_email", transaction.getUser().getEmail());
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
        if (transaction.getTransactionType() == TransactionType.PURCHASE)
            return docxTemplateService.generatePdf("purchase.docx", textValues, Collections.emptyMap());
        if (transaction.getTransactionType() == TransactionType.SALE)
            return docxTemplateService.generatePdf("sale.docx", textValues, Collections.emptyMap());
        if (transaction.getTransactionType() == TransactionType.RETURN_TO_SUPPLIER)
            return docxTemplateService.generatePdf("return_to_supplier.docx", textValues, Collections.emptyMap());
        throw new NotFoundException("Loại giao dịch không được hỗ trợ: " + transaction.getTransactionType());
    }

    @Override
    public Response getActivityReport(Long staffId, String fromDate, String toDate){
        LocalDateTime fromDatee = null;
        LocalDateTime toDatee = null;

        if (fromDate != null && !fromDate.isEmpty()) {
            try {
                fromDatee = LocalDateTime.parse(fromDate);
            } catch (Exception e) {
                // Invalid date format, keep as null
                log.warn("Định dạng fromDate không hợp lệ: " + fromDate);
            }
        }

        if (toDate != null && !toDate.isEmpty()) {
            try {
                toDatee = LocalDateTime.parse(toDate);
            } catch (Exception e) {
                // Invalid date format, keep as null
                log.warn("Định dạng toDate không hợp lệ: " + toDate);
            }
        }
        List<Object[]> res = transactionRepository.getActivityReport(staffId, fromDatee, toDatee);
        Object[] object = res.get(0);
        //Vi query repo tra ve object nen phai ep kieu thu cong tu Object sang long
        return Response.builder()
                .status(200)
                .message("success")
                .activityReport(new ActivityReport((Long) object[0], (Long) object[1], (Long) object[2], (Long) object[3], (Long) object[4], (Long) object[5], (Long) object[6], (Long) object[7]))
                .build();
    }

    @Override
    public Response getBestSellingProducts(int limit, String fromDate, String toDate) {
        LocalDateTime fromDateTime = null;
        LocalDateTime toDateTime = null;

        if (fromDate != null && !fromDate.isEmpty()) {
            try {
                fromDateTime = LocalDateTime.parse(fromDate);
            } catch (Exception e) {
                log.error("Lỗi chuyển đổi fromDate: {}", fromDate, e);
            }
        }

        if (toDate != null && !toDate.isEmpty()) {
            try {
                toDateTime = LocalDateTime.parse(toDate);
            } catch (Exception e) {
                log.error("Lỗi chuyển đổi toDate: {}", toDate, e);
            }
        }

        List<Object[]> results = transactionRepository.findBestSellingProducts(limit, fromDateTime, toDateTime);
        
        // Convert Object[] to Map for easier frontend consumption
        List<Map<String, Object>> bestSellingProducts = results.stream()
            .map(result -> {
                Map<String, Object> product = new HashMap<>();
                product.put("id", result[0]);
                product.put("name", result[1]);
                product.put("sku", result[2]);
                product.put("imageUrl", result[3]);
                product.put("totalSold", result[4]);
                return product;
            })
            .collect(Collectors.toList());

        return Response.builder()
                .status(200)
                .message("Best selling products retrieved successfully")
                .bestSellingProducts(bestSellingProducts)
                .build();
    }

    @Override
    public Response updateTransaction(Long transactionId, TransactionUpdateRequest request) throws JsonProcessingException {
        Transaction existingTransaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy giao dịch"));

        // Update product if provided
        if (request.getProductId() != null) {
            Product product = productRepository.findById(request.getProductId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy sản phẩm"));
            existingTransaction.setProduct(product);
        }

        // Update supplier if provided
        if (request.getSupplierId() != null) {
            Supplier supplier = supplierRepository.findById(request.getSupplierId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy nhà cung cấp"));
            existingTransaction.setSupplier(supplier);
        }

        // Update user if provided
        if (request.getUserId() != null) {
            User user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));
            existingTransaction.setUser(user);
        }

        // Update other fields
        if (request.getQuantity() != null) {
            existingTransaction.setTotalProducts(request.getQuantity());
        }

        if (request.getTotalPrice() != null) {
            existingTransaction.setTotalPrice(request.getTotalPrice());
        } else if (request.getQuantity() != null && existingTransaction.getProduct() != null) {
            // Recalculate total price if quantity changed but total price not provided
            existingTransaction.setTotalPrice(
                existingTransaction.getProduct().getPrice().multiply(BigDecimal.valueOf(request.getQuantity()))
            );
        }

        if (request.getDescription() != null) {
            existingTransaction.setDescription(request.getDescription());
        }

        if (request.getStatus() != null) {
            existingTransaction.setStatus(request.getStatus());
        }

        existingTransaction.setUpdatedAt(LocalDateTime.now());

        Transaction updatedTransaction = transactionRepository.save(existingTransaction);

        // Send WebSocket notification
        Map<String, Object> message = new HashMap<>();
        message.put("type", "TRANSACTION_UPDATED_RENDER");
        message.put("data", Map.of(
                "id", updatedTransaction.getId(),
                "product",Product.builder().name(updatedTransaction.getProduct().getName()).build(),
                "transactionType", updatedTransaction.getTransactionType().toString(),
                "status", updatedTransaction.getStatus().toString(),
                "totalPrice", updatedTransaction.getTotalPrice(),
                "totalProducts", updatedTransaction.getTotalProducts(),
                "createdAt", updatedTransaction.getCreatedAt().toString()
        ));

        ObjectMapper mapper = new ObjectMapper();
        String jsonString = mapper.writeValueAsString(message);
        WebSocketHandler.broadcastChanges(jsonString);

        return Response.builder()
                .status(200)
                .message("Transaction cập nhật thành công")
                .build();
    }
}
