package no.nav.foreldrepenger.domene.mappers.til_kalkulator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.folketrygdloven.kalkulator.modell.opptjening.OpptjeningAktiviteterDto;
import no.nav.folketrygdloven.kalkulator.tid.Intervall;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.mappers.RelevantOpptjeningMapper;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningAktiviteter;

public class OpptjeningMapperTilKalkulus {
    private static final Logger LOG = LoggerFactory.getLogger(OpptjeningMapperTilKalkulus.class);

    private OpptjeningMapperTilKalkulus() {
        // Hindrer default konstruktør
    }

    public static OpptjeningAktiviteterDto mapOpptjeningAktiviteter(OpptjeningAktiviteter opptjeningAktiviteter,
                                                                    InntektArbeidYtelseGrunnlag iayGrunnlag,
                                                                    BehandlingReferanse ref, Skjæringstidspunkt stp) {
        var relevanteAktiviteter = RelevantOpptjeningMapper.map(opptjeningAktiviteter, iayGrunnlag, ref, stp);
        var opptjeningInput = new OpptjeningAktiviteterDto(relevanteAktiviteter
            .stream()
            .map(opptjeningPeriode -> OpptjeningAktiviteterDto.nyPeriode(
                KodeverkTilKalkulusMapper.mapOpptjeningAktivitetType(opptjeningPeriode.opptjeningAktivitetType()), mapPeriode(opptjeningPeriode),
                opptjeningPeriode.arbeidsgiverOrgNummer(), opptjeningPeriode.arbeidsgiverAktørId(),
                opptjeningPeriode.arbeidsforholdId() == null ? null : IAYMapperTilKalkulus.mapArbeidsforholdRef(
                    opptjeningPeriode.arbeidsforholdId())))
            .toList());
        if (opptjeningInput.getOpptjeningPerioder().isEmpty() && !opptjeningAktiviteter.getOpptjeningPerioder().isEmpty()) {
            LOG.warn("FP-658423: Fjernet alle opptjeningsaktiviteter før innsending til beregning. Oppteningaktiviteter: " + opptjeningAktiviteter);
        }

        return opptjeningInput;
    }

    private static Intervall mapPeriode(OpptjeningAktiviteter.OpptjeningPeriode opptjeningPeriode) {
        if (opptjeningPeriode.periode().getTom() == null) {
            return Intervall.fraOgMed(opptjeningPeriode.periode().getFom());
        }
        return Intervall.fraOgMedTilOgMed(opptjeningPeriode.periode().getFom(), opptjeningPeriode.periode().getTom());
    }
}
