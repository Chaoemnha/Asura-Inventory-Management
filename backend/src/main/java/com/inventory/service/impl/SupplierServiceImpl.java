package com.inventory.service.impl;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.dto.Response;
import com.inventory.dto.SupplierDTO;
import com.inventory.entity.Supplier;
import com.inventory.exceptions.InvalidCredentialsException;
import com.inventory.exceptions.NotFoundException;
import com.inventory.repository.SupplierRepository;
import com.inventory.service.SupplierService;
import com.inventory.utils.WebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupplierServiceImpl implements SupplierService {

    private final SupplierRepository supplierRepository;
    @Autowired
    @Qualifier("modelMapper")
    private final ModelMapper modelMapper;

    @Override
    public Response addSupplier(SupplierDTO supplierDTO) throws JsonProcessingException {
        Optional<Supplier> supplier1 = supplierRepository.findByName(supplierDTO.getName());
        Optional<Supplier> supplier2 = supplierRepository.findByPhone(supplierDTO.getPhone());
        Optional<Supplier> supplier3 = supplierRepository.findByEmail(supplierDTO.getEmail());
        if (supplier1.isPresent()||supplier2.isPresent()||supplier3.isPresent()) {
            throw new InvalidCredentialsException("Supplier already exists");
        }
        Supplier supplierToSave = modelMapper.map(supplierDTO, Supplier.class);
        Supplier result = supplierRepository.save(supplierToSave);
        Map<String, Object> message = new HashMap<>();
        message.put("type", "SUPPLIER_ADDED_RENDER");
        message.put("data", Map.of(
                "id", result.getId().toString(),
                "name", result.getName(),
                "email", result.getEmail(),
                "phone", result.getPhone(),
                "address", result.getAddress()
        ));

        ObjectMapper mapper = new ObjectMapper();
        String jsonString = mapper.writeValueAsString(message);
        WebSocketHandler.broadcastChanges(jsonString);
        return Response.builder()
                .status(200)
                .message("Supplier added successfully")
                .build();
    }

    @Override
    public Response updateSupplier(Long id, SupplierDTO supplierDTO) throws JsonProcessingException {

        Supplier existingSupplier = supplierRepository.findById(id)
                .orElseThrow(()-> new NotFoundException("Supplier Not Found"));

        if (supplierDTO.getName() != null) existingSupplier.setName(supplierDTO.getName());
        if (supplierDTO.getAddress() != null) existingSupplier.setAddress(supplierDTO.getAddress());
        if (supplierDTO.getPhone() != null) existingSupplier.setPhone(supplierDTO.getPhone());
        if (supplierDTO.getEmail() != null) existingSupplier.setEmail(supplierDTO.getEmail());

        supplierRepository.save(existingSupplier);
        Map<String, Object> message = new HashMap<>();
        message.put("type", "SUPPLIER_UPDATED_RENDER");
        message.put("data", Map.of(
                "id", existingSupplier.getId(),
                "name", existingSupplier.getName(),
                "email", existingSupplier.getEmail(),
                "phone", existingSupplier.getPhone(),
                "address", existingSupplier.getAddress()
        ));

        ObjectMapper mapper = new ObjectMapper();
        String jsonString = mapper.writeValueAsString(message);
        WebSocketHandler.broadcastChanges(jsonString);
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
    public Response deleteSupplier(Long id) throws JsonProcessingException {

        supplierRepository.findById(id)
                .orElseThrow(()-> new NotFoundException("Supplier Not Found"));

        supplierRepository.deleteById(id);
        Map<String, Object> message = new HashMap<>();
        message.put("type", "SUPPLIER_DELETED_RENDER");
        message.put("supplierId", id);
        ObjectMapper mapper = new ObjectMapper();
        String jsonString = mapper.writeValueAsString(message);
        WebSocketHandler.broadcastChanges(jsonString);
        return Response.builder()
                .status(200)
                .message("Supplier Successfully Deleted")
                .build();
    }

    @Override
    public List<String> extractTextForEmbedding() {
        List<Supplier> suppliers = supplierRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
        List<SupplierDTO> supplierDTOS = modelMapper.map(suppliers, new TypeToken<List<SupplierDTO>>() {}.getType());
        return supplierDTOS.stream().map(SupplierDTO::getTextForEmbedding).collect(Collectors.toList());
    }
}
