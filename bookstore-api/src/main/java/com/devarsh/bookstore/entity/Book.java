package com.devarsh.bookstore.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "books", indexes = {
        @Index(name = "idx_books_genre", columnList = "genre"),
        @Index(name = "idx_books_title", columnList = "title")
})
@Getter
@Setter
@NoArgsConstructor
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    /** One or more authors, separated by commas. */
    @Column(nullable = false, length = 200)
    private String author;

    @Column(nullable = false, length = 80)
    private String genre;

    @Column(nullable = false, unique = true, length = 20)
    private String isbn;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private int stockQuantity;

    @Column(length = 500)
    private String imageUrl;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /** Deleting a book also deletes its reviews. */
    @OneToMany(mappedBy = "book", cascade = CascadeType.REMOVE)
    private List<Review> reviews = new ArrayList<>();
}
