
package com.banking.transaction.repository;

import com.banking.transaction.entity.Transaction;
import com.banking.transaction.entity.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository
        extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByTransactionReference(
            String transactionReference);

    boolean existsByTransactionReference(
            String transactionReference);

    List<Transaction> findByStatus(TransactionStatus status);

    List<Transaction> findByFromAccountAccountId(Long accountId);

    List<Transaction> findByToAccountAccountId(Long accountId);
}
