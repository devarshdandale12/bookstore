-- Reference schema for the Bookstore Management System (MySQL 8).
-- You do NOT need to run this: Spring Boot / Hibernate creates the tables automatically on first start
-- (spring.jpa.hibernate.ddl-auto=update). It is included so you can see and explain the database design.

CREATE DATABASE IF NOT EXISTS bookstore_db;
USE bookstore_db;

CREATE TABLE users (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  name       VARCHAR(100) NOT NULL,
  email      VARCHAR(150) NOT NULL UNIQUE,
  password   VARCHAR(255) NOT NULL,              -- BCrypt hash
  role       ENUM('CUSTOMER','ADMIN') NOT NULL,
  created_at DATETIME(6) NOT NULL
);

CREATE TABLE books (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  title          VARCHAR(200) NOT NULL,
  author         VARCHAR(200) NOT NULL,
  genre          VARCHAR(80)  NOT NULL,
  isbn           VARCHAR(20)  NOT NULL UNIQUE,
  price          DECIMAL(10,2) NOT NULL,
  description    VARCHAR(2000),
  stock_quantity INT NOT NULL,
  image_url      VARCHAR(500),
  created_at     DATETIME(6) NOT NULL,
  INDEX idx_books_genre (genre),
  INDEX idx_books_title (title)
);

CREATE TABLE orders (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id        BIGINT NOT NULL,
  total_amount   DECIMAL(12,2) NOT NULL,
  status         ENUM('PENDING','SHIPPED','DELIVERED','CANCELLED') NOT NULL,
  payment_status ENUM('PENDING','PAID','REFUNDED') NOT NULL,
  payment_method ENUM('CARD','UPI','COD') NOT NULL,
  created_at     DATETIME(6) NOT NULL,
  FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE order_items (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id   BIGINT NOT NULL,
  book_id    BIGINT NOT NULL,
  quantity   INT NOT NULL,
  unit_price DECIMAL(10,2) NOT NULL,              -- price at the time of purchase
  FOREIGN KEY (order_id) REFERENCES orders(id),
  FOREIGN KEY (book_id)  REFERENCES books(id)
);

CREATE TABLE reviews (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  book_id      BIGINT NOT NULL,
  user_id      BIGINT NOT NULL,
  rating       INT NOT NULL,
  comment_text VARCHAR(1000),
  created_at   DATETIME(6) NOT NULL,
  UNIQUE KEY uk_review_book_user (book_id, user_id),
  FOREIGN KEY (book_id) REFERENCES books(id),
  FOREIGN KEY (user_id) REFERENCES users(id)
);
-- Relationships: users 1-N orders, orders 1-N order_items, books 1-N order_items, books 1-N reviews, users 1-N reviews
