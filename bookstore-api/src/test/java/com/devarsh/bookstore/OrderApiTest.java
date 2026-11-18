package com.devarsh.bookstore;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderApiTest extends ApiTestSupport {

    private String orderJson(long bookId, int qty, String method) {
        return """
                {"paymentMethod":"%s","items":[{"bookId":%d,"quantity":%d}]}""".formatted(method, bookId, qty);
    }

    private long placeOrder(String customerToken, long bookId, int qty, String method) throws Exception {
        String body = mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(customerToken))
                        .content(orderJson(bookId, qty, method)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    @Test
    void placingAnOrderReducesStockAndCalculatesTotal() throws Exception {
        long bookId = createBook(10);
        String customer = newCustomerToken();

        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(customer))
                        .content(orderJson(bookId, 3, "CARD")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.paymentStatus").value("PAID"))
                .andExpect(jsonPath("$.totalAmount").value(598.5))
                .andExpect(jsonPath("$.items[0].quantity").value(3));

        mvc.perform(get("/api/books/" + bookId)).andExpect(jsonPath("$.stockQuantity").value(7));
    }

    @Test
    void orderingMoreThanStockReturns400AndKeepsStock() throws Exception {
        long bookId = createBook(2);
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(newCustomerToken()))
                        .content(orderJson(bookId, 5, "UPI")))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/books/" + bookId)).andExpect(jsonPath("$.stockQuantity").value(2));
    }

    @Test
    void emptyOrderAndUnknownBookAreRejected() throws Exception {
        String customer = bearer(newCustomerToken());
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).header("Authorization", customer)
                        .content("""
                                {"paymentMethod":"CARD","items":[]}"""))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).header("Authorization", customer)
                        .content(orderJson(999999, 1, "CARD")))
                .andExpect(status().isNotFound());
    }

    @Test
    void roleRulesAreEnforced() throws Exception {
        long bookId = createBook(5);
        // admins cannot place orders
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(adminToken()))
                        .content(orderJson(bookId, 1, "CARD")))
                .andExpect(status().isForbidden());
        // customers cannot list all orders
        mvc.perform(get("/api/orders").header("Authorization", bearer(newCustomerToken())))
                .andExpect(status().isForbidden());
        // admins can
        mvc.perform(get("/api/orders").header("Authorization", bearer(adminToken())))
                .andExpect(status().isOk());
        // anonymous users cannot order
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(orderJson(bookId, 1, "CARD")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void customerSeesOnlyOwnOrders() throws Exception {
        long bookId = createBook(5);
        String owner = newCustomerToken();
        String stranger = newCustomerToken();
        long orderId = placeOrder(owner, bookId, 1, "COD");

        mvc.perform(get("/api/orders/" + orderId).header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("PENDING"));
        mvc.perform(get("/api/orders/" + orderId).header("Authorization", bearer(stranger)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/orders/" + orderId).header("Authorization", bearer(adminToken())))
                .andExpect(status().isOk());
        mvc.perform(get("/api/orders/my").header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/orders/987654").header("Authorization", bearer(adminToken())))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminMovesOrderThroughStatuses() throws Exception {
        long bookId = createBook(5);
        long orderId = placeOrder(newCustomerToken(), bookId, 1, "COD");
        String admin = bearer(adminToken());

        mvc.perform(put("/api/orders/" + orderId + "/status").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", admin).content("""
                                {"status":"SHIPPED"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPED"));

        // COD is marked paid when delivered
        mvc.perform(put("/api/orders/" + orderId + "/status").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", admin).content("""
                                {"status":"DELIVERED"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("PAID"));

        // a delivered order cannot change any more
        mvc.perform(put("/api/orders/" + orderId + "/status").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", admin).content("""
                                {"status":"PENDING"}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cancellingAnOrderRestocksBooksAndRefunds() throws Exception {
        long bookId = createBook(10);
        long orderId = placeOrder(newCustomerToken(), bookId, 4, "CARD");
        mvc.perform(get("/api/books/" + bookId)).andExpect(jsonPath("$.stockQuantity").value(6));

        mvc.perform(put("/api/orders/" + orderId + "/status").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(adminToken())).content("""
                                {"status":"CANCELLED"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("REFUNDED"));

        mvc.perform(get("/api/books/" + bookId)).andExpect(jsonPath("$.stockQuantity").value(10));
    }

    @Test
    void invalidStatusValueReturns400() throws Exception {
        long bookId = createBook(5);
        long orderId = placeOrder(newCustomerToken(), bookId, 1, "CARD");
        mvc.perform(put("/api/orders/" + orderId + "/status").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(adminToken())).content("""
                                {"status":"FLYING"}"""))
                .andExpect(status().isBadRequest());
    }
}
