# fp-sak — Agent Instructions

## Integration Testing

This application is tested by the [fp-autotest](https://github.com/navikt/fp-autotest) repository.

**Test suites for fp-sak**: `fpsak`, `verdikjede`

### Quick Reference: Test Classes for fp-sak

#### Suite: fpsak (19 classes)
**Engangsstønad:** Adopsjon, Fodsel, Innsyn, Klage, Medlemskap, Revurdering, Soknadsfrist, Termin
**Foreldrepenger:** Aksjonspunkter, ArbeidsforholdVarianter, BeregningVerdikjede, Fodsel, Klage, MorOgFarSammen, RegresjonPreWLB, Revurdering, SammenhengendeUttak, Termin, ToTetteOgMinsterettTester, Ytelser
**Svangerskapspenger:** Førstegangsbehandling

#### Suite: verdikjede (4 classes)
VerdikjedeEngangsstonad, VerdikjedeForeldrepenger, VerdikjedeSvangerskapspenger, AdressebeskyttelseOgSkjermetPersonTester

### Run Commands
```bash
# Full fpsak suite
cd ../fp-autotest && mvn test -P fpsak

# Specific class
mvn test -P fpsak -Dtest=Fodsel

# Single method
mvn test -P fpsak -Dtest="Fodsel#morSøkerFødselMedEttArbeidsforhold"

# Verdikjede suite
mvn test -P verdikjede
```

### Testing Local Changes

1. Build fp-sak and create a local Docker image:
   ```bash
   mvn clean install -DskipTests
   docker build -t fp-sak .
   ```

2. In fp-autotest, generate a fresh `.env` and edit it for local build:
   ```bash
   cd ../fp-autotest/lokal-utvikling
   ./setup-lokal-utvikling.sh
   ```
   Then edit `docker-compose-lokal/.env` — change:
   ```
   FPSAK_IMAGE=fp-sak:latest
   ```
   (replacing the default GAR image reference)

3. Start the Docker Compose environment (all services are needed):
   ```bash
   cd docker-compose-lokal
   docker compose up --detach
   ```
   Wait for all to be healthy: `docker compose ps`

4. Run the integration tests (from fp-autotest root):
   ```bash
   cd ../..
   mvn test -P fpsak -Dtest=<TestClass>
   ```

5. After tests complete, shut down services (unless more tests are planned):
   ```bash
   cd lokal-utvikling/docker-compose-lokal
   docker compose down
   ```
   If code changes are needed before retesting: shut down → rebuild → start again.
   To restart only fp-sak after a rebuild: `docker compose up --detach --force-recreate fpsak`

### For Full Test Catalog

See `AGENTS.md` in the fp-autotest repository for:
- Complete list of test DisplayNames
- Aksjonspunkt codes covered per test class
- Expected results (innvilget/avslag/avvist)
