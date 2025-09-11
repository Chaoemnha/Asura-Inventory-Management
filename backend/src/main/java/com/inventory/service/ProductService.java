package com.inventory.service;

import com.inventory.dto.ProductDTO;
import com.inventory.dto.Response;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProductService {
    Response saveProduct(ProductDTO productDTO, MultipartFile imageFile);
    Response updateProduct(ProductDTO productDTO, MultipartFile imageFile);
    Response getAllProducts();
    Response getProductById(Long id);
    Response deleteProduct(Long id);
    List<String> extractTextForEmbedding();
    Response getAllProductsByCategoryName(String categoryName);
}
