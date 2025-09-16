package com.inventory.repository;

import com.inventory.entity.Product;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    @Query("SELECT p FROM Product p " +
            "LEFT JOIN p.category c WHERE c.name = :categoryName")
    List<Product> findAllByCategory_Name(@Param("categoryName") String categoryName, Sort sort);
}
