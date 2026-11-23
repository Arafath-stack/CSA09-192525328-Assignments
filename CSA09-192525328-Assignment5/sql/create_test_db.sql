-- Local TEST database created only for this assignment (run as a MySQL admin user)
CREATE DATABASE IF NOT EXISTS buspass_test;
CREATE USER IF NOT EXISTS 'bususer'@'localhost' IDENTIFIED BY 'buspass123';
GRANT ALL PRIVILEGES ON buspass_test.* TO 'bususer'@'localhost';
FLUSH PRIVILEGES;

USE buspass_test;

-- Task B2: users table for the login experiment
CREATE TABLE IF NOT EXISTS users (
    id  VARCHAR(30) PRIMARY KEY,
    pwd VARCHAR(60) NOT NULL
) ENGINE=InnoDB;

-- Tasks B3 / B4: bus pass table
CREATE TABLE IF NOT EXISTS bus_pass (
    pass_id  VARCHAR(10) PRIMARY KEY,
    reg_no   VARCHAR(15) NOT NULL,
    name     VARCHAR(60) NOT NULL,
    route_no INT         NOT NULL,
    amount   DECIMAL(10,2) NOT NULL
) ENGINE=InnoDB;
