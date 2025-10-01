package com.inventory.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.inventory.dto.Response;
import com.inventory.dto.TransactionDTO;
import com.inventory.dto.TransactionRequest;
import com.inventory.dto.TransactionUpdateRequest;
import com.inventory.enums.TransactionStatus;

import java.io.File;
import java.util.List;

public interface TransactionService {
    Response restockInventory(TransactionRequest transactionRequest) throws JsonProcessingException;
    Response sell(TransactionRequest transactionRequest) throws JsonProcessingException;
    Response returnToSupplier(TransactionRequest transactionRequest) throws JsonProcessingException;
    Response getAllTransactions(int page, int size, String searchType, String searchStatus,
                               String searchProductName, Long userId,
                               String searchFromDate, String searchToDate);
    Response getTransactionById(Long id);
    List<TransactionDTO> getAllTransactionByCondition(String searchType, String searchStatus, String searchProductName, String searchFromDate, String searchToDate);
    Response getAllTransactionByMonthAndYear(int month, int year);
    Response updateTransactionStatus(Long transactionId, TransactionStatus transactionStatus) throws JsonProcessingException;
    Response updateTransactionStatusViaQR(Long transactionId, TransactionStatus status) throws JsonProcessingException;
    File generateInvoicePdfWithQR(Long transactionId, TransactionStatus transactionStatus) throws Exception;
    Response getActivityReport(Long staffId, String fromDate, String toDate);
    Response getBestSellingProducts(int limit, String fromDate, String toDate);
    Response updateTransaction(Long transactionId, TransactionUpdateRequest request) throws JsonProcessingException;
}
