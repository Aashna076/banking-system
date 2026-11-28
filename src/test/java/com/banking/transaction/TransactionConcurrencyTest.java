
package com.banking.transaction;

import com.banking.transaction.entity.Account;
import com.banking.transaction.entity.AccountStatus;
import com.banking.transaction.entity.AccountType;
import com.banking.transaction.entity.Customer;
import com.banking.transaction.entity.CustomerStatus;

import com.banking.transaction.exception.InsufficientBalanceException;

import com.banking.transaction.repository.AccountRepository;
import com.banking.transaction.repository.CustomerRepository;

import com.banking.transaction.service.TransactionExecutionService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class TransactionConcurrencyTest {

    @Autowired
    private TransactionExecutionService executionService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CustomerRepository customerRepository;

    private Long accountId;

    @BeforeEach
    void setUp() {

        // Create a unique customer for every test run.
        Customer customer = new Customer();

        customer.setFirstName("Concurrency");
        customer.setLastName("Test");
        customer.setEmail(UUID.randomUUID() + "@test.com");
        customer.setPhone("9999999999");
        customer.setStatus(CustomerStatus.ACTIVE);

        customer = customerRepository.saveAndFlush(customer);

        // Create a fresh account with Rs. 10,000 balance.
        Account account = new Account();

        account.setCustomer(customer);
       account.setAccountNumber("C" + UUID.randomUUID().toString().replace("-", "").substring(0, 10));
        account.setAccountType(AccountType.SAVINGS);
        account.setBalance(new BigDecimal("10000.00"));
        account.setStatus(AccountStatus.ACTIVE);

        account = accountRepository.saveAndFlush(account);

        accountId = account.getAccountId();
    }

    @Test
    void concurrentWithdrawalsShouldNotCauseNegativeBalance()
            throws Exception {

        // Generate unique references for every execution.
        String testId = UUID.randomUUID().toString();

        String reference7000 = "CONC-7000-" + testId;
        String reference5000 = "CONC-5000-" + testId;

        // Create two worker threads.
        ExecutorService executor = Executors.newFixedThreadPool(2);

        // Synchronize both threads before starting withdrawals.
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {

            Future<Boolean> withdrawal7000 = executor.submit(() -> {

                ready.countDown();
                start.await();

                try {
                    executionService.executeWithdrawal(
                            reference7000,
                            accountId,
                            new BigDecimal("7000.00")
                    );

                    return true;

                } catch (InsufficientBalanceException exception) {

                    return false;
                }
            });

            Future<Boolean> withdrawal5000 = executor.submit(() -> {

                ready.countDown();
                start.await();

                try {
                    executionService.executeWithdrawal(
                            reference5000,
                            accountId,
                            new BigDecimal("5000.00")
                    );

                    return true;

                } catch (InsufficientBalanceException exception) {

                    return false;
                }
            });

            // Wait until both threads are ready.
            assertTrue(
                    ready.await(10, TimeUnit.SECONDS),
                    "Both threads should be ready"
            );

            // Start both withdrawals together.
            start.countDown();

            boolean firstSucceeded =
                    withdrawal7000.get(30, TimeUnit.SECONDS);

            boolean secondSucceeded =
                    withdrawal5000.get(30, TimeUnit.SECONDS);

            // Exactly one withdrawal must succeed.
            assertNotEquals(
                    firstSucceeded,
                    secondSucceeded,
                    "Exactly one withdrawal should succeed"
            );

            // Read the final account balance from MySQL.
            Account updatedAccount =
                    accountRepository.findById(accountId).orElseThrow();

            BigDecimal finalBalance = updatedAccount.getBalance();

            // Balance must never become negative.
            assertTrue(
                    finalBalance.compareTo(BigDecimal.ZERO) >= 0,
                    "Account balance must never become negative"
            );

            // Final balance must be either Rs. 3,000 or Rs. 5,000.
            assertTrue(
                    finalBalance.equals(new BigDecimal("3000.00"))
                    || finalBalance.equals(new BigDecimal("5000.00")),
                    "Final balance should be 3000 or 5000, but was: "
                            + finalBalance
            );

        } finally {

            // Release waiting threads if an assertion fails.
            start.countDown();

            executor.shutdownNow();

            executor.awaitTermination(10, TimeUnit.SECONDS);
        }
    }
}
