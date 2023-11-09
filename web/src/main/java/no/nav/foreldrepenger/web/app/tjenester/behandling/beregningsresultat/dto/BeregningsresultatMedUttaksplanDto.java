package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat.dto;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

public class BeregningsresultatMedUttaksplanDto {
    private LocalDate opphoersdato;
    private final BeregningsresultatPeriodeDto[] perioder;

    public BeregningsresultatMedUttaksplanDto(LocalDate opphoersdato, List<BeregningsresultatPeriodeDto> perioder) {
        this.opphoersdato = opphoersdato;
        this.perioder = perioder.toArray(BeregningsresultatPeriodeDto[]::new);
    }

    public LocalDate getOpphoersdato() {
        return opphoersdato;
    }

    public BeregningsresultatPeriodeDto[] getPerioder() {
        return Arrays.copyOf(perioder, perioder.length);
    }
}
