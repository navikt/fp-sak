package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat.dto;

import java.time.LocalDate;
import java.util.List;

public record BeregningsresultatMedUttaksplanDto(LocalDate opphoersdato, List<BeregningsresultatPeriodeDto> perioder) {}
