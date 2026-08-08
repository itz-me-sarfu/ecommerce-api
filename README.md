# E-Commerce API

A recruiter-ready online shopping backend built with Java 17, Spring Boot, Spring Data JPA/Hibernate, MySQL, Spring Security, JWT, Maven, Docker, JUnit 5, and Mockito.

## Highlights

- JWT authentication with `CUSTOMER`, `SELLER`, and `ADMIN` roles.
- DTO-only REST responses with a consistent global error format.
- Product search/filtering with JPA Specifications and pageable responses.
- Optimistic-locking inventory reservation inside a transactional order workflow.
- Cart, wishlist, address book, order history, cancellation, and order state transitions.
- Admin dashboard metrics and low-stock visibility.
- OpenAPI/Swagger UI and a ready-to-import Postman collection.
- H2-backed Spring Boot context test plus focused Mockito service tests.

## Run with Docker

```bash
docker compose up --build
```

The API will be available at `http://localhost:8080`. Swagger UI is at `http://localhost:8080/swagger-ui.html`.

The development profile creates these demo categories and an admin account on first startup:

```text
email:    admin@example.com
password: Admin@12345
```

Change them with `SEED_ADMIN_EMAIL` and `SEED_ADMIN_PASSWORD` environment variables before starting the container.

## Run locally

Start MySQL, then set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and `JWT_SECRET` if the defaults do not match your machine.

```bash
mvn spring-boot:run
```

Run tests:

```bash
mvn test
```

## API examples

Register and login:

```http
POST /api/auth/register
Content-Type: application/json

{
  "fullName": "Asha Sharma",
  "email": "asha@example.com",
  "password": "Password@123"
}
```

Use the returned token as `Authorization: Bearer <token>`.

Catalog search supports pagination, sorting, free-text search, category, brand, and price range:

```http
GET /api/products?q=phone&category=electronics&brand=Acme&minPrice=500&maxPrice=5000&page=0&size=20&sort=price,asc
```

Product mutations require `ADMIN` or `SELLER`:

```http
POST /api/products
Authorization: Bearer <seller-or-admin-token>
Content-Type: application/json

{
  "name": "Acme Phone X",
  "sku": "ACME-PHONE-X",
  "description": "128 GB smartphone",
  "brand": "Acme",
  "price": 24999.00,
  "stock": 25,
  "imageUrl": "https://example.com/phone.jpg",
  "categoryId": 1
}
```

Typical customer flow:

1. `POST /api/addresses`
2. `POST /api/cart/items`
3. `GET /api/cart`
4. `POST /api/orders` with `{ "addressId": 1 }`
5. `GET /api/orders`

Order state transitions are validated:

```text
PLACED -> CONFIRMED -> PACKED -> SHIPPED -> DELIVERED
PLACED -> CANCELLED
```

Invalid transitions return HTTP 400. Concurrent stock changes return HTTP 409 and the transaction is rolled back.

## Role matrix

| Capability | CUSTOMER | SELLER | ADMIN |
|---|---:|---:|---:|
| Browse/search products and categories | yes | yes | yes |
| Cart, wishlist, addresses, own orders | yes | yes | yes |
| Create/update products and categories | no | yes | yes |
| Change order status | no | yes | yes |
| Admin dashboard | no | no | yes |

## Error format

Errors are returned through `@RestControllerAdvice`:

```json
{
  "timestamp": "2026-08-08T10:30:00Z",
  "status": 404,
  "error": "RESOURCE_NOT_FOUND",
  "message": "Product with ID 101 was not found.",
  "path": "/api/products/101",
  "validationErrors": {}
}
```

## Project layout

```text
src/main/java/com/ecommerce
├── auth          registration and login
├── security      JWT filter and Spring Security configuration
├── product       catalog, Specifications search, stock management
├── category      category CRUD
├── cart          cart and cart items
├── wishlist      wishlist
├── address       address book
├── order         checkout, inventory reservation, state machine
├── admin         dashboard metrics
└── common        DTOs and global exception handling
```

Import `docs/postman/ecommerce-api.postman_collection.json` into Postman to exercise the main flow.
