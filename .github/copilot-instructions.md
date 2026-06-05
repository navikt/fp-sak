# fp-sak

Core case processing application for foreldrepenger, engangsstonad, and svangerskapspenger.

## Shared context

- Source of truth for shared domain, architecture, and conventions: `navikt/fp-context`
- Copilot Space: `navikt/TeamForeldrepenger`

## Repo-specific context

| Topic             | Details                                                                                 |
|-------------------|-----------------------------------------------------------------------------------------|
| Role              | Central application for case processing from søknad to vedtak/dismissal                 |
| Consumers         | `fp-frontend`, `fp-los`, `fp-oversikt`, `fp-mottak`, `fp-soknad`, `fptilbake`, external |
| Tech stack        | Standard fp Java backend using `fp-prosesstask`                                         |
| Data              | Oracle; FSS deployment; long-term storage of sak, behandling, vedtak                    |

Direct relations/integrations:
- Upstream: `fp-soknad`, `fp-mottak`, `fp-inntektsmelding`, Joark, K9-sak (vedtak pleiepenger), Kabal (vedtak klage)
- Frontend: `fp-frontend`, `fp-swagger`
- Satellites: `fp-abakus`, `fp-kalkulus`, `fpoppdrag`, `fp-formidling`
- Downstream: `fp-oversikt`, OS, Joark
- Parallel: `fp-los`, Kabal (klage unit)
- Main data sources: PDL, Joark
- Data warehouse: Oracle export schema fpsak_hist; Topic

## Entry points

- JakartaRS apps: `ApiConfig`, `EksternApiConfig`, `ForvaltningApiConfig`
- Endpoints from `RestImplementationClasses` methods: `getImplementationClasses` (fp-frontend), `getServiceClasses` (fp-apps), `getForvaltningClasses` (`fp-swagger`)
- Nav vedtak sharing endpoint: `EksternDelingYtelseInfoRestTjeneste`
- Nav Xacml PIP endpoint: `EksternPipRestTjeneste`

## Repo structure

| Area | Purpose                                                   |
|---|-----------------------------------------------------------|
| `behandlingslager` | JPA entities and repositories                             |
| `behandlingskontroll` | Behandlingsteg orchestration                              |
| `domenetjenester` | Domain services per fagomrade                             |
| `infrastrukturtjenester` | Structure for scheduled jobs                              |
| `web` | Jersey REST and Jetty bootstrap                           |
| `mottak` | Inbound events for soknad, inntektsmelding, and hendelser |
| `migreringer` | Flyway scripts                                            |

## Key concepts in code and database

- `Fagsak` = Case. Connecting citizen, benefit, documents, behandling, vedtak
- `Behandling` = a unit of case processing from start (søknad, klage, event) to vedtak/dismissal
- `BehandlingÅrsak` = reason and chaining to previous behandling
- `Behandlingsresultat` = detailed outcome and reasons
- `BehandlingVedtak` = vedtak overall outcome
- `BehandlingModell` = pre-defined pipeline (behandlingsprosess) for a benefit and behandling type. See `ForeldrepengerModellProducer`and similar for ES/SVP
- `BehandlingSteg` = pipeline stage with implementation 
- `Aksjonspunkt` = saksbehandler decision point
- `Vilkår`: conditions for the benefit (medlemskap, relasjon til barn, opptjening)
- Beregning: process of calculating the benefit using `fp-kalkulus`
- Uttak: process of selecting quota configuration resulting in the `UttakResultatEntitet` aggregate 
- Beregningsresultat: distrubution of benefit amounts over time and recipients (citizen, employer reimbursement)

## Verification

- Verify integration impact via `navikt/fp-autotest`
- Relevant suites: `fpsak`, `verdikjede`
- Preferred path: use the `run-integration-tests` skill
