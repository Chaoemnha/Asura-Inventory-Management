package com.inventory.service;

import com.inventory.dto.TransactionDTO;
import com.inventory.enums.TransactionType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.web.servlet.view.document.AbstractXlsxView;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class InvoiceReport extends AbstractXlsxView {
    @Override
    protected void buildExcelDocument(Map<String, Object> model, Workbook workbook, HttpServletRequest request, HttpServletResponse response) throws Exception {
        // TODO Auto-generated method stub
        String fileName = "";
        List<TransactionDTO> transactionDTOList = (List<TransactionDTO>) model.get("report");
        if(transactionDTOList.size()<2&&transactionDTOList.getFirst().getTransactionType()== TransactionType.PURCHASE) {
            fileName = "transactions_purchase"+System.currentTimeMillis()+".xlsx";
        }
        else if(transactionDTOList.size()<2&&transactionDTOList.getFirst().getTransactionType()== TransactionType.SALE){
            fileName = "transactions_sale"+System.currentTimeMillis()+".xlsx";
        }
        else{
            fileName = "transaction_all"+System.currentTimeMillis()+".xlsx";
        }
        response.setHeader("Content-Disposition", "attachment;filename=\""+fileName+"\"");
        Sheet sheet = workbook.createSheet("data");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("#");
        header.createCell(1).setCellValue("Description");
        header.createCell(2).setCellValue("Status");
        header.createCell(3).setCellValue("Total price");
        header.createCell(4).setCellValue("Total products");
        header.createCell(5).setCellValue("Transaction type");
        header.createCell(6).setCellValue("Create date");
        header.createCell(7).setCellValue("Update date");
        int rownum = 1;
        for(TransactionDTO transaction: transactionDTOList) {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            Row row = sheet.createRow(rownum++);
            //Vi dong dau la tieu de nen stt du lieu se tru 1
            row.createCell(0).setCellValue(rownum-1);
            row.createCell(1).setCellValue(transaction.getDescription());
            row.createCell(2).setCellValue(String.valueOf(transaction.getStatus()));
            row.createCell(3).setCellValue(transaction.getTotalPrice().toString());
            row.createCell(4).setCellValue(transaction.getTotalProducts());
            row.createCell(5).setCellValue(transaction.getTransactionType().toString());
            LocalDateTime createdAt = transaction.getCreatedAt();
            Date createdDate = Date.from(createdAt.atZone(ZoneId.systemDefault()).toInstant());
            row.createCell(6).setCellValue(simpleDateFormat.format(createdDate));

            if(transaction.getUpdatedAt()!=null) {
                LocalDateTime updatedAt = transaction.getUpdatedAt();
                Date updatedDate = Date.from(updatedAt.atZone(ZoneId.systemDefault()).toInstant());
                row.createCell(7).setCellValue(simpleDateFormat.format(updatedDate));
            }
            else {
                row.createCell(7).setCellValue("");
            }
        }

    }
}

