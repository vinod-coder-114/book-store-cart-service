# Cart Service

Spring Boot cart service using Java 21, Spring MVC, Spring Data JPA, and MySQL.
MySQL is the source of truth; Redis is not used.

## Local development

The Compose configuration starts a local MySQL 8.4 instance and creates a
non-root application user. Its default credentials and database are for local
development only. Override them with `MYSQL_DATABASE`, `MYSQL_ROOT_PASSWORD`,
`CART_DB_USERNAME`, `CART_DB_PASSWORD`, and `MYSQL_PORT` as needed. The
application connection can be configured independently with `CART_DB_URL`.

Start the database:

```powershell
docker compose up -d mysql
```

Run the application or tests:

```powershell
.\gradlew.bat bootRun
.\gradlew.bat test
```

When running locally, the application connects to `localhost:3306` by default.
The initial schema is created idempotently from
`src/main/resources/schema.sql`; Hibernate validates mappings against it and
does not modify the schema. Update the SQL bootstrap script alongside entity
changes until a migration strategy is selected.

Health endpoints are exposed at `/actuator/health` and
`/actuator/health/readiness`.

## Security contract

Cart endpoints under `/api/carts/**` require a valid bearer JWT. The service is
configured as an OAuth2 resource server and validates tokens with:

- `CART_JWT_ISSUER_URI` (default: `http://localhost:8081`)
- `CART_JWT_JWK_SET_URI` (default: `${USER_SERVICE_URL}/.well-known/jwks.json`)
- `CART_JWT_AUDIENCE` (default: `cart-service`)

The cart owner is resolved from JWT claims in this order:

1. `sub` (preferred)
2. `userId` (fallback)

The selected claim must contain a UUID. The API never trusts user IDs provided
in request payloads or query parameters.

Gateway/user-service token contract for cart calls:

- include `sub` (or `userId`) as the authenticated user UUID
- include issuer and audience values accepted by this service
- forward the bearer token unchanged to cart-service

Authentication failures return `401` and authorization failures return `403`
using a consistent JSON error body.

## Cart API reference

The examples below use the local service URL `http://localhost:9090`. When
calling through the API Gateway, use the gateway's origin and keep the same
`/api/carts` paths. All cart routes require
`Authorization: Bearer <access-token>` and `Content-Type: application/json`
when sending a body. The authenticated user is taken from the JWT; do not send
a user ID.

| Method | URL | Request body | Success response |
| --- | --- | --- | --- |
| `GET` | `/api/carts` | None | `200` — current cart (`CartResponse`) |
| `POST` | `/api/carts/items` | `CartItemRequest` | `201` — updated cart (`CartResponse`) |
| `PUT` | `/api/carts/items/{bookId}` | `CartQuantityUpdateRequest` | `200` — updated cart (`CartResponse`) |
| `DELETE` | `/api/carts/items/{bookId}` | None | `200` — updated cart (`CartResponse`) |
| `DELETE` | `/api/carts` | None | `200` — emptied cart (`CartResponse`) |
| `GET` | `/api/carts/count` | None | `200` — `CartCountResponse` |
| `POST` | `/api/carts/validate` | None | `200` — `CartValidationResponse` |
| `POST` | `/api/carts/checkout` | None | `200` — cart moved to `CHECKOUT_PENDING` (`CartResponse`) |

Add item request (`POST /api/carts/items`):

```json
{
  "bookId": "22222222-2222-2222-2222-222222222222",
  "quantity": 2
}
```

Set item quantity request
(`PUT /api/carts/items/22222222-2222-2222-2222-222222222222`):

```json
{
  "quantity": 3
}
```

Both quantities must be positive; the default maximum quantity per item is
configured by `CART_MAX_QUANTITY_PER_ITEM` (default `20`). `bookId` must
identify a book available from the catalog. Adding an existing book increases
its current quantity; `PUT` sets the quantity to the supplied value.

Example `CartResponse` (returned by cart read/mutation and checkout routes):

```json
{
  "cartId": "33333333-3333-3333-3333-333333333333",
  "status": "ACTIVE",
  "items": [
    {
      "bookId": "22222222-2222-2222-2222-222222222222",
      "quantity": 2,
      "unitPrice": 12.50,
      "lineTotal": 25.00
    }
  ],
  "totalUnits": 2,
  "distinctItemCount": 1,
  "subtotal": 25.00,
  "createdAt": "2026-10-09T07:30:00Z",
  "updatedAt": "2026-10-09T07:30:00Z"
}
```

Example `GET /api/carts/count` response:

```json
{
  "totalUnits": 2,
  "distinctItemCount": 1
}
```

If the user does not have an open cart, this endpoint returns `200` with both
counts set to `0`. `GET /api/carts` and the other operations that need an
existing cart return `404` when no open cart exists; adding an item creates
one.

Example `POST /api/carts/validate` response (validation problems are returned
in the response body, not as an HTTP error):

```json
{
  "cartId": "33333333-3333-3333-3333-333333333333",
  "valid": false,
  "totalUnits": 2,
  "distinctItemCount": 1,
  "subtotal": 25.00,
  "issues": [
    {
      "bookId": "22222222-2222-2222-2222-222222222222",
      "code": "PRICE_CHANGED",
      "message": "Catalog price has changed",
      "requestedQuantity": 2,
      "availableStock": 5,
      "cartUnitPrice": 12.50,
      "currentUnitPrice": 13.00
    }
  ]
}
```

`POST /api/carts/checkout` runs the same validation and only prepares the cart
for checkout when it is valid; it does not create an order. On validation
failure it returns `422 Unprocessable Entity` with the standard error shape:

```json
{
  "timestamp": "2026-10-09T07:30:00Z",
  "status": 422,
  "error": "Unprocessable Content",
  "message": "Cart validation failed",
  "path": "/api/carts/checkout",
  "details": {
    "22222222-2222-2222-2222-222222222222.STOCK_UNAVAILABLE": "Requested quantity is no longer available"
  }
}
```

Other errors use the same fields (`timestamp`, `status`, `error`, `message`,
`path`, `details`): `400` for invalid requests or unavailable books, `401` for
missing/invalid authentication, `403` for denied access, `404` for a missing
cart, and `503` when the catalog service is unavailable.
