package com.inventory.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.inventory.dto.Response;
import com.inventory.dto.TransactionDTO;
import com.inventory.dto.TransactionRequest;
import com.inventory.entity.Transaction;
import com.inventory.enums.TransactionStatus;
import com.inventory.enums.TransactionType;
import com.inventory.service.InvoiceReport;
import com.inventory.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;


    @PostMapping("/purchase")
    public ResponseEntity<Response> restockInventory(@RequestBody @Valid TransactionRequest transactionRequest)  throws JsonProcessingException {
        return ResponseEntity.ok(transactionService.restockInventory(transactionRequest));
    }
    @PostMapping("/sell")
    public ResponseEntity<Response> sell(@RequestBody @Valid TransactionRequest transactionRequest)  throws JsonProcessingException{
        return ResponseEntity.ok(transactionService.sell(transactionRequest));
    }
    @PostMapping("/return")
    public ResponseEntity<Response> returnToSupplier(@RequestBody @Valid TransactionRequest transactionRequest)  throws JsonProcessingException{
        return ResponseEntity.ok(transactionService.returnToSupplier(transactionRequest));
    }

    @GetMapping("/all")
    public ResponseEntity<Response> getAllTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "1000") int size,
            @RequestParam(required = false) String searchText,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long supplierId
    ) {
        return ResponseEntity.ok(transactionService.getAllTransactions(page, size, searchText, userId, supplierId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Response> getTransactionById(@PathVariable Long id) {
        return ResponseEntity.ok(transactionService.getTransactionById(id));
    }

    @GetMapping("/by-month-year")
    public ResponseEntity<Response> getAllTransactionByMonthAndYear(
            @RequestParam int month,
            @RequestParam int year
    ) {
        return ResponseEntity.ok(transactionService.getAllTransactionByMonthAndYear(month, year));
    }

    @PutMapping("/update/{transactionId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Response> updateTransactionStatus(
            @PathVariable Long transactionId,
            @RequestBody @Valid TransactionStatus status) throws JsonProcessingException {
        System.out.println("ID IS: " + transactionId);
        System.out.println("Status IS: " + status);
        return ResponseEntity.ok(transactionService.updateTransactionStatus(transactionId, status));
    }

    @GetMapping("/update/{transactionId}/{statu}")
    public ResponseEntity<Response> updateTransactionStatus(
            @PathVariable Long transactionId, @PathVariable String statu) throws JsonProcessingException{
        TransactionStatus status =  TransactionStatus.valueOf(statu);
        return ResponseEntity.ok(transactionService.updateTransactionStatusViaQR(transactionId, status));
    }

    @GetMapping("/export")
    public ModelAndView exportReport(@RequestParam String type) {
        ModelAndView mav = new ModelAndView("export");
        if(type.equals("purchase")){
            List<TransactionType> transactionTypes = Arrays.asList(TransactionType.PURCHASE);
            List<TransactionDTO> transactionDTOList = transactionService.getAllTransactionByType(transactionTypes);
            mav.addObject("report", transactionDTOList);
            mav.addObject("type", type);
            mav.setView(new InvoiceReport());
        } else if (type.equals("sale")) {
            List<TransactionType> transactionTypes = Arrays.asList(TransactionType.SALE);
            List<TransactionDTO> transactionDTOList = transactionService.getAllTransactionByType(transactionTypes);
            mav.addObject("report", transactionDTOList);
            mav.addObject("type", type);
            mav.setView(new InvoiceReport());
        } else {
            List<TransactionType> transactionTypes = Arrays.asList(TransactionType.PURCHASE,  TransactionType.SALE);
            List<TransactionDTO> transactionDTOList = transactionService.getAllTransactionByType(transactionTypes);
            mav.addObject("report", transactionDTOList);
            mav.addObject("type", type);
            mav.setView(new InvoiceReport());
        }
        return mav;
    }

    @GetMapping("/export-invoice-qr")
    public ResponseEntity<Resource> exportInvoiceWithQR(@RequestParam Long transactionId, @RequestParam TransactionStatus transactionStatus) throws Exception {
        // Tạo file PDF h8uóa đơn có QR code (giả sử service trả về file PDF)
        File pdfFile = transactionService.generateInvoicePdfWithQR(transactionId, transactionStatus);

        InputStreamResource resource = new InputStreamResource(new FileInputStream(pdfFile));
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=invoice_" + transactionId + ".pdf");
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE);

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(pdfFile.length())
                .body(resource);
    }
}
