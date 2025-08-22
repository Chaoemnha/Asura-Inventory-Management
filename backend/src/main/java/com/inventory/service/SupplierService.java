package com.inventory.service;

import com.inventory.dto.Response;
import com.inventory.dto.SupplierDTO;

import java.util.List;

public interface SupplierService {
    Response addSupplier(SupplierDTO supplierDTO);
    Response updateSupplier(Long id, SupplierDTO supplierDTO);
    Response getAllSuppliers();
    Response getSupplierById(Long id);
    Response deleteSupplier(Long id);
    List<String> extractTextForEmbedding();
}
