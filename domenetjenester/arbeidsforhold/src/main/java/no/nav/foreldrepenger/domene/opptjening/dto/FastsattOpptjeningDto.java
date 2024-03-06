package no.nav.foreldrepenger.domene.opptjening.dto;

import java.time.LocalDate;
import java.util.List;

import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetKlassifisering;

public record FastsattOpptjeningDto(LocalDate opptjeningFom, LocalDate opptjeningTom, OpptjeningPeriodeDto opptjeningperiode,
                                    List<FastsattOpptjeningAktivitetDto> fastsattOpptjeningAktivitetList) {

    public static record OpptjeningPeriodeDto(int måneder, int dager) {
    }

    public static record FastsattOpptjeningAktivitetDto(LocalDate fom, LocalDate tom, OpptjeningAktivitetKlassifisering klasse) {
    }
}
