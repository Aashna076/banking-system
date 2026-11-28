
package com.banking.transaction.service;

import com.banking.transaction.entity.Account;
import com.banking.transaction.entity.AccountStatus;
import com.banking.transaction.entity.Transaction;
import com.banking.transaction.entity.TransactionStatus;
import com.banking.transaction.entity.TransactionType;
import com.banking.transaction.entity.TransactionAuditLog;
import com.banking.transaction.entity.AuditResult;

import com.banking.transaction.exception.AccountNotFoundException;
import com.banking.transaction.exception.DuplicateTransactionException;
import com.banking.transaction.exception.InsufficientBalanceException;
import com.banking.transaction.exception.InvalidTransactionException;

import com.banking.transaction.repository.AccountRepository;
import com.banking.transaction.repository.TransactionRepository;
import com.banking.transaction.repository.TransactionAuditLogRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionAuditLogRepository auditLogRepository;

    public TransactionService(
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            TransactionAuditLogRepository auditLogRepository) {

        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.auditLogRepository = auditLogRepository;
    }

    // Deposit money into an account
    @Transactional
    public Transaction deposit(
            String transactionReference,
            Long accountId,
            BigDecimal amount) {

        long startTime = System.nanoTime();

        validateTransaction(transactionReference, amount);
        checkDuplicateTransaction(transactionReference);

        Account account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() ->
                        new AccountNotFoundException(
                                "Account not found: " + accountId));

        validateActiveAccount(account);

        account.setBalance(account.getBalance().add(amount));

        Transaction transaction = new Transaction();
        transaction.setTransactionReference(transactionReference);
        transaction.setToAccount(account);
        transaction.setTransactionType(TransactionType.DEPOSIT);
        transaction.setAmount(amount);
        transaction.setStatus(TransactionStatus.SUCCESS);

        Transaction savedTransaction = transactionRepository.save(transaction);

        saveAuditLog(
                savedTransaction,
                account,
                "DEPOSIT",
                AuditResult.SUCCESS,
                null,
                getDuration(startTime)
        );

        return savedTransaction;
    }

    // Withdraw money from an account
    @Transactional
    public Transaction withdraw(
            String transactionReference,
            Long accountId,
            BigDecimal amount) {

        long startTime = System.nanoTime();

        validateTransaction(transactionReference, amount);
        checkDuplicateTransaction(transactionReference);

        Account account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() ->
                        new AccountNotFoundException(
                                "Account not found: " + accountId));

        validateActiveAccount(account);

        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException(
                    "Insufficient balance in account: " + accountId);
        }

        account.setBalance(account.getBalance().subtract(amount));

        Transaction transaction = new Transaction();
        transaction.setTransactionReference(transactionReference);
        transaction.setFromAccount(account);
        transaction.setTransactionType(TransactionType.WITHDRAWAL);
        transaction.setAmount(amount);
        transaction.setStatus(TransactionStatus.SUCCESS);

        Transaction savedTransaction = transactionRepository.save(transaction);

        saveAuditLog(
                savedTransaction,
                account,
                "WITHDRAWAL",
                AuditResult.SUCCESS,
                null,
                getDuration(startTime)
        );

        return savedTransaction;
    }

    // Transfer money between two accounts
    @Transactional
    public Transaction transfer(
            String transactionReference,
            Long fromAccountId,
            Long toAccountId,
            BigDecimal amount) {

        long startTime = System.nanoTime();

        validateTransaction(transactionReference, amount);

        if (fromAccountId == null || toAccountId == null
                || fromAccountId.equals(toAccountId)) {
            throw new InvalidTransactionException(
                    "Source and destination accounts must be different");
        }

        checkDuplicateTransaction(transactionReference);

        // Lock accounts in ascending ID order to prevent deadlocks
        Long firstAccountId = Math.min(fromAccountId, toAccountId);
        Long secondAccountId = Math.max(fromAccountId, toAccountId);

        Account firstAccount = accountRepository
                .findByIdForUpdate(firstAccountId)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Account not found: " + firstAccountId));

        Account secondAccount = accountRepository
                .findByIdForUpdate(secondAccountId)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Account not found: " + secondAccountId));

        Account fromAccount = fromAccountId.equals(firstAccountId)
                ? firstAccount : secondAccount;

        Account toAccount = toAccountId.equals(firstAccountId)
                ? firstAccount : secondAccount;

        validateActiveAccount(fromAccount);
        validateActiveAccount(toAccount);

        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException(
                    "Insufficient balance in source account: "
                            + fromAccountId);
        }

        // Update both account balances
        fromAccount.setBalance(
                fromAccount.getBalance().subtract(amount));

        toAccount.setBalance(
                toAccount.getBalance().add(amount));

        Transaction transaction = new Transaction();

        transaction.setTransactionReference(transactionReference);
        transaction.setFromAccount(fromAccount);
        transaction.setToAccount(toAccount);
        transaction.setTransactionType(TransactionType.TRANSFER);
        transaction.setAmount(amount);
        transaction.setStatus(TransactionStatus.SUCCESS);

        Transaction savedTransaction = transactionRepository.save(transaction);

        saveAuditLog(
                savedTransaction,
                fromAccount,
                "TRANSFER",
                AuditResult.SUCCESS,
                null,
                getDuration(startTime)
        );

        saveAuditLog(
                savedTransaction,
                toAccount,
                "TRANSFER",
                AuditResult.SUCCESS,
                null,
                getDuration(startTime)
        );

        return savedTransaction;
    }

    // Save transaction audit details
    private void saveAuditLog(
            Transaction transaction,
            Account account,
            String action,
            AuditResult result,
            String reason,
            long durationMs) {

        TransactionAuditLog auditLog = new TransactionAuditLog();

        auditLog.setTransaction(transaction);
        auditLog.setAccount(account);
        auditLog.setAction(action);
        auditLog.setResult(result);
        auditLog.setReason(reason);
        auditLog.setThreadName(Thread.currentThread().getName());
        auditLog.setDurationMs(durationMs);

        auditLogRepository.save(auditLog);
    }

    // Calculate processing duration in milliseconds
    private long getDuration(long startTime) {
        return (System.nanoTime() - startTime) / 1_000_000;
    }

    // Validate transaction reference and amount
    private void validateTransaction(
            String transactionReference,
            BigDecimal amount) {

        if (transactionReference == null ||
                transactionReference.isBlank()) {

            throw new InvalidTransactionException(
                    "Transaction reference cannot be empty");
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException(
                    "Transaction amount must be greater than zero");
        }
    }

    // Check duplicate transaction reference
    private void checkDuplicateTransaction(String reference) {

        if (transactionRepository.existsByTransactionReference(reference)) {
            throw new DuplicateTransactionException(
                    "Duplicate transaction reference: " + reference);
        }
    }

    // Validate account status
    private void validateActiveAccount(Account account) {

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new InvalidTransactionException(
                    "Account is not active: " + account.getAccountNumber());
        }
    }
}

