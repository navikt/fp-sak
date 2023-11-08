package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.fp;

import java.time.Period;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableSet;

import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.Aktivitet;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.Opptjeningsgrunnlag;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.OpptjeningsvilkårMellomregning;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.OpptjeningsvilkårParametre;
import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

/**
 * Regel som sjekker om mellomliggende perioder i en aktivitet av type Arbeid for samme arbeidsgiver kan aksepteres som
 * gyldig aktivitet.
 * <p>
 * Må typisk være kortvarig (inntil 2 uker), bruker må ha jobbet minst 4 sammenhengende uker før og begynne for samme
 * arbeidsgiver etterpå.
 */
@RuleDocumentation("FP_VK_23.1.2")
public class SjekkMellomliggendePerioderForArbeid extends LeafSpecification<OpptjeningsvilkårMellomregning> {

    public static final String ID = SjekkMellomliggendePerioderForArbeid.class.getSimpleName();
    private static final String ARBEID = OpptjeningsvilkårForeldrepenger.ARBEID;

    public SjekkMellomliggendePerioderForArbeid() {
        super(ID);
    }

    @Override
    public Evaluation evaluate(OpptjeningsvilkårMellomregning data) {

        var mellomliggende = new SjekkMellomliggende(data.getGrunnlag(), data.getRegelParametre());
        data.getAktivitetTidslinjer(true, true)
                .entrySet().stream()
                .filter(e -> ARBEID.equals(e.getKey().getAktivitetType()))
                .forEach(mellomliggende::sjekkMellomliggende);

        Evaluation evaluation = ja();
        data.setAkseptertMellomliggendePerioder(mellomliggende.getAkseptertMellomliggendePerioder());
        evaluation.setEvaluationProperty(OpptjeningsvilkårForeldrepenger.EVAL_RESULT_AKSEPTERT_MELLOMLIGGENDE_PERIODE,
                mellomliggende.getAkseptertMellomliggendePerioder());

        return evaluation;
    }

    /** Implementerer algoritme for sammenligne mellomliggende perioder. */
    static class SjekkMellomliggende {

        private final Map<Aktivitet, LocalDateTimeline<Boolean>> akseptertMellomliggendePerioder = new HashMap<>();
        private Opptjeningsgrunnlag grunnlag;
        private OpptjeningsvilkårParametre parametre;

        SjekkMellomliggende(Opptjeningsgrunnlag grunnlag, OpptjeningsvilkårParametre parametre) {
            this.grunnlag = grunnlag;
            this.parametre = parametre;
        }

        Map<Aktivitet, LocalDateTimeline<Boolean>> getAkseptertMellomliggendePerioder() {
            return akseptertMellomliggendePerioder;
        }

        void sjekkMellomliggende(Entry<Aktivitet, LocalDateTimeline<Boolean>> e) {
            var key = e.getKey();
            // compress for å sikre at vi slipper å sjekke sammenhengende segmenter med samme verdi
            var timeline = e.getValue().compress();

            var mellomliggendePeriode = timeline.collect(this::toPeriod, true).mapValue(v -> Boolean.TRUE);
            if (!mellomliggendePeriode.isEmpty()) {
                akseptertMellomliggendePerioder.put(key, mellomliggendePeriode);
            }
        }

        private boolean toPeriod(@SuppressWarnings("unused") NavigableSet<LocalDateSegment<Boolean>> segmenterFør,
                LocalDateSegment<Boolean> segmentUnderVurdering,
                NavigableSet<LocalDateSegment<Boolean>> foregåendeSegmenter,
                NavigableSet<LocalDateSegment<Boolean>> påfølgendeSegmenter) {

            if (!erMellomliggendeSegment(segmentUnderVurdering, foregåendeSegmenter, påfølgendeSegmenter)
                    || foregåendeSegmenter.isEmpty()) {
                // mellomliggende segmenter har ingen verdi, så skipper de som har
                return false;
            }
            var foregående = foregåendeSegmenter.last();

            var foregåendeVarighet = Period.ofDays((int) foregående.getLocalDateInterval().totalDays()).normalized();
            var mellomliggendeVarighet = Period.ofDays((int) segmentUnderVurdering.getLocalDateInterval().totalDays()).normalized();

            // Mellomliggende perioder må være <=14 dager og tilknyttende foregående periode med registrert arbeid
            // minst 4 uker for å tas i betraktning.
            var mellomliggendeSammenlignet = mellomliggendeVarighet
                    .minus(parametre.maksMellomliggendePeriodeForArbeidsforhold());
            var foregåendeSammenlignet = foregåendeVarighet
                    .minus(parametre.minForegåendeForMellomliggendePeriodeForArbeidsforhold());

            return Boolean.TRUE.equals(foregående.getValue())
                    && !foregåendeSammenlignet.isNegative()
                    && (mellomliggendeSammenlignet.isZero() || mellomliggendeSammenlignet.isNegative());
        }

        private boolean erMellomliggendeSegment(LocalDateSegment<Boolean> segmentUnderVurdering,
                NavigableSet<LocalDateSegment<Boolean>> foregåendeSegmenter,
                NavigableSet<LocalDateSegment<Boolean>> påfølgendeSegmenter) {
            var suvInterval = segmentUnderVurdering.getLocalDateInterval();

            return segmentUnderVurdering.getValue() == null && !foregåendeSegmenter.isEmpty() && !påfølgendeSegmenter.isEmpty()
                && påfølgendeSegmentErIPerioden(påfølgendeSegmenter) && (suvInterval.abuts(foregåendeSegmenter.last().getLocalDateInterval())
                || suvInterval.abuts(påfølgendeSegmenter.first().getLocalDateInterval()));
        }

        private boolean påfølgendeSegmentErIPerioden(NavigableSet<LocalDateSegment<Boolean>> påfølgendeSegmenter) {
            return grunnlag.getOpptjeningPeriode().contains(new LocalDateInterval(påfølgendeSegmenter.first().getFom(), påfølgendeSegmenter.first().getFom()));
        }
    }
}
