package com.inventory.repository;

import com.inventory.entity.Product;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    @Query("SELECT p FROM Product p " +
            "LEFT JOIN p.category c " +
            "WHERE c.name = :categoryName " +
            "AND (" +
            ":searchText IS NULL OR " +
            "LOWER(p.name) LIKE LOWER(CONCAT('%', :searchText, '%')) OR " +
            "STR(p.id) LIKE :searchText" +
            ")")
    List<Product> findAllByCategory_Name(@Param("categoryName") String categoryName, @Param("searchText") String searchText, Sort sort);

    @Query("SELECT p FROM Product p " +
            "WHERE (" +
            ":searchText IS NULL OR " +
            "LOWER(p.name) LIKE LOWER(CONCAT('%', :searchText, '%')) OR " +
            "STR(p.id) LIKE :searchText" +
            ")")
    List<Product> findAll(@Param("searchText") String searchText, Sort sort);

    @Query(value = "SELECT * FROM products ORDER BY stock_quantity DESC LIMIT :limit", nativeQuery = true)
    List<Product> findTopProductsByStockQuantity(@Param("limit") int limit);
}
