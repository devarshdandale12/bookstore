package com.devarsh.bookstore.repository;

import com.devarsh.bookstore.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByBookIdOrderByCreatedAtDesc(Long bookId);

    Optional<Review> findByBookIdAndUserId(Long bookId, Long userId);

    long countByBookId(Long bookId);

    @Query("select avg(r.rating) from Review r where r.book.id = :bookId")
    Double averageRating(@Param("bookId") Long bookId);
}
