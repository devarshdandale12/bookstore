package com.devarsh.bookstore.service;

import com.devarsh.bookstore.dto.BookRequest;
import com.devarsh.bookstore.dto.BookResponse;
import com.devarsh.bookstore.dto.PageResponse;
import com.devarsh.bookstore.entity.Book;
import com.devarsh.bookstore.exception.BadRequestException;
import com.devarsh.bookstore.exception.ConflictException;
import com.devarsh.bookstore.exception.ResourceNotFoundException;
import com.devarsh.bookstore.repository.BookRepository;
import com.devarsh.bookstore.repository.ReviewRepository;
import com.devarsh.bookstore.util.Paging;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BookService {

    /** Only these fields may be used for sorting (protects against invalid / unsafe sort values). */
    private static final Set<String> SORTABLE =
            Set.of("id", "title", "author", "genre", "price", "stockQuantity", "createdAt");

    private final BookRepository books;
    private final ReviewRepository reviews;

    @Transactional(readOnly = true)
    public PageResponse<BookResponse> list(int page, int size, String search, String genre,
                                           String sortBy, String direction) {
        if (!SORTABLE.contains(sortBy)) {
            throw new BadRequestException(
                    "Cannot sort by '" + sortBy + "'. Allowed: id, title, author, genre, price, stockQuantity, createdAt");
        }
        Sort sort = "desc".equalsIgnoreCase(direction) ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Page<Book> result = books.searchBooks(
                search == null ? "" : search.trim(),
                genre == null ? "" : genre.trim(),
                Paging.of(page, size, sort));
        return PageResponse.from(result.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public List<String> genres() {
        return books.findAllGenres();
    }

    @Transactional(readOnly = true)
    public BookResponse get(Long id) {
        return toResponse(findEntity(id));
    }

    @Transactional
    public BookResponse create(BookRequest req) {
        String isbn = req.isbn().trim();
        if (books.existsByIsbn(isbn)) {
            throw new ConflictException("A book with ISBN " + isbn + " already exists");
        }
        Book book = new Book();
        apply(book, req);
        return toResponse(books.save(book));
    }

    @Transactional
    public BookResponse update(Long id, BookRequest req) {
        Book book = findEntity(id);
        String isbn = req.isbn().trim();
        if (books.existsByIsbnAndIdNot(isbn, id)) {
            throw new ConflictException("Another book already uses ISBN " + isbn);
        }
        apply(book, req);
        return toResponse(book);   // saved automatically when the transaction ends
    }

    @Transactional
    public void delete(Long id) {
        Book book = findEntity(id);
        books.delete(book);
        books.flush();   // so a foreign-key problem (book already ordered) is reported right now as 409
    }

    public Book findEntity(Long id) {
        return books.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id " + id));
    }

    private void apply(Book b, BookRequest r) {
        b.setTitle(r.title().trim());
        b.setAuthor(r.author().trim());
        b.setGenre(r.genre().trim());
        b.setIsbn(r.isbn().trim());
        b.setPrice(r.price());
        b.setDescription(blankToNull(r.description()));
        b.setStockQuantity(r.stockQuantity());
        b.setImageUrl(blankToNull(r.imageUrl()));
    }

    private String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private BookResponse toResponse(Book b) {
        Double avg = reviews.averageRating(b.getId());
        Double rounded = avg == null ? null : Math.round(avg * 10) / 10.0;
        return new BookResponse(b.getId(), b.getTitle(), b.getAuthor(), b.getGenre(), b.getIsbn(), b.getPrice(),
                b.getDescription(), b.getStockQuantity(), b.getImageUrl(), rounded, reviews.countByBookId(b.getId()));
    }
}
