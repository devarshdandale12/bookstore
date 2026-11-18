package com.devarsh.bookstore;

import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Shared helpers for the API tests. The tests start the whole application on an in-memory H2 database
 * and call the real endpoints with MockMvc (no MySQL needed).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class ApiTestSupport {

    static final String PASSWORD = "secret123";

    @Autowired
    protected MockMvc mvc;

    protected String uniqueEmail() {
        return "user" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
    }

    protected static String bearer(String token) {
        return "Bearer " + token;
    }

    /** Registers a new customer and returns a JWT for that customer. */
    protected String newCustomerToken() throws Exception {
        String email = uniqueEmail();
        mvc.perform(post("/api/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Test Customer","email":"%s","password":"%s"}""".formatted(email, PASSWORD)))
                .andExpect(status().isCreated());
        return login(email, PASSWORD);
    }

    /** The admin account is created by DataInitializer from application.properties. */
    protected String adminToken() throws Exception {
        return login("admin@bookstore.com", "Admin@123");
    }

    protected String login(String email, String password) throws Exception {
        String body = mvc.perform(post("/api/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}""".formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    protected String bookJson(String title, String isbn, int stock) {
        return """
                {"title":"%s","author":"Test Author","genre":"Testing","isbn":"%s","price":199.50,
                 "description":"A test book","stockQuantity":%d,"imageUrl":null}""".formatted(title, isbn, stock);
    }

    protected String uniqueIsbn() {
        return "T" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    /** Creates a book as admin and returns its id. */
    protected long createBook(int stock) throws Exception {
        String body = mvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(adminToken()))
                        .content(bookJson("Book " + UUID.randomUUID(), uniqueIsbn(), stock)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }
}
