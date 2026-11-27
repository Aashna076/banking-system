
package com.banking.transaction.service;

import com.banking.transaction.entity.Account;
import com.banking.transaction.repository.AccountRepository;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    // Find account using account ID
    public Optional<Account> getAccountById(Long accountId) {
        return accountRepository.findById(accountId);
    }

    // Find account using account number
    public Optional<Account> getAccountByNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber);
    }

    // Get all accounts
    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    // Get all accounts belonging to a customer
    public List<Account> getAccountsByCustomer(Long customerId) {
        return accountRepository.findByCustomerCustomerId(customerId);
    }
}
