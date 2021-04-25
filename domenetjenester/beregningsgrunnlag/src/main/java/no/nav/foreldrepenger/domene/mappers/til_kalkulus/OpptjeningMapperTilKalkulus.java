package no.nav.foreldrepenger.domene.mappers.til_kalkulus;

import java.util.stream.Collectors;

import no.nav.folketrygdloven.kalkulus.kodeverk.OpptjeningAktivitetType;
import no.nav.folketrygdloven.kalkulator.modell.opptjening.OpptjeningAktiviteterDto;
import no.nav.folketrygdloven.kalkulator.tid.Intervall;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningAktiviteter;

public class OpptjeningMapperTilKalkulus {

    public static OpptjeningAktiviteterDto mapOpptjeningAktiviteter(OpptjeningAktiviteter opptjeningAktiviteter) {
        return new OpptjeningAktiviteterDto(
            opptjeningAktiviteter.getOpptjeningPerioder().stream()
                .map(opptjeningPeriode -> OpptjeningAktiviteterDto.nyPeriode(
                    OpptjeningAktivitetType.fraKode(opptjeningPeriode.opptjeningAktivitetType().getKode()),
                    mapPeriode(opptjeningPeriode),
                    opptjeningPeriode.arbeidsgiverOrgNummer(),
                    opptjeningPeriode.arbeidsgiverAktørId(),
                    opptjeningPeriode.arbeidsforholdId() == null ? null : IAYMapperTilKalkulus.mapArbeidsforholdRef(opptjeningPeriode.arbeidsforholdId()))).collect(Collectors.toList()));
    }

    private static Intervall mapPeriode(OpptjeningAktiviteter.OpptjeningPeriode opptjeningPeriode) {
        if (opptjeningPeriode.periode().getTom() == null) {
            return Intervall.fraOgMed(opptjeningPeriode.periode().getFom());
        }
        return Intervall.fraOgMedTilOgMed(opptjeningPeriode.periode().getFom(), opptjeningPeriode.periode().getTom());
    }
}
