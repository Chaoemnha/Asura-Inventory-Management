package com.inventory.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class RagDataExtractionService {
    private final CategoryServiceImpl categoryServiceImpl;
    private final UserServiceImpl userServiceImpl;
    private final ProductServiceImpl productServiceImpl;
    private final SupplierServiceImpl supplierServiceImpl;
    private final TransactionServiceImpl transactionServiceImpl;

    public List<String> extractAllDataForEmbedding(){
        List<String> texts = new ArrayList<>();
        texts.addAll(categoryServiceImpl.extractTextForEmbedding());
        texts.addAll(userServiceImpl.extractTextForEmbedding());
        texts.addAll(productServiceImpl.extractTextForEmbedding());
        texts.addAll(supplierServiceImpl.extractTextForEmbedding());
        texts.addAll(transactionServiceImpl.extractTextForEmbedding());
        return texts;
    }

    public void textExtraction(){
        List<String> texts = extractAllDataForEmbedding();
        //Lambda co the thay bang method reference text -> {System.out.println(text);} => System.out::println)
        texts.forEach(System.out::println);
    }
}
