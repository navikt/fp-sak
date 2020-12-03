package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.mappers.til_kalkulus;

import java.util.stream.Collectors;

import no.nav.folketrygdloven.kalkulator.modell.opptjening.OpptjeningAktivitetType;
import no.nav.folketrygdloven.kalkulator.modell.opptjening.OpptjeningAktiviteterDto;
import no.nav.folketrygdloven.kalkulator.tid.Intervall;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.opptjening.OpptjeningAktiviteter;

public class OpptjeningMapperTilKalkulus {

    public static OpptjeningAktiviteterDto mapOpptjeningAktiviteter(OpptjeningAktiviteter opptjeningAktiviteter) {
       return new OpptjeningAktiviteterDto(
            opptjeningAktiviteter.getOpptjeningPerioder().stream()
                .map(opptjeningPeriode -> OpptjeningAktiviteterDto.nyPeriode(
                OpptjeningAktivitetType.fraKode(opptjeningPeriode.getOpptjeningAktivitetType().getKode()),
                mapPeriode(opptjeningPeriode),
                opptjeningPeriode.getArbeidsgiverOrgNummer(),
                opptjeningPeriode.getArbeidsgiverAktørId(),
                    opptjeningPeriode.getArbeidsforholdId() == null ? null : IAYMapperTilKalkulus.mapArbeidsforholdRef(opptjeningPeriode.getArbeidsforholdId()))).collect(Collectors.toList()));
    }

    private static Intervall mapPeriode(OpptjeningAktiviteter.OpptjeningPeriode opptjeningPeriode) {
        if (opptjeningPeriode.getPeriode().getTom() == null) {
            return Intervall.fraOgMed(opptjeningPeriode.getPeriode().getFom());
        }
        return Intervall.fraOgMedTilOgMed(opptjeningPeriode.getPeriode().getFom(), opptjeningPeriode.getPeriode().getTom());
    }

}
