
package com.banking.transaction.service;

import com.banking.transaction.entity.Account;
import com.banking.transaction.entity.FailedTransaction;
import com.banking.transaction.entity.Transaction;
import com.banking.transaction.entity.TransactionStatus;
import com.banking.transaction.entity.TransactionType;

import com.banking.transaction.repository.AccountRepository;
import com.banking.transaction.repository.FailedTransactionRepository;
import com.banking.transaction.repository.TransactionRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class FailureLoggingService {

    private final FailedTransactionRepository failedTransactionRepository;
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    public FailureLoggingService(
            FailedTransactionRepository failedTransactionRepository,
            TransactionRepository transactionRepository,
            AccountRepository accountRepository) {

        this.failedTransactionRepository = failedTransactionRepository;
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
    }

    // Save failed transaction independently
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logFailure(
            String transactionReference,
            Long fromAccountId,
            Long toAccountId,
            TransactionType transactionType,
            BigDecimal amount,
            String errorCategory,
            String reason) {

        // Retrieve accounts if they exist
        Account fromAccount = fromAccountId == null
                ? null
                : accountRepository.findById(fromAccountId).orElse(null);

        Account toAccount = toAccountId == null
                ? null
                : accountRepository.findById(toAccountId).orElse(null);

        // Create failed transaction record
        Transaction transaction = new Transaction();

        transaction.setTransactionReference(transactionReference);
        transaction.setFromAccount(fromAccount);
        transaction.setToAccount(toAccount);
        transaction.setTransactionType(transactionType);
        transaction.setAmount(amount);
        transaction.setStatus(TransactionStatus.FAILED);
        transaction.setFailureReason(reason);

        Transaction savedTransaction =
                transactionRepository.save(transaction);

        // Save failure details
        FailedTransaction failedTransaction = new FailedTransaction();

        failedTransaction.setTransaction(savedTransaction);
        failedTransaction.setErrorCategory(errorCategory);
        failedTransaction.setReason(reason);

        failedTransactionRepository.save(failedTransaction);
    }
}
