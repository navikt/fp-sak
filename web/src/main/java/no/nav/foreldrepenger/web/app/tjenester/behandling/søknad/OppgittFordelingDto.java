package no.nav.foreldrepenger.web.app.tjenester.behandling.søknad;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record OppgittFordelingDto(LocalDate startDatoForPermisjon, @NotNull DekningsgradInfoDto dekningsgrader) {

    public record DekningsgradInfoDto(Integer avklartDekningsgrad, @NotNull OppgittDekningsgradDto søker, @NotNull OppgittDekningsgradDto annenPart) {
    }

    public record OppgittDekningsgradDto(@NotNull LocalDate søknadsdato, Integer dekningsgrad) {
    }
}
