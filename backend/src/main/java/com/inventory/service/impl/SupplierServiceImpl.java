package com.inventory.service.impl;


import com.inventory.dto.Response;
import com.inventory.dto.SupplierDTO;
import com.inventory.entity.Supplier;
import com.inventory.exceptions.NotFoundException;
import com.inventory.repository.SupplierRepository;
import com.inventory.service.SupplierService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupplierServiceImpl implements SupplierService {

    private final SupplierRepository supplierRepository;
    private final ModelMapper modelMapper;

    @Override
    public Response addSupplier(SupplierDTO supplierDTO) {
        Supplier supplierToSave = modelMapper.map(supplierDTO, Supplier.class);
        supplierRepository.save(supplierToSave);

        return Response.builder()
                .status(200)
                .message("Supplier added successfully")
                .build();
    }

    @Override
    public Response updateSupplier(Long id, SupplierDTO supplierDTO) {

        Supplier existingSupplier = supplierRepository.findById(id)
                .orElseThrow(()-> new NotFoundException("Supplier Not Found"));

        if (supplierDTO.getName() != null) existingSupplier.setName(supplierDTO.getName());
        if (supplierDTO.getAddress() != null) existingSupplier.setAddress(supplierDTO.getAddress());

        supplierRepository.save(existingSupplier);

        return Response.builder()
                .status(200)
                .message("Supplier Successfully Updated")
                .build();
    }

    @Override
    public Response getAllSuppliers() {

        List<Supplier> suppliers = supplierRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));

        List<SupplierDTO> supplierDTOS = modelMapper.map(suppliers, new TypeToken<List<SupplierDTO>>() {}.getType());

        return Response.builder()
                .status(200)
                .message("success")
                .suppliers(supplierDTOS)
                .build();
    }

    @Override
    public Response getSupplierById(Long id) {

        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(()-> new NotFoundException("Supplier Not Found"));

        SupplierDTO supplierDTO = modelMapper.map(supplier, SupplierDTO.class);

        return Response.builder()
                .status(200)
                .message("success")
                .supplier(supplierDTO)
                .build();
    }

    @Override
    public Response deleteSupplier(Long id) {

        supplierRepository.findById(id)
                .orElseThrow(()-> new NotFoundException("Supplier Not Found"));

        supplierRepository.deleteById(id);

        return Response.builder()
                .status(200)
                .message("Supplier Successfully Deleted")
                .build();
    }

    @Override
    public List<String> extractTextForEmbedding() {
        List<Supplier> suppliers = supplierRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
        return suppliers.stream().map(Supplier::getTextForEmbedding).collect(Collectors.toList());
    }
}
