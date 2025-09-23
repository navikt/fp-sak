package no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record InnsynVedtaksdokumentasjonDto(@NotNull UUID behandlingUuid, @NotNull String tittel, @NotNull LocalDate opprettetDato) {

}
