package com.inventory.repository;

import com.inventory.dto.ActivityReport;
import com.inventory.entity.Transaction;
import com.inventory.enums.TransactionStatus;
import com.inventory.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {


    @Query("SELECT t FROM Transaction t " +
            "WHERE YEAR(t.createdAt) = :year AND MONTH(t.createdAt) = :month")
    List<Transaction> findAllByMonthAndYear(@Param("month") int month, @Param("year") int year);

    @Query("SELECT t FROM Transaction t " +
            "LEFT JOIN t.product p " +
            "LEFT JOIN t.user u " +
            "LEFT JOIN t.supplier s " +
            "WHERE " +
            "(:transactionType IS NULL OR t.transactionType = :transactionType) " +
            "AND (:status IS NULL OR t.status = :status) " +
            "AND (:productName IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :productName, '%'))) " +
            "AND (:fromDate IS NULL OR t.createdAt >= :fromDate) " +
            "AND (:toDate IS NULL OR t.createdAt <= :toDate)")
    List<Transaction> findAllByTransactionConditions(@Param("transactionType") TransactionType transactionType,
                                                @Param("status") TransactionStatus status,
                                                @Param("productName") String productName,
                                                @Param("fromDate") LocalDateTime fromDate, @Param("toDate") LocalDateTime toDate);

    //we are searching these field; Transaction's description, note, status, Product's name, sku
    @Query("SELECT t FROM Transaction t " +
            "LEFT JOIN t.product p " +
            "LEFT JOIN t.user u " +
            "LEFT JOIN t.supplier s " +
            "LEFT JOIN t.sender sd " +
            "WHERE " +
            "(:transactionType IS NULL OR t.transactionType = :transactionType) " +
            "AND (:status IS NULL OR t.status = :status) " +
            "AND (:productName IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :productName, '%'))) " +
            "AND (:userId IS NULL OR (u.id = :userId OR sd.id = :userId) OR (t.status = 'SENDER_DECIDING' AND (t.transactionType = 'SALE' OR t.transactionType = 'PURCHASE') AND s.id = :supplierId))" +
            "AND (:fromDate IS NULL OR t.createdAt >= :fromDate) " +
            "AND (:toDate IS NULL OR t.createdAt <= :toDate)")
    Page<Transaction> searchTransactions(@Param("transactionType") TransactionType transactionType,
                                         @Param("status") TransactionStatus status,
                                         @Param("productName") String productName,
                                         @Param("userId") Long userId,
                                         @Param("supplierId") Long supplierId,
                                         @Param("fromDate") LocalDateTime fromDate,
                                         @Param("toDate") LocalDateTime toDate,
                                         Pageable pageable);

    @Query("SELECT " +
            "COUNT(CASE WHEN t.transactionType = 'SALE' AND t.status != 'CANCELED' THEN 1 ELSE NULL END), " +
            "COUNT(CASE WHEN t.transactionType = 'SALE' AND t.status = 'COMPLETED' THEN 1 ELSE NULL END), " +
            "COUNT(CASE WHEN t.transactionType = 'PURCHASE' AND t.status != 'CANCELED' THEN 1 ELSE NULL END), " +
            "COUNT(CASE WHEN t.transactionType = 'PURCHASE' AND t.status = 'COMPLETED' THEN 1 ELSE NULL END), " +
            "COALESCE(SUM(CASE WHEN t.transactionType = 'SALE' AND t.status != 'CANCELED' THEN t.totalProducts ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN t.transactionType = 'SALE' AND t.status = 'COMPLETED' THEN t.totalProducts ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN t.transactionType = 'PURCHASE' AND t.status != 'CANCELED' THEN t.totalProducts ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN t.transactionType = 'PURCHASE' AND t.status = 'COMPLETED' THEN t.totalProducts ELSE 0 END), 0) " +
            "FROM Transaction t WHERE (t.user.id = :staffId OR t.sender.id = :staffId) AND (t.createdAt BETWEEN :fromDatee AND :toDatee)")
    List<Object[]> getActivityReport(@Param("staffId") Long staffId,@Param("fromDatee") LocalDateTime fromDatee,@Param("toDatee") LocalDateTime toDatee);

    @Query(value = "SELECT p.id, p.name, p.sku, p.image_url, SUM(t.total_products) as total_sold " +
            "FROM transactions t " +
            "INNER JOIN products p ON t.product_id = p.id " +
            "WHERE t.transaction_type = 'SALE' AND t.status = 'COMPLETED' " +
            "AND (:fromDate IS NULL OR t.created_at >= :fromDate) " +
            "AND (:toDate IS NULL OR t.created_at <= :toDate) " +
            "GROUP BY p.id, p.name, p.sku, p.image_url " +
            "ORDER BY total_sold DESC " +
            "LIMIT :limit", nativeQuery = true)
    List<Object[]> findBestSellingProducts(@Param("limit") int limit, 
                                          @Param("fromDate") LocalDateTime fromDate, 
                                          @Param("toDate") LocalDateTime toDate);
}
