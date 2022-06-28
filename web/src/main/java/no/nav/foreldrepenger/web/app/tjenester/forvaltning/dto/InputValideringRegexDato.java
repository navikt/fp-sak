package no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto;

final class InputValideringRegexDato {
    private InputValideringRegexDato() {
    }

    static final String DATO_PATTERN = "^(\\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01]))?$";
}
