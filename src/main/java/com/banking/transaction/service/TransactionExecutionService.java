
package com.banking.transaction.service;

import com.banking.transaction.entity.Transaction;
import com.banking.transaction.entity.TransactionType;

import com.banking.transaction.exception.AccountNotFoundException;
import com.banking.transaction.exception.DuplicateTransactionException;
import com.banking.transaction.exception.InsufficientBalanceException;
import com.banking.transaction.exception.InvalidTransactionException;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class TransactionExecutionService {

    private final TransactionService transactionService;
    private final FailureLoggingService failureLoggingService;

    public TransactionExecutionService(
            TransactionService transactionService,
            FailureLoggingService failureLoggingService) {

        this.transactionService = transactionService;
        this.failureLoggingService = failureLoggingService;
    }

    // Execute deposit with failure handling
    public Transaction executeDeposit(
            String reference,
            Long accountId,
            BigDecimal amount) {

        try {
            return transactionService.deposit(reference, accountId, amount);

        } catch (DuplicateTransactionException exception) {
            throw exception;

        } catch (RuntimeException exception) {

            logFailureSafely(
                    reference,
                    null,
                    accountId,
                    TransactionType.DEPOSIT,
                    amount,
                    exception
            );

            throw exception;
        }
    }

    // Execute withdrawal with failure handling
    public Transaction executeWithdrawal(
            String reference,
            Long accountId,
            BigDecimal amount) {

        try {
            return transactionService.withdraw(reference, accountId, amount);

        } catch (DuplicateTransactionException exception) {
            throw exception;

        } catch (RuntimeException exception) {

            logFailureSafely(
                    reference,
                    accountId,
                    null,
                    TransactionType.WITHDRAWAL,
                    amount,
                    exception
            );

            throw exception;
        }
    }

    // Execute transfer with failure handling
    public Transaction executeTransfer(
            String reference,
            Long fromAccountId,
            Long toAccountId,
            BigDecimal amount) {

        try {
            return transactionService.transfer(
                    reference, fromAccountId, toAccountId, amount);

        } catch (DuplicateTransactionException exception) {
            throw exception;

        } catch (RuntimeException exception) {

            logFailureSafely(
                    reference,
                    fromAccountId,
                    toAccountId,
                    TransactionType.TRANSFER,
                    amount,
                    exception
            );

            throw exception;
        }
    }

    // Save failure details without hiding the original error
    private void logFailureSafely(
            String reference,
            Long fromAccountId,
            Long toAccountId,
            TransactionType type,
            BigDecimal amount,
            RuntimeException exception) {

        if (reference == null || reference.isBlank()
                || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        String category = getErrorCategory(exception);

        String reason = exception.getMessage() == null
                ? "Transaction failed"
                : exception.getMessage();

        try {
            failureLoggingService.logFailure(
                   reference,
                   fromAccountId,
                   toAccountId,
                   type,
                   amount,
                   category,
                   reason
);

        } catch (RuntimeException loggingException) {
            // Preserve the original transaction exception.
        }
    }

    // Identify the failure category
    private String getErrorCategory(RuntimeException exception) {

        if (exception instanceof InsufficientBalanceException) {
            return "INSUFFICIENT_BALANCE";
        }

        if (exception instanceof AccountNotFoundException) {
            return "ACCOUNT_NOT_FOUND";
        }

        if (exception instanceof InvalidTransactionException) {
            return "INVALID_TRANSACTION";
        }

        return "PROCESSING_ERROR";
    }
}

