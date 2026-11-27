
package com.banking.transaction.repository;

import com.banking.transaction.entity.TransactionAuditLog;
import com.banking.transaction.entity.AuditResult;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TransactionAuditLogRepository
        extends JpaRepository<TransactionAuditLog, Long> {

    List<TransactionAuditLog> findByTransactionTransactionId(
            Long transactionId);

    List<TransactionAuditLog> findByAccountAccountId(
            Long accountId);

    List<TransactionAuditLog> findByResult(AuditResult result);
}

