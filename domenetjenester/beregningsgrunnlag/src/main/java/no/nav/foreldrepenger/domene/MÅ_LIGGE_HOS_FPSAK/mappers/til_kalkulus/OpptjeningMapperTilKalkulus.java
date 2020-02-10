package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.mappers.til_kalkulus;

import java.util.stream.Collectors;

import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.Periode;
import no.nav.folketrygdloven.kalkulator.modell.opptjening.OpptjeningAktivitetType;
import no.nav.folketrygdloven.kalkulator.opptjening.OpptjeningAktiviteterDto;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.opptjening.OpptjeningAktiviteter;

public class OpptjeningMapperTilKalkulus {

    public static OpptjeningAktiviteterDto mapOpptjeningAktiviteter(OpptjeningAktiviteter opptjeningAktiviteter) {
       return new OpptjeningAktiviteterDto(
            opptjeningAktiviteter.getOpptjeningPerioder().stream()
                .map(opptjeningPeriode -> OpptjeningAktiviteterDto.nyPeriode(
                OpptjeningAktivitetType.fraKode(opptjeningPeriode.getOpptjeningAktivitetType().getKode()),
                Periode.of(opptjeningPeriode.getPeriode().getFom(), opptjeningPeriode.getPeriode().getTom()),
                opptjeningPeriode.getArbeidsgiverOrgNummer(),
                opptjeningPeriode.getArbeidsgiverAktørId(),
                    opptjeningPeriode.getArbeidsforholdId() == null ? null : IAYMapperTilKalkulus.mapArbeidsforholdRef(opptjeningPeriode.getArbeidsforholdId()))).collect(Collectors.toList()));
    }
}
