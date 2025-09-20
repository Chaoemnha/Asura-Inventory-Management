package com.inventory.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.inventory.dto.Response;
import com.inventory.dto.SupplierDTO;

import java.util.List;

public interface SupplierService {
    Response addSupplier(SupplierDTO supplierDTO) throws JsonProcessingException;
    Response updateSupplier(Long id, SupplierDTO supplierDTO) throws JsonProcessingException;
    Response getAllSuppliers();
    Response getSupplierById(Long id);
    Response deleteSupplier(Long id) throws JsonProcessingException;
    List<String> extractTextForEmbedding();
}
