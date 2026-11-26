
USE banking_system;

-- Sample Customers
INSERT INTO customers
(first_name, last_name, email, phone)
VALUES
('Rahul', 'Sharma', 'rahul@example.com', '9876543210'),
('Priya', 'Verma', 'priya@example.com', '9876543211'),
('Amit', 'Kumar', 'amit@example.com', '9876543212'),
('Neha', 'Gupta', 'neha@example.com', '9876543213'),
('Rohit', 'Singh', 'rohit@example.com', '9876543214');

-- Sample Bank Accounts
INSERT INTO accounts
(customer_id, account_number, account_type, balance)
VALUES
(1, 'BANK100001', 'SAVINGS', 10000.00),
(1, 'BANK100002', 'CURRENT', 25000.00),
(2, 'BANK100003', 'SAVINGS', 15000.00),
(3, 'BANK100004', 'SAVINGS', 20000.00),
(4, 'BANK100005', 'SAVINGS', 5000.00),
(5, 'BANK100006', 'CURRENT', 30000.00);
