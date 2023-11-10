package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening;

import java.time.LocalDate;
import java.time.Period;

import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.MerknadRuleReasonRef;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.RegelUtfallMerknad;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.fp.OpptjeningsvilkårForeldrepenger;
import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.doc.RuleOutcomeDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.evaluation.Resultat;
import no.nav.fpsak.nare.specification.LeafSpecification;

/**
 * Sjekk om bruker har tilstrekkelig opptjening inklusiv antatt godkjente perioder for arbeidsforhold uten innrapportert
 * inntekt ennå.
 * <p>
 * (antar at dersom bruker ikke trenger antatt godkjente perioder er det sjekket av tidligere regel)
 */
@RuleDocumentation(value = "FP_VK_23.2.2", outcomes = {
        @RuleOutcomeDocumentation(code = SjekkTilstrekkeligOpptjeningInklAntatt.IKKE_TILSTREKKELIG_OPPTJENING_ID, result = Resultat.NEI, description = "Ikke tilstrekkelig opptjening")
})
public class SjekkTilstrekkeligOpptjeningInklAntatt extends LeafSpecification<OpptjeningsvilkårMellomregning> {

    public static final String ID = SjekkTilstrekkeligOpptjeningInklAntatt.class.getSimpleName();

    private static final int INNTEKT_RAPPORTERING_SENEST = 5;

    public static final String IKKE_TILSTREKKELIG_OPPTJENING_ID = "1035";
    public static final MerknadRuleReasonRef IKKE_TILSTREKKELIG_OPPTJENING =
        new MerknadRuleReasonRef(RegelUtfallMerknad.RVM_1035, "Ikke tilstrekkelig opptjening. Har opptjening: {0}");

    public SjekkTilstrekkeligOpptjeningInklAntatt() {
        super(ID);
    }

    @Override
    public Evaluation evaluate(OpptjeningsvilkårMellomregning data) {
        var antattTotalOpptjening = data.getAntattTotalOpptjening().getOpptjentPeriode();
        var bekreftetOpptjeningPeriode = data.getBekreftetOpptjening().getOpptjentPeriode();


        if (data.sjekkErInnenforMinstePeriodeGodkjent(bekreftetOpptjeningPeriode)) {
            // quick return, skal være håndtert av tidligere regel.
            data.setTotalOpptjening(data.getBekreftetOpptjening());
            return ja();
        }

        //TODO(OJR) burde kanskje lage et egen regelsett for SVP, da det er store forskjeller
        if (data.getRegelParametre().skalGodkjenneBasertPåAntatt()) {
            // SVP godkjenner basert på antatt opptjening hvis behandling er før frist for inntektsrapportering.
            var fristForInntektsrapportering = beregnFristForOpptjeningsopplysninger(data);
            var skalKreveRapportertInntekt = data.getGrunnlag().behandlingsDato().isAfter(fristForInntektsrapportering);
            var antattOpptjeningTidsserie = data.getAntattTotalOpptjening();
            var avkortetTidsserie = antattOpptjeningTidsserie.getTidslinje().intersection(data.getGrunnlag().getOpptjeningPeriode());
            // Avslå hvis antattopptjening ikke har nok dager (bytte aktivitet, mv) eller rapporteringsfrist passert
            if (skalKreveRapportertInntekt || !data.sjekkErInnenforMinstePeriodeGodkjent(antattTotalOpptjening) || avkortetTidsserie.isEmpty()) {
                var opptjentPeriode = antattOpptjeningTidsserie.getOpptjentPeriode();
                data.setTotalOpptjening(antattOpptjeningTidsserie);
                return nei(IKKE_TILSTREKKELIG_OPPTJENING, opptjentPeriode);
            }
            var totalOpptjening = new OpptjentTidslinje(Period.between(avkortetTidsserie.getMinLocalDate(), avkortetTidsserie.getMaxLocalDate().plusDays(1)), avkortetTidsserie);
            data.setTotalOpptjening(totalOpptjening);
            var opptjentPeriode = totalOpptjening.getOpptjentPeriode();
            if (data.sjekkErInnenforMinstePeriodeGodkjent(opptjentPeriode)) {
                return ja();
            }
            return nei(IKKE_TILSTREKKELIG_OPPTJENING, opptjentPeriode);
        }

        data.setTotalOpptjening(data.getBekreftetOpptjening());

        Evaluation evaluation = nei(IKKE_TILSTREKKELIG_OPPTJENING, bekreftetOpptjeningPeriode);
        loggAntattOpptjeningPeriode(data, evaluation);
        return evaluation;
    }

    private LocalDate beregnFristForOpptjeningsopplysninger(OpptjeningsvilkårMellomregning data) {
        var skjæringstidspunkt = data.getGrunnlag().sisteDatoForOpptjening();

        // first er 5 i måned etter skjæringstidspunktet
        return skjæringstidspunkt.plusMonths(1).withDayOfMonth(INNTEKT_RAPPORTERING_SENEST);
    }

    private void loggAntattOpptjeningPeriode(OpptjeningsvilkårMellomregning data, Evaluation ev) {
        var antattTotalOpptjening = data.getAntattTotalOpptjening();
        ev.setEvaluationProperty(OpptjeningsvilkårForeldrepenger.EVAL_RESULT_ANTATT_AKTIVITET_TIDSLINJE, antattTotalOpptjening.getTidslinje());
        ev.setEvaluationProperty(OpptjeningsvilkårForeldrepenger.EVAL_RESULT_ANTATT_GODKJENT, antattTotalOpptjening.getOpptjentPeriode());
    }
}
