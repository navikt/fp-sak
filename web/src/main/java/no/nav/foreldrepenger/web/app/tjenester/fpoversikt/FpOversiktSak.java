package no.nav.foreldrepenger.web.app.tjenester.fpoversikt;

public record FpOversiktSak(String saksnummer,
                            Status status,
                            YtelseType ytelseType,
                            String aktørId) {
    enum Status {
        AVSLUTTET,
        ÅPEN
    }

    enum YtelseType {
        FORELDREPENGER,
        SVANGERSKAPSPENGER,
        ENGANGSSTØNAD
    }
}
