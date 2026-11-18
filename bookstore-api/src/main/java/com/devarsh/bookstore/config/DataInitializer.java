package com.devarsh.bookstore.config;

import com.devarsh.bookstore.entity.Book;
import com.devarsh.bookstore.entity.Role;
import com.devarsh.bookstore.entity.User;
import com.devarsh.bookstore.repository.BookRepository;
import com.devarsh.bookstore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/** Runs once at start-up: creates the first admin and some sample books (only if the tables are empty). */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository users;
    private final BookRepository books;
    private final PasswordEncoder encoder;

    @Value("${app.admin.name}")
    private String adminName;
    @Value("${app.admin.email}")
    private String adminEmail;
    @Value("${app.admin.password}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (!users.existsByRole(Role.ADMIN)) {
            User admin = new User();
            admin.setName(adminName);
            admin.setEmail(adminEmail.toLowerCase());
            admin.setPassword(encoder.encode(adminPassword));
            admin.setRole(Role.ADMIN);
            users.save(admin);
            log.info("Created default admin account: {}", adminEmail);
        }
        if (books.count() == 0) {
            books.saveAll(List.of(
                    book("Clean Code", "Robert C. Martin", "Programming", "9780132350884", "599.00", 25,
                            "A handbook of agile software craftsmanship: how to write code that is readable and easy to change."),
                    book("Effective Java", "Joshua Bloch", "Programming", "9780134685991", "749.00", 18,
                            "Practical best practices for the Java platform, from the architect of the Java Collections Framework."),
                    book("The Pragmatic Programmer", "Andrew Hunt, David Thomas", "Programming", "9780135957059", "699.00", 14,
                            "Tips and habits that help developers grow from journeyman to master."),
                    book("Head First Java", "Kathy Sierra, Bert Bates", "Programming", "9780596009205", "549.00", 30,
                            "A visual, beginner-friendly introduction to Java and object-oriented thinking."),
                    book("Atomic Habits", "James Clear", "Self-help", "9780735211292", "399.00", 40,
                            "Small changes, remarkable results: a proven way to build good habits and break bad ones."),
                    book("Deep Work", "Cal Newport", "Self-help", "9781455586691", "429.00", 4,
                            "Rules for focused success in a distracted world."),
                    book("The Alchemist", "Paulo Coelho", "Fiction", "9780062315007", "299.00", 50,
                            "A shepherd boy travels from Spain to Egypt in search of treasure and finds his personal legend."),
                    book("1984", "George Orwell", "Fiction", "9780451524935", "249.00", 22,
                            "A chilling story of surveillance, propaganda and the loss of truth."),
                    book("Sapiens", "Yuval Noah Harari", "History", "9780062316097", "499.00", 16,
                            "A brief history of humankind, from the Stone Age to the present."),
                    book("Wings of Fire", "A. P. J. Abdul Kalam, Arun Tiwari", "Biography", "9788173711466", "275.00", 35,
                            "The autobiography of India's missile man and former President."),
                    book("The Psychology of Money", "Morgan Housel", "Finance", "9780857197689", "379.00", 0,
                            "Timeless lessons on wealth, greed and happiness.")
            ));
            log.info("Inserted sample books");
        }
    }

    private Book book(String title, String author, String genre, String isbn, String price, int stock, String description) {
        Book b = new Book();
        b.setTitle(title);
        b.setAuthor(author);
        b.setGenre(genre);
        b.setIsbn(isbn);
        b.setPrice(new BigDecimal(price));
        b.setStockQuantity(stock);
        b.setDescription(description);
        b.setImageUrl("https://covers.openlibrary.org/b/isbn/" + isbn + "-L.jpg?default=false");
        return b;
    }
}
