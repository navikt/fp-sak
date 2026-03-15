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
cd ~/git/fp-autotest && mvn test -P fpsak

# Specific class
mvn test -P fpsak -Dtest=Fodsel

# Single method
mvn test -P fpsak -Dtest="Fodsel#morSøkerFødselMedEttArbeidsforhold"

# Verdikjede suite
mvn test -P verdikjede
```

### Testing Local Changes

For building, deploying, and running tests against local fp-sak changes, use the `run-integration-tests` skill in fp-autotest (see `fp-autotest/.github/skills/run-integration-tests/`).

Quick reference for fp-sak:
- Docker build tag: `fp-sak`
- .env variable: `FPSAK_IMAGE`
- Docker Compose service: `fpsak`

### For Full Test Catalog

See `AGENTS.md` in the fp-autotest repository for:
- Complete list of test methods with DisplayNames
- Aksjonspunkt code → test method mapping
