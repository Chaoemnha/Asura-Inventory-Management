package com.inventory.service;

import com.inventory.dto.TransactionDTO;
import com.inventory.enums.TransactionType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
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
        fileName = "transaction_report"+System.currentTimeMillis()+".xlsx";
        response.setHeader("Content-Disposition", "attachment;filename=\""+fileName+"\"");
        Sheet sheet = workbook.createSheet("data");

        // Set column widths
        int[] colWidths = {1500, 4000, 3000, 5000, 7000, 4000, 4000, 4000, 4000, 5000, 5000};
        for (int i = 0; i < colWidths.length; i++) {
            sheet.setColumnWidth(i, colWidths[i]);
        }

        // Create header style
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
        headerStyle.setFont(headerFont);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);

        // Create data style
        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setAlignment(HorizontalAlignment.LEFT);

        Row header = sheet.createRow(0);
        header.setHeightInPoints(24); // Set header row height
        String[] headers = {
            "#", "Transaction type", "Status", "Product Name", "Description",
            "Total price", "Total products", "Supplier name", "User name", "Create date", "Update date"
        };
        for (int i = 0; i < headers.length; i++) {
            header.createCell(i).setCellValue(headers[i]);
            header.getCell(i).setCellStyle(headerStyle);
        }

        int rownum = 1;
        for(TransactionDTO transaction: transactionDTOList) {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            Row row = sheet.createRow(rownum++);
            row.setHeightInPoints(20); // Set data row height

            row.createCell(0).setCellValue(rownum-1);
            row.getCell(0).setCellStyle(dataStyle);

            row.createCell(1).setCellValue(transaction.getTransactionType().toString());
            row.getCell(1).setCellStyle(dataStyle);

            row.createCell(2).setCellValue(String.valueOf(transaction.getStatus()));
            row.getCell(2).setCellStyle(dataStyle);

            row.createCell(3).setCellValue(transaction.getProduct().getName());
            row.getCell(3).setCellStyle(dataStyle);

            row.createCell(4).setCellValue(transaction.getDescription());
            row.getCell(4).setCellStyle(dataStyle);

            row.createCell(5).setCellValue(transaction.getTotalPrice().toString());
            row.getCell(5).setCellStyle(dataStyle);

            row.createCell(6).setCellValue(transaction.getTotalProducts());
            row.getCell(6).setCellStyle(dataStyle);

            row.createCell(7).setCellValue(transaction.getSupplier().getName());
            row.getCell(7).setCellStyle(dataStyle);

            row.createCell(8).setCellValue(transaction.getUser().getName());
            row.getCell(8).setCellStyle(dataStyle);

            LocalDateTime createdAt = transaction.getCreatedAt();
            Date createdDate = Date.from(createdAt.atZone(ZoneId.systemDefault()).toInstant());
            row.createCell(9).setCellValue(simpleDateFormat.format(createdDate));
            row.getCell(9).setCellStyle(dataStyle);

            if(transaction.getUpdatedAt()!=null) {
                LocalDateTime updatedAt = transaction.getUpdatedAt();
                Date updatedDate = Date.from(updatedAt.atZone(ZoneId.systemDefault()).toInstant());
                row.createCell(10).setCellValue(simpleDateFormat.format(updatedDate));
            }
            else {
                row.createCell(10).setCellValue("");
            }
            row.getCell(10).setCellStyle(dataStyle);
        }

    }
}
