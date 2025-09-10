package com.inventory.service;

import com.deepoove.poi.XWPFTemplate;
import com.deepoove.poi.config.Configure;
import com.deepoove.poi.config.ConfigureBuilder;
import com.deepoove.poi.plugin.table.LoopRowTableRenderPolicy;
import com.deepoove.poi.data.PictureRenderData;
import com.deepoove.poi.data.Pictures;
import com.deepoove.poi.data.PictureType;
import com.inventory.config.TemplateProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.awt.image.BufferedImage;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocxTemplateService {
    private final TemplateProperties props;

    public File convertDocxToPdf(File docx) throws Exception {
        log.info("📤 Converting DOCX to PDF: {}", docx.getName());

        File parentDir = docx.getParentFile();

        ProcessBuilder pb = new ProcessBuilder(
                "C:\\Program Files\\LibreOffice\\program\\soffice.exe", "--headless", "--convert-to", "pdf",
                "--outdir", parentDir.getAbsolutePath(),
                docx.getAbsolutePath()
        );
        pb.inheritIO();
        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            log.error("❌ LibreOffice convert failed with exit code: {}", exitCode);
            throw new RuntimeException("LibreOffice convert failed");
        }
        String pdfFileName = docx.getName().replaceFirst("\\.docx$", ".pdf");
        File actualPdf = new File(parentDir, pdfFileName);
        if (!actualPdf.exists()) {
            throw new FileNotFoundException("❌ PDF not found: " + actualPdf.getAbsolutePath());
        }
        log.info("✅ PDF created at: {}", actualPdf.getAbsolutePath());
        return actualPdf;
    }


    public File generateDocx(String templateName,
                             Map<String, String> textValues,
                             Map<String, List<Map<String, String>>> tableValues) throws Exception {
        // Chuan bi data de render vao template
        BufferedImage qrImage = BarcodeService.generateQR("http://localhost:8080/api/transactions/update/"+textValues.get("transaction_id")+"/"+ textValues.get("update_status"), 150, 150);

        PictureRenderData qrData = Pictures.ofBufferedImage(qrImage, PictureType.PNG)
                .size(150, 150)
                .create();

        BufferedImage barcodeImage = BarcodeService.generateBarcode(textValues.get("transaction_id"), 300, 100);

        PictureRenderData barcodeData = Pictures.ofBufferedImage(barcodeImage, PictureType.PNG)
                .size(64, 24)
                .create();
        Map<String, Object> data = new HashMap<>();
        data.put("qr_code", qrData);
        data.put("bar_code", barcodeData);
        if (textValues != null) data.putAll(textValues);
        if (tableValues != null) data.putAll(tableValues);
        // 2. Configure loop policies for table keys
        ConfigureBuilder configBuilder = Configure.builder();
        if (tableValues != null) {
            for (String tableKey : tableValues.keySet()) {
                configBuilder.bind(tableKey, new LoopRowTableRenderPolicy());
            }
        }
        Configure config = configBuilder.build();
        // 3. Prepare template path
        String templatePath = Paths.get(props.getDir(), templateName).toString();
        // 4. Load and render template
        XWPFTemplate template = XWPFTemplate.compile(templatePath, config).render(data);

        // 5. Generate temp file
        Path outputPath = Files.createTempFile("filled_", ".docx");
        try (FileOutputStream out = new FileOutputStream(outputPath.toFile())) {
            template.write(out);
        }
        template.close();
        return outputPath.toFile();
    }

    public File generatePdf(String templateName,
                            Map<String, String> textValues,
                            Map<String, List<Map<String, String>>> tableValues) throws Exception {

        File docxFile = generateDocx(templateName, textValues, tableValues);
        return convertDocxToPdf(docxFile);
    }

}
