package no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling;

import java.time.LocalDate;
import java.util.UUID;

//dokumentId er intern behandlingId
public record VedtaksdokumentasjonDto(String dokumentId, UUID behandlingUuid, String tittel, LocalDate opprettetDato) {

}
