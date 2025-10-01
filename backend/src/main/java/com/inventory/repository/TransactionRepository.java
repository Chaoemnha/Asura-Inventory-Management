package com.inventory.repository;

import com.inventory.entity.Transaction;
import com.inventory.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {


    @Query("SELECT t FROM Transaction t " +
            "WHERE YEAR(t.createdAt) = :year AND MONTH(t.createdAt) = :month")
    List<Transaction> findAllByMonthAndYear(@Param("month") int month, @Param("year") int year);

    @Query("SELECT t FROM Transaction t WHERE t.transactionType IN (:types)")
    List<Transaction> findAllByTransactionTypes(@Param("types") List<TransactionType> types);

    //we are searching these field; Transaction's description, note, status, Product's name, sku
    @Query("SELECT t FROM Transaction t " +
            "LEFT JOIN t.product p " +
            "WHERE " +
            "(:searchText IS NULL OR " +
            "LOWER(t.description) LIKE LOWER(CONCAT('%', :searchText, '%')) OR " +
            "LOWER(t.transactionType) LIKE LOWER(CONCAT('%', :searchText, '%')) OR " +
            "LOWER(t.status) LIKE LOWER(CONCAT('%', :searchText, '%')) OR " +
            "LOWER(p.name) LIKE LOWER(CONCAT('%', :searchText, '%')) OR " +
            "LOWER(p.sku) LIKE LOWER(CONCAT('%', :searchText, '%'))) " +
            "AND (" +
            "(:userId IS NULL AND :supplierId IS NULL) " +
            "OR (t.user.id = :userId) " +
            "OR (t.supplier.id = :supplierId)" +
            ")")
    Page<Transaction> searchTransactions(@Param("searchText") String searchText,
                                         @Param("userId") Long userId,
                                         @Param("supplierId") Long supplierId,
                                         Pageable pageable);

}
