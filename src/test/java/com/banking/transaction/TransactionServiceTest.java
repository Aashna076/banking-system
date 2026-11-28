
package com.banking.transaction;

import com.banking.transaction.entity.Account;
import com.banking.transaction.entity.AccountStatus;
import com.banking.transaction.entity.Transaction;
import com.banking.transaction.entity.TransactionStatus;
import com.banking.transaction.entity.TransactionType;
import com.banking.transaction.entity.TransactionAuditLog;
import com.banking.transaction.entity.AuditResult;

import com.banking.transaction.exception.InsufficientBalanceException;

import com.banking.transaction.repository.AccountRepository;
import com.banking.transaction.repository.TransactionRepository;
import com.banking.transaction.repository.TransactionAuditLogRepository;

import com.banking.transaction.service.TransactionService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionAuditLogRepository auditLogRepository;

    @InjectMocks
    private TransactionService transactionService;

    private Account account;

    @BeforeEach
    void setUp() {
        account = new Account();
        account.setAccountId(1L);
        account.setAccountNumber("ACC1001");
        account.setBalance(new BigDecimal("10000.00"));
        account.setStatus(AccountStatus.ACTIVE);
    }

    @Test
    void depositShouldIncreaseBalance() {

        when(transactionRepository.existsByTransactionReference("TXN101"))
                .thenReturn(false);

        when(accountRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(account));

        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Transaction result = transactionService.deposit(
                "TXN101", 1L, new BigDecimal("2000.00"));

        assertEquals(new BigDecimal("12000.00"), account.getBalance());
        assertEquals(TransactionType.DEPOSIT, result.getTransactionType());
        assertEquals(TransactionStatus.SUCCESS, result.getStatus());

        verify(auditLogRepository, times(1))
                .save(any(TransactionAuditLog.class));
    }

    @Test
    void withdrawalShouldDecreaseBalance() {

        when(transactionRepository.existsByTransactionReference("TXN102"))
                .thenReturn(false);

        when(accountRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(account));

        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Transaction result = transactionService.withdraw(
                "TXN102", 1L, new BigDecimal("3000.00"));

        assertEquals(new BigDecimal("7000.00"), account.getBalance());
        assertEquals(TransactionType.WITHDRAWAL, result.getTransactionType());
        assertEquals(TransactionStatus.SUCCESS, result.getStatus());

        verify(auditLogRepository, times(1))
                .save(any(TransactionAuditLog.class));
    }

    @Test
    void withdrawalShouldFailWhenBalanceIsInsufficient() {

        when(transactionRepository.existsByTransactionReference("TXN103"))
                .thenReturn(false);

        when(accountRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(account));

        assertThrows(
                InsufficientBalanceException.class,
                () -> transactionService.withdraw(
                        "TXN103", 1L, new BigDecimal("15000.00"))
        );

        assertEquals(new BigDecimal("10000.00"), account.getBalance());

        verify(transactionRepository, never()).save(any(Transaction.class));
        verify(auditLogRepository, never()).save(any(TransactionAuditLog.class));
    }
    @Test
void duplicateTransactionShouldBeRejected() {

    when(transactionRepository.existsByTransactionReference("TXN104"))
            .thenReturn(true);

    assertThrows(
            com.banking.transaction.exception.DuplicateTransactionException.class,
            () -> transactionService.deposit(
                    "TXN104", 1L, new BigDecimal("1000.00"))
    );

    verify(accountRepository, never()).findByIdForUpdate(anyLong());
}

@Test
void shouldThrowExceptionWhenAccountDoesNotExist() {

    when(transactionRepository.existsByTransactionReference("TXN105"))
            .thenReturn(false);

    when(accountRepository.findByIdForUpdate(99L))
            .thenReturn(Optional.empty());

    assertThrows(
            com.banking.transaction.exception.AccountNotFoundException.class,
            () -> transactionService.deposit(
                    "TXN105", 99L, new BigDecimal("1000.00"))
    );
}
@Test
void transferShouldUpdateBothAccountBalances() {

    Account destinationAccount = new Account();
    destinationAccount.setAccountId(2L);
    destinationAccount.setAccountNumber("ACC1002");
    destinationAccount.setBalance(new BigDecimal("5000.00"));
    destinationAccount.setStatus(AccountStatus.ACTIVE);

    when(transactionRepository.existsByTransactionReference("TXN106"))
            .thenReturn(false);

    when(accountRepository.findByIdForUpdate(1L))
            .thenReturn(Optional.of(account));

    when(accountRepository.findByIdForUpdate(2L))
            .thenReturn(Optional.of(destinationAccount));

    when(transactionRepository.save(any(Transaction.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

    Transaction result = transactionService.transfer(
            "TXN106",
            1L,
            2L,
            new BigDecimal("2000.00")
    );

    assertEquals(new BigDecimal("8000.00"), account.getBalance());
    assertEquals(new BigDecimal("7000.00"), destinationAccount.getBalance());

    assertEquals(TransactionType.TRANSFER, result.getTransactionType());
    assertEquals(TransactionStatus.SUCCESS, result.getStatus());

    verify(auditLogRepository, times(2))
            .save(any(TransactionAuditLog.class));
}
}

