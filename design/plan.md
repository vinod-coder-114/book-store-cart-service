# Cart Service Implementation Plan

## Problem and proposed approach

`design/cart-service-design.md` defines a cart service for maintaining book carts, calculating totals, validating availability, enforcing ownership, and supporting checkout and order handoff. The repository is currently a Spring Boot starter: it has one application class, a context-load test, minimal application configuration, a Gradle build, and a MySQL Compose service. There are no cart domain classes, API controllers, persistence models, migrations, service integrations, or security configuration yet.

Implement the service in layers, using MySQL as the authoritative store and Spring Data JPA for transactional cart state. Keep business rules in application/domain services, expose validated REST DTOs, and isolate Catalog and Order Service calls behind typed clients. Build checkout as a stateful, idempotent handoff: a cart is validated and prepared first, and is only completed after order creation is confirmed.

## Confirmed scope and decisions

- Support authenticated user carts only; guest carts and merge-on-login are out of scope for the initial implementation.
- Validate JWTs in the cart service. Derive cart ownership from the authenticated principal, not request payloads.
- Do not add Redis in the initial implementation; MySQL remains the source of truth.
- Adding a book already in the cart increments its quantity. Updating a book with `PUT` replaces its quantity.
- Catalog remains authoritative for book identity, current price, and availability. Store a price snapshot with each cart line for cart calculations; revalidate price and stock before checkout.
- Do not model Users or Products as locally owned relational tables. Store the user and book UUIDs as identifiers to respect service boundaries.

## Implementation work

1. **Establish the platform baseline (completed)**
   - Review and align the Gradle dependencies with the service's persistence model. Prefer Spring MVC with JPA because JPA is blocking; remove WebFlux unless a concrete reactive requirement is established.
   - Add externalized MySQL connection, validation, JWT resource-server, and management configuration. Use an idempotent SQL bootstrap script with Hibernate schema validation for now; no migration framework is included.
   - Replace invalid/inconsistent Compose MySQL settings with a pinned MySQL image, a non-root application account, environment-driven credentials, a published port, and a database health check.

2. **Build the cart domain and persistence (completed)**
   - Define cart lifecycle states (at minimum active, checkout-pending, and completed), cart and line-item entities, UUID identifiers, timestamps, quantity, and price snapshot.
   - Enforce one active cart per user and one line per book per cart with database constraints. Keep monetary arithmetic in `BigDecimal` with explicit scale/currency policy.
   - Implement repositories and transactional operations. Use optimistic versioning or an equivalent locking strategy to prevent lost updates and duplicate lines under concurrent requests.
   - Define cart count semantics explicitly in the API: return total units, and expose distinct book-line count separately if needed. (API response semantics remain for the REST API phase.)

3. **Add Catalog integration and cart rules (Completed)**
   - Create a typed Catalog client/adapter for book existence, current price, and availability, with configurable base URL/timeouts and explicit handling of unavailable or invalid upstream responses.
   - Validate book identifiers and positive quantities; keep any maximum quantity configurable and documented rather than silently accepting unbounded values.
   - Calculate line totals and cart subtotal from stored price snapshots. Revalidate current price and stock before checkout and return structured validation details when the cart is no longer valid.

4. **Implement authenticated REST APIs (Completed)**
   - Implement `GET /api/carts`, `POST /api/carts/items`, `PUT /api/carts/items/{bookId}`, `DELETE /api/carts/items/{bookId}`, `DELETE /api/carts`, `GET /api/carts/count`, `POST /api/carts/validate`, and `POST /api/carts/checkout`.
   - Use request/response DTOs, Bean Validation, consistent status codes, and a stable error response format. Avoid exposing persistence entities directly.
   - Resolve an authenticated user's active cart on every operation and ensure all queries and mutations are owner-scoped.

5. **Secure service endpoints (Completed)**
   - Configure Spring Security JWT resource-server validation with issuer/JWK and audience settings provided through environment configuration.
   - Require authentication for cart operations and map the trusted JWT subject/user claim to the user UUID. Do not trust a user ID supplied by the client.
   - Document the expected identity claim and gateway token contract; return consistent `401` and `403` responses.

6. **Implement docker compose and local development**
   - Add a `docker-compose.yaml` with the service and MySQL, including a health check for the database. Use environment variables for configuration.
   - Document local development steps, including how to run the service, access the API, and view logs. Include instructions for running tests and any necessary setup for the database.
   - Follow the user-service's compose file pattern for local development, ensuring that the service can be run and tested independently.
   - Add a Dockerfile for the service, ensuring that it can be built and run in a containerized environment. Include instructions for building and running the Docker image in the documentation.
   
7. **Implement checkout and order handoff**
   - Validate the cart and transition it to a checkout-pending/locked state or capture a versioned immutable snapshot so edits cannot race with order creation.
   - Agree on the Order Service request, response, failure, and idempotency contract before wiring the client. Retry safely without creating duplicate orders.
   - Mark the cart completed/clear it only after confirmed order creation; restore it to an editable state on a definitive failure, and define recovery for ambiguous timeouts.
   - Record cart lifecycle events reliably (transactional outbox recommended). Select the event broker and event schema only after confirming the system's existing event infrastructure.

8. **Add error handling and operations**
   - Map invalid input/book, missing cart, conflicts, checkout validation failures, and Catalog/Order outages to documented HTTP problem responses, including the design's `400`, `401`, `403`, `404`, `409`, `422`, and `503` cases as appropriate.
   - Add structured logs, request correlation, health/readiness information, and audit timestamps. Never log JWTs or other credentials.
   - Add service documentation for API contracts, configuration, local database setup, lifecycle behavior, and integration contracts.

9. **Verify behavior**
   - Add unit tests for quantity rules, duplicate additions, calculations, ownership, and lifecycle transitions.
   - Add repository tests for schema constraints and concurrent updates; API/security tests for all endpoints and error mappings.
   - Add Catalog and Order client tests, including upstream failures, checkout idempotency, and ambiguous order outcomes.
   - Run the existing Gradle test suite and the smallest focused checks after each implementation phase.

## Decisions to resolve before integration implementation

- Catalog Service API contract and authentication mechanism.
- Order Service endpoint, request/response schema, idempotency key, and behavior for timeouts/partial failures.
- Whether a message broker already exists, which events are required, and the event delivery contract.
- JWT issuer, audience, and claim that maps to the cart's user UUID.
- Currency policy, maximum permitted item quantity, and whether historical cart prices should be refreshed outside checkout.
- Whether cart expiration is required. The source design describes it as optional; keep it out of initial scope unless a retention policy is provided.

## Current repository context

- Existing components: Java 21 Gradle Spring Boot application, one `CartServiceApplication` entry point, one context-load test, basic `application.yaml`, and a MySQL `compose.yaml`.
- The platform baseline now uses Spring MVC with JPA, validation, JWT resource-server support, Actuator, MySQL schema bootstrap, and environment-driven configuration. Cart entities, repositories, transactional operations, authenticated REST APIs, endpoint security, JWT claim-to-user mapping, and consistent `401`/`403` responses are implemented; Order integration remains future work.
- The worktree contains an existing untracked `.github/` directory; this plan does not modify it.
