CREATE DATABASE inventory;
GO

USE inventory

WAITFOR DELAY '00:00:30'

EXEC sys.sp_cdc_enable_db

CREATE TABLE customers (
    id INT PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL
)

CREATE TABLE products (
    id INT PRIMARY KEY,
    name VARCHAR(100),
    description TEXT,
    weight BIGINT
)

CREATE TABLE orders (
    id INT PRIMARY KEY,
    order_date date NOT NULL,
    purchaser integer NOT NULL,
    quantity integer NOT NULL,
    product_id integer NOT NULL,
    FOREIGN KEY (purchaser) REFERENCES customers(id),
    FOREIGN KEY (product_id) REFERENCES products(id)
)

INSERT INTO customers (id, first_name, last_name, email) VALUES
(1,'Alice', 'Smith', 'alice.smith@example.com'),
(2,'Bob', 'Johnson', 'bob.johnson@example.com'),
(3,'Charlie', 'Brown', 'charlie.brown@example.com');

INSERT INTO products (id, name, description, weight) VALUES
(1, 'Laptop', 'High-performance ultrabook', 1250),
(2, 'Smartphone', 'Latest model with AMOLED display', 180),
(3, 'Coffee Mug', 'Ceramic mug with lid', 350);

INSERT INTO orders (id, order_date, purchaser, quantity, product_id) VALUES
(1,'2025-07-01', 1, 1, 2),
(2,'2025-07-02', 2, 2, 1),
(3,'2025-07-03', 3, 1, 3);


EXEC sys.sp_cdc_enable_table @source_schema = 'dbo', @source_name = 'products', @role_name = NULL, @supports_net_changes = 0
EXEC sys.sp_cdc_enable_table @source_schema = 'dbo', @source_name = 'orders', @role_name = NULL, @supports_net_changes = 0
EXEC sys.sp_cdc_enable_table @source_schema = 'dbo', @source_name = 'customers', @role_name = NULL, @supports_net_changes = 0