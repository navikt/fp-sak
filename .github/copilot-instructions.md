# Copilot Instructions for fp-sak

## What is fp-sak?

fp-sak is the core case processing application (fagsystem) for Norwegian parental benefits. It handles:
- **Foreldrepenger** (parental benefits) — the primary benefit type
- **Engangsstønad** (lump-sum grant) — one-time payment for birth/adoption
- **Svangerskapspenger** (pregnancy benefits) — benefits during pregnancy

fp-sak is the largest and most central application in the foreldrepenger ecosystem, with dependencies on fp-abakus (historical data), fp-kalkulus (benefit calculation), and many other services.

## Integration Tests

Integration tests for fp-sak live in a **separate repository**: [fp-autotest](https://github.com/navikt/fp-autotest).

### Which test suites apply to fp-sak

| Suite | What it tests | Run command |
|-------|---------------|-------------|
| `fpsak` | All fp-sak functionality (19 test classes, ~120 tests) | `mvn test -P fpsak` |
| `verdikjede` | End-to-end value chain (4 test classes, ~50 tests) | `mvn test -P verdikjede` |

Both suites are triggered automatically in CI when fp-sak merges to master.

### Running integration tests for local fp-sak changes

1. Build fp-sak locally:
   ```bash
   mvn clean install -DskipTests
   docker build -t fp-sak .
   ```

2. In fp-autotest, generate a fresh `.env` and edit for local build:
   ```bash
   cd ../fp-autotest/lokal-utvikling
   ./setup-lokal-utvikling.sh
   ```
   Then edit `docker-compose-lokal/.env`:
   ```
   FPSAK_IMAGE=fp-sak:latest
   ```
   (replacing the default GAR image reference)

3. Start all services and run tests:
   ```bash
   cd ../fp-autotest/lokal-utvikling
   ./setup-lokal-utvikling.sh
   cd docker-compose-lokal
   docker compose up --detach
   # Wait for healthy: docker compose ps
   cd ../..
   mvn test -P fpsak                        # Full fpsak suite
   mvn test -P fpsak -Dtest=Fodsel          # Specific test class
   mvn test -P fpsak -Dtest="Fodsel#morSøkerFødselMedEttArbeidsforhold"  # Single test
   ```

5. Quick setup alternative:
   ```bash
   cd ../fp-autotest/lokal-utvikling
   ./lokal-utvikling-fpsak.sh               # Start all deps in Docker
   ./lokal-utvikling-ide.sh fpsak           # Start deps, run fpsak in IDE
   ```

### Finding relevant tests

The fp-autotest repository has an `AGENTS.md` file with a complete searchable test catalog including:
- All test DisplayNames organized by suite and class
- Aksjonspunkt codes tested per class
- Expected results (innvilget/avslag/avvist)

Ask Copilot: "Which fp-autotest tests cover [feature]?" and it will search the catalog.

## Local Development

- Java 25, Maven
- The application runs on port 8080 (mapped to 8080 in Docker Compose)
- Database: PostgreSQL (local), Oracle (legacy)
- All external services are mocked by VTP in the test environment
