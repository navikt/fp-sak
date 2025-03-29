package no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.v2;

import java.time.LocalDate;

public record VergeV2Dto(String aktoerId, String navn, String organisasjonsnummer, LocalDate gyldigFom, LocalDate gyldigTom) {

}
