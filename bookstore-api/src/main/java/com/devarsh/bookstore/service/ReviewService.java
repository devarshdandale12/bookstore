package com.devarsh.bookstore.service;

import com.devarsh.bookstore.dto.ReviewRequest;
import com.devarsh.bookstore.dto.ReviewResponse;
import com.devarsh.bookstore.entity.Book;
import com.devarsh.bookstore.entity.Review;
import com.devarsh.bookstore.entity.User;
import com.devarsh.bookstore.repository.ReviewRepository;
import com.devarsh.bookstore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Optional enhancement: star ratings and comments. One review per user per book (posting again updates it). */
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviews;
    private final UserRepository users;
    private final BookService bookService;

    @Transactional(readOnly = true)
    public List<ReviewResponse> list(Long bookId) {
        bookService.findEntity(bookId);   // 404 if the book does not exist
        return reviews.findByBookIdOrderByCreatedAtDesc(bookId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public ReviewResponse addOrUpdate(Long bookId, ReviewRequest req, String email) {
        Book book = bookService.findEntity(bookId);
        User user = users.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("This account no longer exists"));
        Review review = reviews.findByBookIdAndUserId(bookId, user.getId()).orElseGet(Review::new);
        review.setBook(book);
        review.setUser(user);
        review.setRating(req.rating());
        review.setComment(req.comment() == null || req.comment().isBlank() ? null : req.comment().trim());
        return toResponse(reviews.save(review));
    }

    private ReviewResponse toResponse(Review r) {
        return new ReviewResponse(r.getId(), r.getUser().getName(), r.getRating(), r.getComment(), r.getCreatedAt());
    }
}
