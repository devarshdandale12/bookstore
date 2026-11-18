# Bookstore Management System - REST API (Java, Spring Boot, MySQL)

A complete backend with JWT security, plus a small HTML/CSS/JS storefront and admin panel served by the same app.

| Part | Technology |
|------|-----------|
| API | Java 21, Spring Boot 3.3, Spring Web, Spring Data JPA (Hibernate) |
| Database | MySQL 8 |
| Security | Spring Security + JWT (jjwt), BCrypt password hashing, role-based access (CUSTOMER / ADMIN) |
| Docs | Swagger UI (springdoc-openapi) |
| Tests | JUnit 5 + MockMvc (H2 in-memory DB, so tests need no MySQL) |
| Frontend | HTML, CSS, JavaScript (in `src/main/resources/static`) |

## 1. Run it

1. Install JDK 21 and MySQL 8, and make sure MySQL is running.
2. Open `src/main/resources/application.properties` and set your MySQL username and password
   (defaults: `root` / `root`, or use the `DB_USERNAME` and `DB_PASSWORD` environment variables).
   The database `bookstore_db` is created automatically.
3. Open the project folder in IntelliJ / Eclipse as a **Maven** project.
   Lombok is used: in IntelliJ enable *Settings > Build > Compiler > Annotation Processors > Enable annotation processing*
   (IntelliJ usually asks you). In Eclipse install Lombok once.
4. Run `BookstoreApplication`, or from a terminal: `mvn spring-boot:run`

Then open:

| URL | What |
|-----|------|
| http://localhost:8080 | Storefront (browse, cart, orders, admin panel) |
| http://localhost:8080/swagger-ui.html | Swagger API documentation (try the endpoints) |

On first start the app creates sample books and a default admin:

* **Admin:** `admin@bookstore.com` / `Admin@123` (change it in `application.properties`)
* **Customer:** register from the storefront or `POST /api/register`
* To register another admin, send `"role":"ADMIN"` and `"adminCode":"BOOKSTORE-ADMIN-2026"` (setting `app.admin.registration-code`).
  This stops random people from making themselves admins.

## 2. Run the tests

```
mvn test
```
Tests cover registration, login, JWT protection, role rules, book CRUD, validation errors, search, pagination,
ordering, stock reduction, cancellation and the order status rules.

## 3. API summary

`Authorization: Bearer <token>` is needed where the "Access" column says so.

| Method | Endpoint | Access | Purpose |
|--------|----------|--------|---------|
| POST | /api/register | public | Register a customer (admin needs the admin code) |
| POST | /api/login | public | Returns a JWT token |
| GET | /api/me | logged in | Current user |
| GET | /api/books | public | List books. Query: `page, size, search, genre, sortBy, direction` |
| GET | /api/books/genres | public | Genres for filters |
| GET | /api/books/{id} | public | One book (with average rating) |
| POST | /api/books | ADMIN | Add a book |
| PUT | /api/books/{id} | ADMIN | Update a book |
| DELETE | /api/books/{id} | ADMIN | Delete a book (409 if it is part of an order) |
| GET / POST | /api/books/{id}/reviews | public / logged in | Reviews (bonus feature) |
| GET | /api/orders | ADMIN | All orders, paginated, optional `status` filter |
| GET | /api/orders/my | logged in | My order history |
| GET | /api/orders/{id} | owner or ADMIN | One order |
| POST | /api/orders | CUSTOMER | Place an order |
| PUT | /api/orders/{id}/status | ADMIN | Update order status |

### Example requests

```
POST /api/login
{ "email": "admin@bookstore.com", "password": "Admin@123" }
-> 200 { "token": "eyJhbGciOi...", "tokenType": "Bearer", "expiresInMs": 86400000,
         "user": { "id": 1, "name": "Store Admin", "email": "admin@bookstore.com", "role": "ADMIN" } }

POST /api/orders          (customer token)
{ "paymentMethod": "CARD", "items": [ { "bookId": 1, "quantity": 2 } ] }
-> 201 { "id": 1, "status": "PENDING", "paymentStatus": "PAID", "totalAmount": 1198.00, ... }
```

### Example errors (always the same JSON shape)

