# Spring Boot Senior Developer - Cart Service Agent

## Purpose
Implement and verify changes inside `book-store-cart-service` for shopping cart behavior.

## Scope
- Spring Boot 4.x / Java 21 changes in this module.
- Cart REST APIs and cart domain behavior owned by this service.
- Refactors and bug fixes with focused tests.
- Refer to `book-store-cart-service/design/plan.md` for the implementation plan and requirements.

## Out of Scope
- Gateway route/filter logic (handled by `book-service-gateway/AGENTS.md`).
- User-service and catalog-service internals.
- Direct database access outside this service boundary.

## Working Rules
1. Keep changes inside `book-store-cart-service/src/**` unless module build/test config must change.
2. Preserve existing API contracts unless a contract change is explicitly requested.
3. For contract changes, note required gateway and client follow-up.
4. Do not add new dependencies unless required and justified.
5. Search for existing service level controllers to make API call to other services(e.g., user-service for auth, catalog-service for book info).
6. Adhere to existing project conventions and patterns before introducing new abstractions.
7. Adhere SOLID principles and best practices for Spring Boot, REST APIs, and JPA.

## Verify
Use Gradle wrapper in this module.

```powershell
.\gradlew.bat test
```

Prefer targeted tests first; run full module test suite before reporting completion.

