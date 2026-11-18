package com.devarsh.bookstore;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BookApiTest extends ApiTestSupport {

    @Test
    void anyoneCanBrowseBooksWithPagination() throws Exception {
        mvc.perform(get("/api/books?page=0&size=3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(lessThanOrEqualTo(3))))
                .andExpect(jsonPath("$.size").value(3))
                .andExpect(jsonPath("$.totalElements").isNumber());
    }

    @Test
    void anonymousUserCannotCreateBook() throws Exception {
        mvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON)
                        .content(bookJson("Nope", uniqueIsbn(), 1)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void customerCannotCreateBook() throws Exception {
        mvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(newCustomerToken()))
                        .content(bookJson("Nope", uniqueIsbn(), 1)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanCreateReadUpdateAndDeleteBook() throws Exception {
        String admin = bearer(adminToken());
        String isbn = uniqueIsbn();

        String created = mvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", admin)
                        .content(bookJson("Lifecycle Book", isbn, 10)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Lifecycle Book"))
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) com.jayway.jsonpath.JsonPath.read(created, "$.id")).longValue();

        mvc.perform(get("/api/books/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isbn").value(isbn));

        mvc.perform(put("/api/books/" + id).contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", admin)
                        .content("""
                                {"title":"Lifecycle Book","author":"Test Author","genre":"Testing","isbn":"%s",
                                 "price":250.00,"description":"updated","stockQuantity":5,"imageUrl":null}""".formatted(isbn)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(250.0))
                .andExpect(jsonPath("$.stockQuantity").value(5));

        mvc.perform(delete("/api/books/" + id).header("Authorization", admin))
                .andExpect(status().isOk());

        mvc.perform(get("/api/books/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Book not found with id " + id));
    }

    @Test
    void invalidBookReturns400WithFieldErrors() throws Exception {
        mvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(adminToken()))
                        .content("""
                                {"title":"","author":"A","genre":"G","isbn":"123","price":-5,"stockQuantity":-1}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.title").exists())
                .andExpect(jsonPath("$.fieldErrors.price").exists())
                .andExpect(jsonPath("$.fieldErrors.stockQuantity").exists());
    }

    @Test
    void duplicateIsbnReturns409() throws Exception {
        String admin = bearer(adminToken());
        String isbn = uniqueIsbn();
        mvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON).header("Authorization", admin)
                .content(bookJson("First", isbn, 1))).andExpect(status().isCreated());
        mvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON).header("Authorization", admin)
                .content(bookJson("Second", isbn, 1))).andExpect(status().isConflict());
    }

    @Test
    void searchFindsBookByTitle() throws Exception {
        String unique = "Zebra" + UUID.randomUUID().toString().substring(0, 8);
        mvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(adminToken()))
                        .content(bookJson(unique, uniqueIsbn(), 3)))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/books").param("search", unique.toLowerCase()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value(unique));
    }

    @Test
    void genreFilterAndInvalidSortAreHandled() throws Exception {
        mvc.perform(get("/api/books").param("genre", "Fiction"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].genre").value("Fiction"));
        mvc.perform(get("/api/books").param("sortBy", "password"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void customerCanReviewABook() throws Exception {
        long id = createBook(5);
        String customer = bearer(newCustomerToken());
        mvc.perform(post("/api/books/" + id + "/reviews").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", customer)
                        .content("""
                                {"rating":4,"comment":"Good read"}"""))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/books/" + id))
                .andExpect(jsonPath("$.averageRating").value(4.0))
                .andExpect(jsonPath("$.reviewCount").value(1));
        mvc.perform(post("/api/books/" + id + "/reviews").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", customer)
                        .content("""
                                {"rating":9}"""))
                .andExpect(status().isBadRequest());
    }
}
