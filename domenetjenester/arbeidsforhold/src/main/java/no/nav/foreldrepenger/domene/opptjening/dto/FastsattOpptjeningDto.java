package no.nav.foreldrepenger.domene.opptjening.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetKlassifisering;

public record FastsattOpptjeningDto(@NotNull LocalDate opptjeningFom, @NotNull LocalDate opptjeningTom, @NotNull OpptjeningPeriodeDto opptjeningperiode,
                                    @NotNull List<FastsattOpptjeningAktivitetDto> fastsattOpptjeningAktivitetList) {

    public static record OpptjeningPeriodeDto(@NotNull int måneder, @NotNull int dager) {
    }

    public static record FastsattOpptjeningAktivitetDto(@NotNull LocalDate fom, @NotNull LocalDate tom, @NotNull OpptjeningAktivitetKlassifisering klasse) {
    }
}
