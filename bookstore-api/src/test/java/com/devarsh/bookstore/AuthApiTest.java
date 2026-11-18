package com.devarsh.bookstore;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthApiTest extends ApiTestSupport {

    @Test
    void registerThenLoginReturnsToken() throws Exception {
        String email = uniqueEmail();
        mvc.perform(post("/api/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Asha","email":"%s","password":"secret123"}""".formatted(email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.password").doesNotExist());

        mvc.perform(post("/api/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"secret123"}""".formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(notNullValue()))
                .andExpect(jsonPath("$.user.email").value(email));
    }

    @Test
    void duplicateEmailIsRejectedWith409() throws Exception {
        String email = uniqueEmail();
        String body = """
                {"name":"Asha","email":"%s","password":"secret123"}""".formatted(email);
        mvc.perform(post("/api/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void invalidRegistrationReturns400WithFieldErrors() throws Exception {
        mvc.perform(post("/api/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","email":"not-an-email","password":"123"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void wrongPasswordReturns401() throws Exception {
        mvc.perform(post("/api/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@bookstore.com","password":"wrong-password"}"""))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminRegistrationNeedsTheSecretCode() throws Exception {
        mvc.perform(post("/api/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Sneaky","email":"%s","password":"secret123","role":"ADMIN","adminCode":"guess"}"""
                                .formatted(uniqueEmail())))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Real Admin","email":"%s","password":"secret123","role":"ADMIN","adminCode":"BOOKSTORE-ADMIN-2026"}"""
                                .formatted(uniqueEmail())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void protectedEndpointNeedsToken() throws Exception {
        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/me").header("Authorization", "Bearer not.a.real.token"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/me").header("Authorization", bearer(newCustomerToken())))
                .andExpect(status().isOk());
    }
}
