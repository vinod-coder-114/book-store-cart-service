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
