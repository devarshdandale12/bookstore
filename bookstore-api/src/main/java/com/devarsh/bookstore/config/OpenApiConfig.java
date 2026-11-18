package com.devarsh.bookstore.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI: http://localhost:8080/swagger-ui.html
 * Click "Authorize", paste the token from /api/login, and you can try the protected endpoints.
 */
@Configuration
@OpenAPIDefinition(info = @Info(
        title = "Bookstore Management System API",
        version = "1.0",
        description = "REST API for books, users (customers and admins) and orders. "
                + "Log in with POST /api/login, then click Authorize and paste the token."))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
}
