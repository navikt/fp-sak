package no.nav.foreldrepenger.web.app.tjenester.behandling.ytelsefordeling;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record YtelseFordelingDto(Boolean overstyrtOmsorg, @NotNull LocalDate førsteUttaksdato, @NotNull LocalDate startDatoForPermisjon,
                                 @NotNull DekningsgradInfoDto dekningsgrader) {

    public record DekningsgradInfoDto(Integer avklartDekningsgrad, @NotNull OppgittDekningsgradDto søker, OppgittDekningsgradDto annenPart) {
    }

    public record OppgittDekningsgradDto(@NotNull LocalDate søknadsdato, Integer dekningsgrad) {
    }

}
