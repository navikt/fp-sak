package no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling;

import java.time.LocalDate;
import java.util.UUID;

public record InnsynVedtaksdokumentasjonDto(UUID behandlingUuid, String tittel, LocalDate opprettetDato) {

}
