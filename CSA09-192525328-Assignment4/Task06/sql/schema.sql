-- Campus Event Registration: MySQL 8 schema and sample data
CREATE DATABASE IF NOT EXISTS campus_events CHARACTER SET utf8mb4;
USE campus_events;

CREATE TABLE IF NOT EXISTS events (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(80) NOT NULL UNIQUE,
    event_date DATE        NOT NULL
);

CREATE TABLE IF NOT EXISTS registrations (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    reg_no        CHAR(9)      NOT NULL,
    student_name  VARCHAR(60)  NOT NULL,
    email         VARCHAR(100) NOT NULL,
    event_id      INT          NOT NULL,
    registered_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reg_event FOREIGN KEY (event_id) REFERENCES events(id),
    -- the database itself refuses a second registration of the same student for the same event
    CONSTRAINT uq_student_event UNIQUE (reg_no, event_id),
    INDEX idx_event_id (event_id, id)
);

INSERT IGNORE INTO events (name, event_date) VALUES
    ('Inaugural Dance Night', '2026-12-05'), ('Battle of Bands', '2026-12-06'),
    ('Drama Fest', '2026-12-07'), ('Quiz Marathon', '2026-12-08');

INSERT IGNORE INTO registrations (reg_no, student_name, email, event_id) VALUES
    ('192311001', 'Arun Kumar',  'arun@college.edu',  1),
    ('192311002', 'Divya S',     'divya@college.edu', 1),
    ('192311001', 'Arun Kumar',  'arun@college.edu',  2),
    ('192311003', 'Karthik R',   'karthik@college.edu', 3);

-- useful checks
-- SELECT COUNT(*) FROM registrations;                                         -- 4
-- INSERT INTO registrations (reg_no, student_name, email, event_id) VALUES ('192311001','Arun Kumar','arun@college.edu',1);   -- fails with error 1062 (duplicate)
