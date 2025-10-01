-- Migration script for FIFO Inventory Management
-- Create tables for inventory lots and lot consumptions

-- Create inventory_lots table for FIFO tracking
CREATE TABLE inventory_lots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    supplier_id BIGINT,
    inbound_transaction_id BIGINT,
    initial_quantity INT NOT NULL,
    remaining_quantity INT NOT NULL,
    unit_cost DECIMAL(19,2) NOT NULL,
    batch_number VARCHAR(255),
    received_date DATETIME NOT NULL,
    expiry_date DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    FOREIGN KEY (supplier_id) REFERENCES suppliers(id) ON DELETE SET NULL,
    FOREIGN KEY (inbound_transaction_id) REFERENCES transactions(id) ON DELETE SET NULL,

    INDEX idx_product_remaining (product_id, remaining_quantity),
    INDEX idx_received_date (received_date),
    INDEX idx_expiry_date (expiry_date)
);

-- Create lot_consumptions table for tracking FIFO consumption
CREATE TABLE lot_consumptions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    inventory_lot_id BIGINT NOT NULL,
    outbound_transaction_id BIGINT NOT NULL,
    consumed_quantity INT NOT NULL,
    unit_cost DECIMAL(19,2) NOT NULL,
    unit_sale_price DECIMAL(19,2) NOT NULL,
    total_cost DECIMAL(19,2) NOT NULL,
    total_revenue DECIMAL(19,2) NOT NULL,
    total_profit DECIMAL(19,2) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (inventory_lot_id) REFERENCES inventory_lots(id) ON DELETE CASCADE,
    FOREIGN KEY (outbound_transaction_id) REFERENCES transactions(id) ON DELETE CASCADE,

    INDEX idx_lot_consumption (inventory_lot_id),
    INDEX idx_transaction_consumption (outbound_transaction_id),
    INDEX idx_created_at (created_at)
);
