
package com.banking.transaction.repository;

import com.banking.transaction.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AccountRepository
        extends JpaRepository<Account, Long> {

    Optional<Account> findByAccountNumber(String accountNumber);

    List<Account> findByCustomerCustomerId(Long customerId);

    boolean existsByAccountNumber(String accountNumber);
}

