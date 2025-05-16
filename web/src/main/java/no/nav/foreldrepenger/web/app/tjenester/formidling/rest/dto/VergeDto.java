package no.nav.foreldrepenger.web.app.tjenester.formidling.rest.dto;

import java.time.LocalDate;

public record VergeDto(String aktoerId, String navn, String organisasjonsnummer, LocalDate gyldigFom, LocalDate gyldigTom) {

    @Override
    public String toString() {
        return "VergeDto{" + "navn='" + navn + '\'' + ", organisasjonsnummer='" + organisasjonsnummer + '\'' + ", gyldigFom=" + gyldigFom
            + ", gyldigTom=" + gyldigTom + '}';
    }
}
