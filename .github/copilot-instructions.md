# fp-sak

Core case processing application (fagsystem) for Norwegian parental benefits:
**foreldrepenger**, **engangsstønad**, **svangerskapspenger**. Largest and most
central backend in the Team Foreldrepenger ecosystem.

## Context (read first)

- **fp-context** (https://github.com/navikt/fp-context) — team-wide domain,
  architecture, conventions, workflow. Treat as source of truth.
- **Copilot Space**: navikt / TeamForeldrepenger — attaches fp-context + key repos.
- Defer to fp-context for: domain/Folketrygdloven kap. 14, backend stack,
  Java code style, testing conventions, workflow/PR rules, CI/CD, dependency mgmt.

## Role in the value chain

| Upstream | fp-sak | Downstream |
|---|---|---|
| fp-mottak (søknad, inntektsmelding routing) | Behandling, vilkår, vedtak | fp-formidling (brev) |
| fp-abakus (arbeid/inntekt historikk) | | fpoppdrag → OS (utbetaling, JMS) |
| fp-kalkulus (beregning) | | fp-oversikt (innsyn) |
| fp-inntektsmelding (IM-API) | | fptilbake (via kravgrunnlag fra OS) |
| fp-tilgang (ABAC) | | |

Frontend: fp-frontend (saksbehandler UI).

## Repo-specific structure

| Area | Purpose |
|---|---|
| `behandlingslager` | JPA entities, repositories |
| `behandlingskontroll` | Behandlingsteg orchestration |
| `domenetjenester` | Domain services per fagområde |
| `web` | Jersey REST + Jetty bootstrap |
| `mottak` | Inbound events (søknad, IM, hendelser) |
| `migreringer` | Flyway scripts |

Behandling = a unit of case processing; steg = pipeline stage;
aksjonspunkt = saksbehandler decision point. See fp-context glossary.

## Integration testing

See `AGENTS.md` and [fp-autotest](https://github.com/navikt/fp-autotest).

## Local notes

- Port 8080
- DB: PostgreSQL (local), Oracle (prod legacy)
- All external systems mocked by VTP in test
