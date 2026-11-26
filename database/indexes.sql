
USE banking_system;

-- 1. Search accounts by customer ID
CREATE INDEX idx_accounts_customer
ON accounts(customer_id);

-- 2. Search transactions by sender account
CREATE INDEX idx_transactions_from_account
ON transactions(from_account_id);

-- 3. Search transactions by receiver account
CREATE INDEX idx_transactions_to_account
ON transactions(to_account_id);

-- 4. Filter transactions by status and date
CREATE INDEX idx_transactions_status_date
ON transactions(status, created_at);

-- 5. Search audit logs by transaction ID
CREATE INDEX idx_audit_transaction
ON transaction_audit_logs(transaction_id);

-- 6. Search audit logs by account ID
CREATE INDEX idx_audit_account
ON transaction_audit_logs(account_id);