```
GET /api/books/999            -> 404 { "status": 404, "error": "Not Found", "message": "Book not found with id 999", "path": "/api/books/999" }
POST /api/books (price: -5)   -> 400 { "message": "Validation failed...", "fieldErrors": { "price": "Price must be greater than 0" } }
POST /api/books (no token)    -> 401 { "message": "Authentication required..." }
POST /api/books (customer)    -> 403 { "message": "You do not have permission to perform this action." }
POST /api/register (same email) -> 409 { "message": "An account with this email already exists" }
```

## 4. How it works (good to know for your viewing / viva)

**Layers** (each class has one job):

```
Browser / Swagger / Postman
        |  HTTP + JSON
   controller   - receives the request, validates the JSON (@Valid), calls a service, returns JSON
        |
   service      - business rules (stock check, order total, status rules, admin code)
        |
   repository   - Spring Data JPA interfaces that talk to MySQL
        |
   entity       - Java classes that map to tables (User, Book, Order, OrderItem, Review)
```
`dto` = the request/response shapes (so passwords and internal fields never leak),
`exception` = custom errors + `GlobalExceptionHandler` (turns every error into clean JSON with the right status),
`security` + `config/SecurityConfig` = JWT login flow and access rules.

**Login flow:** `POST /api/login` -> check email + BCrypt password -> create a signed JWT (email + role + expiry) ->
client sends it as `Authorization: Bearer ...` -> `JwtAuthFilter` checks it on every request -> `SecurityConfig` rules decide 200 / 401 / 403.

**Order flow (`OrderService.place`)**, all in one database transaction: lock each book row -> check stock ->
reduce stock -> save the order with a price snapshot per item -> mock payment (CARD/UPI = PAID, COD = PENDING).
If anything fails, nothing is saved. Cancelling an order puts the stock back and marks paid orders as REFUNDED.
Order statuses follow `PENDING -> SHIPPED -> DELIVERED` (or `CANCELLED` before delivery).

## 5. Decisions I made beyond the PDF

* **Admin registration is protected by a secret code** - letting anyone self-register as admin would defeat role-based security.
* **Order statuses have rules** and cancelling restocks books; COD payment turns PAID when delivered.
* **Payment is mocked** (allowed by the task). `OrderService` is the one place to plug in Stripe / PayPal later.
* **Reviews and ratings** (optional enhancement) and an **admin panel** in the frontend (add / edit / delete books, update order status).
* Books that already appear in an order cannot be deleted (keeps order history correct) - the API answers 409.
* Prices are shown in Indian rupees in the storefront; change `money()` in `static/js/app.js` if you want another currency.

## 6. Project structure

```
bookstore-api
|-- pom.xml
|-- database/schema.sql                  (reference only)
`-- src
    |-- main
    |   |-- java/com/devarsh/bookstore
    |   |   |-- BookstoreApplication.java
    |   |   |-- config/        SecurityConfig, OpenApiConfig, CommonErrors, DataInitializer
    |   |   |-- controller/    AuthController, BookController, OrderController, ReviewController
    |   |   |-- dto/           request / response records
    |   |   |-- entity/        User, Book, Order, OrderItem, Review + enums
    |   |   |-- exception/     custom exceptions + GlobalExceptionHandler
    |   |   |-- repository/    Spring Data JPA repositories
    |   |   |-- security/      JwtService, JwtAuthFilter, CustomUserDetailsService
    |   |   |-- service/       AuthService, BookService, OrderService, ReviewService
    |   |   `-- util/          Paging
    |   `-- resources
    |       |-- application.properties
    |       `-- static/        index.html, css/style.css, js/app.js   (the frontend)
    `-- test                   JUnit tests (+ application-test.properties for H2)
```

## 7. Troubleshooting

* **`Access denied for user 'root'`** - fix the MySQL password in `application.properties`.
* **`cannot find symbol ... getTitle()` / similar** - Lombok annotation processing is not enabled in your IDE (see step 3).
* **Port 8080 already used** - set `server.port=8081` in `application.properties`.
* **Cover images missing** - they load from Open Library and need internet; the app shows a generated cover otherwise.
