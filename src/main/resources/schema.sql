CREATE TABLE IF NOT EXISTS user (
        username VARCHAR(50) PRIMARY KEY,
        full_name VARCHAR(100) NOT NULL,
        password VARCHAR(300) NOT NULL,
        role ENUM('ADMIN', 'USER') NOT NULL
);

CREATE TABLE IF NOT EXISTS customer(
        id INT PRIMARY KEY AUTO_INCREMENT,
        name VARCHAR(100) NOT NULL,
        address VARCHAR(100) NOT NULL,
        contact VARCHAR(15) NOT NULL,
        picture MEDIUMBLOB NOT NULL,
        CONSTRAINT uk_contact UNIQUE (contact)
);

CREATE TABLE IF NOT EXISTS supplier(
        id INT PRIMARY KEY AUTO_INCREMENT,
        name VARCHAR(100) NOT NULL,
        address VARCHAR(100) NOT NULL,
        contact VARCHAR(15) NOT NULL,
        picture MEDIUMBLOB NOT NULL,
        CONSTRAINT uk_contact UNIQUE (contact)
);

CREATE TABLE IF NOT EXISTS item(
        code INT PRIMARY KEY AUTO_INCREMENT,
        description VARCHAR(100) NOT NULL,
        qty DECIMAL(8,2) NOT NULL,
        unit_of_measure VARCHAR(15) NOT NULL,
        alert_qty DECIMAL(8, 2) not null
);


CREATE TABLE IF NOT EXISTS product(
        code INT PRIMARY KEY AUTO_INCREMENT,
        description VARCHAR(100) NOT NULL,
        qty INT NOT NULL,
        selling_price DECIMAL(10,2) NOT NULL,
        picture MEDIUMBLOB NOT NULL
);


CREATE TABLE IF NOT EXISTS purchase_order(
        order_id INT PRIMARY KEY AUTO_INCREMENT,
        item_code INT NOT NULL ,
        unit_price DECIMAL(10,2) NOT NULL ,
        qty DECIMAL(8,2) NOT NULL,
        unit_of_measure VARCHAR(15) NOT NULL,
        supplier_id INT NOT NULL,
        order_date DATETIME NOT NULL,
        total DECIMAL(10,2) NOT NULL ,
        status enum('OPEN', 'CLOSED') DEFAULT 'OPEN',
        CONSTRAINT fk_purchase_order_item FOREIGN KEY (item_code) REFERENCES item(code),
        CONSTRAINT fk_purchase_order_supplier FOREIGN KEY (supplier_id) REFERENCES supplier(id)
);

CREATE TABLE IF NOT EXISTS sales_order(
        order_id INT PRIMARY KEY AUTO_INCREMENT,
        product_code INT NOT NULL,
        unit_price DECIMAL(10,2) NOT NULL ,
        qty INT NOT NULL,
        customer_id INT NOT NULL,
        order_date DATETIME NOT NULL,
        total DECIMAL(10,2) NOT NULL ,
        status enum('OPEN', 'CLOSED') DEFAULT 'OPEN',
        CONSTRAINT fk_sales_order_product FOREIGN KEY (product_code) REFERENCES product(code),
        CONSTRAINT fk_sales_order_customer FOREIGN KEY (customer_id) REFERENCES customer(id)
);