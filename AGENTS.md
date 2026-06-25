# AGENTS.md

Spring Boot 4 / Java 21 REST backend for a PYMES (small business) management app.
Single-module Maven project. No monorepo.

## Commands

```bash
./mvnw spring-boot:run          # Run app (port 8091 by default)
./mvnw test                     # All tests (H2 in-memory, no DB needed)
./mvnw test -Dtest=ClassName    # Single test class
./mvnw test -Dtest=ClassName#methodName  # Single test method
./mvnw package -DskipTests      # Build fat JAR without running tests
./mvnw clean                    # Clean build output
```

No lint, checkstyle, formatter, or typecheck plugins are configured — compilation is the only enforced quality gate.

## Local DB

PostgreSQL runs via Podman/Docker. Host port is **5436** (non-standard).

```bash
podman-compose -f local-dev/podman-compose.yml up -d
# or
docker compose -f local-dev/podman-compose.yml up -d
```

Credentials: `postgres` / `123456P`, DB: `pymes_app`.

## Schema migrations

**No Flyway or Liquibase.** JPA `ddl-auto=update` handles initial table creation; everything else is manual.

- Pending scripts live in `src/main/resources/db/manual/` — apply them by hand against PostgreSQL when pulling entity changes.
- Current scripts: `product-variants.sql`, `drop-sale-time.sql`.

## Profiles

- Default profile: `prod` (PostgreSQL, port 8091).
- Tests use `test` profile automatically (`@ActiveProfiles("test")`) — H2 in-memory with `MODE=PostgreSQL`, `ddl-auto=create-drop`.

## Key environment variables

| Var | Default |
|---|---|
| `PORT` | `8091` |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:4200` |
| `DB_URL` | `jdbc:postgresql://localhost:5436/pymes_app` |
| `DB_USERNAME` / `DB_PASSWORD` | `postgres` / `123456P` |
| `APP_AUTH_JWT_SECRET` | `CAMBIAR_ESTA_CLAVE_...` (insecure default — must change in prod) |
| `APP_AUTH_JWT_EXPIRATION_MS` | `28800000` (8 h) |
| `APP_AUTH_JWT_REFRESH_EXPIRATION_MS` | `604800000` (7 days) |
| `app.seed.demo-data` | `false` |

## Architecture

```
src/main/java/es/pedro/pymes_backend/
├── domain/           # JPA entities + enums
├── application/      # Business logic / use-case services
└── infrastructure/
    ├── repository/   # Spring Data JPA repos + report projections
    ├── security/     # JWT filter, SecurityConfig
    └── web/
        ├── controller/   # REST controllers (all under /api)
        ├── dto/          # Request/Response DTOs
        ├── config/       # CORS (WebConfig)
        └── exception/    # GlobalExceptionHandler + custom exceptions
```

Entry point: `PymesBackendApplication.java`.

## Testing conventions

- All tests are `@SpringBootTest` (full context).
- Web-layer tests extend `AuthenticatedApiIntegrationTestSupport` — provides `buildAuthenticatedMockMvc()` (admin JWT pre-set) and `buildBaseMockMvc()`.
- `@Transactional` rolls back DB state between test methods.
- H2 quirk: `ensureClientIdentifierSequence()` creates a sequence via native SQL to match PostgreSQL DDL — needed because H2's schema generation differs.
- `OrnitorrincoCasiDefinitivoApiIntegrationTest` is a catch-all integration test; the name is not descriptive.

## Domain gotchas

- **Appointment → Sale auto-creation:** setting appointment status to `CONFIRMED` automatically creates a sale. Re-confirming does NOT duplicate it.
- **Sales from appointments cannot be deleted:** `DELETE /api/sales/{id}` returns `409 Conflict` for appointment-linked sales.
- **SUPPLIES expenses** must match an existing product name exactly. Use `/api/expenses/suggestions` for autocomplete; free-text is only valid for `SERVICES` and `OTHER`.
- **Soft-delete for products:** deactivating a parent product deactivates all its variants.
- **Demo data seeder:** `DevDataSeeder` exists and is tested; enable with `app.seed.demo-data=true`.

## No CI

No `.github/workflows/` directory — there is no automated pipeline.
