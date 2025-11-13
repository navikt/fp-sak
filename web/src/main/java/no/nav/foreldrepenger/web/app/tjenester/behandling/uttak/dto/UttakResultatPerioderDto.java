package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record UttakResultatPerioderDto(@NotNull List<UttakResultatPeriodeDto> perioderSøker,
                                       @NotNull List<UttakResultatPeriodeDto> perioderAnnenpart,
                                       @NotNull FilterDto årsakFilter,
                                       @NotNull LocalDate endringsdato) {

    public record FilterDto(@NotNull LocalDate kreverSammenhengendeUttakTom, @NotNull boolean utenMinsterett, @NotNull boolean søkerErMor) {}
}
