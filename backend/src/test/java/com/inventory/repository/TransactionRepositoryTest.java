package com.inventory.repository;

import com.inventory.entity.Transaction;
import com.inventory.enums.TransactionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class TransactionRepositoryTest {
    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    @DisplayName("Luu va tim giao dich theo ID")
    void testSaveAndFindById(){
        Transaction transaction = new Transaction();
        transaction.setTransactionType(TransactionType.PURCHASE);
        Transaction saved = transactionRepository.save(transaction);

        Optional<Transaction> found = transactionRepository.findById(saved.getId());
        // Testcase: Kiểm tra xem giao dịch đã được lưu và tìm lại bằng ID có tồn tại không
        assertThat(found).isPresent();
        // Testcase: Kiểm tra loại giao dịch tìm được có khớp với giá trị ban đầu không
        assertThat(found.get().getTransactionType()).isEqualTo(TransactionType.PURCHASE);
    }

    @Test
    @DisplayName("Luu va lay tat ca giao dich")
    void testSaveAndFindAll(){
        Transaction transaction = new Transaction();
        transaction.setTransactionType(TransactionType.PURCHASE);
        Transaction saved = transactionRepository.save(transaction);

        List<Transaction> transactions = transactionRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
        // Testcase: Kiểm tra danh sách giao dịch sau khi lưu có không rỗng không
        assertThat(transactions).isNotEmpty();
        // Testcase: Kiểm tra giao dịch đầu tiên trong danh sách có loại khớp với giá trị ban đầu không
        assertThat(transactions.get(0).getTransactionType()).isEqualTo(TransactionType.PURCHASE);
    }

    @Test
    @DisplayName("Luu va xoa giao dich theo ID")
    void testSaveAndDeleteById(){
        Transaction transaction = new Transaction();
        transaction.setTransactionType(TransactionType.PURCHASE);
        Transaction saved = transactionRepository.save(transaction);

        transactionRepository.deleteById(saved.getId());
        Optional<Transaction> found = transactionRepository.findById(saved.getId());
        // Testcase: Kiểm tra xem giao dịch đã bị xóa và không còn tồn tại không
        assertThat(found).isNotPresent();
    }

    @Test
    @DisplayName("Tim giao dich theo thang va nam")
    void testFindAllByMonthAndYear() {
        Transaction t1 = new Transaction();
        t1.setTransactionType(TransactionType.PURCHASE);
        transactionRepository.save(t1);

        Transaction t2 = new Transaction();
        t2.setTransactionType(TransactionType.SALE);
        transactionRepository.save(t2);

        List<Transaction> nowTransactions = transactionRepository.findAllByMonthAndYear(LocalDateTime.now().getMonth().getValue(), LocalDateTime.now().getYear());
        // Testcase: Kiểm tra danh sách giao dịch theo tháng và năm hiện tại có đúng số lượng không
        assertThat(nowTransactions).hasSize(2);
    }

    @Test
    @DisplayName("Tim giao dich theo loai giao dich")
    void testFindAllByTransactionTypes() {
        Transaction t1 = new Transaction();
        t1.setTransactionType(TransactionType.PURCHASE);
        transactionRepository.save(t1);

        Transaction t2 = new Transaction();
        t2.setTransactionType(TransactionType.SALE);
        transactionRepository.save(t2);

        List<Transaction> found = transactionRepository.findAllByTransactionConditions(TransactionType.PURCHASE, null, null, null, null);
        // Testcase: Kiểm tra danh sách giao dịch theo loại PURCHASE có đúng số lượng không
        assertThat(found).hasSize(1);
        // Testcase: Kiểm tra loại giao dịch đầu tiên trong danh sách có khớp với PURCHASE không
        assertThat(found.get(0).getTransactionType()).isEqualTo(TransactionType.PURCHASE);
    }
}
