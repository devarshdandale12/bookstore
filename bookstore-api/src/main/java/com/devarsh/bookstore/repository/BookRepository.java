package com.devarsh.bookstore.repository;

import com.devarsh.bookstore.entity.Book;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long> {

    /** Search by title or author and/or filter by genre. Empty string means "no filter". */
    @Query("""
            select b from Book b
            where (:search = '' or lower(b.title) like lower(concat('%', :search, '%'))
                                or lower(b.author) like lower(concat('%', :search, '%')))
              and (:genre = '' or lower(b.genre) = lower(:genre))
            """)
    Page<Book> searchBooks(@Param("search") String search,
                           @Param("genre") String genre,
                           Pageable pageable);

    boolean existsByIsbn(String isbn);

    boolean existsByIsbnAndIdNot(String isbn, Long id);

    /** Locks the row while an order is being placed so two customers cannot buy the last copy together. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Book b where b.id = :id")
    Optional<Book> findByIdForUpdate(@Param("id") Long id);

    @Query("select distinct b.genre from Book b order by b.genre")
    List<String> findAllGenres();
}
