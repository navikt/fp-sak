package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening;

import java.time.LocalDate;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.fp.OpptjeningsvilkårForeldrepenger;
import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.LocalDateTimeline.JoinStyle;
import no.nav.fpsak.tidsserie.StandardCombinators;

/**
 * Regel som sjekker om det finnes registrerte inntekter for de periodene arbeid er innrapportert, for samme
 * arbeidsgiver.
 * Perioder der det ikke finnes inntekter underkjennes som aktivitet.
 */
@RuleDocumentation("FP_VK_23.1.1")
public class SjekkInntektSamsvarerMedArbeidAktivitet extends LeafSpecification<OpptjeningsvilkårMellomregning> {
    public static final String ID = SjekkInntektSamsvarerMedArbeidAktivitet.class.getSimpleName();

    private static final String ARBEID = OpptjeningsvilkårForeldrepenger.ARBEID;
    private static final String FRILANSREGISTER = OpptjeningsvilkårForeldrepenger.FRILANSREGISTER;

    public SjekkInntektSamsvarerMedArbeidAktivitet() {
        super(ID);
    }

    @Override
    public Evaluation evaluate(OpptjeningsvilkårMellomregning data) {

        var ikkeGodkjent = finnPerioderSomIkkeHarNokInntektForOpplystArbeid(data);

        var sisteAntattGodkjentDato = sisteAntattGodkjentDato(data);


        // regn utførste dato for antatt godkjent bakover
        var periodeAntattGodkjentAksepteres = data.getRegelParametre().periodeAntattGodkjentFørBehandlingstidspunkt();
        var førsteDatoForAntattGodkjent = sisteAntattGodkjentDato
            .plusMonths(1).withDayOfMonth(1) // Periode P2M blir denne måneden (enn så lenge) og forrige måned
            .minus(periodeAntattGodkjentAksepteres);

        var antattGodkjentInterval = new LocalDateInterval(førsteDatoForAntattGodkjent, sisteAntattGodkjentDato);

        var antaGodkjent = new AntaGodkjent(antattGodkjentInterval, ikkeGodkjent);

        var opptjeningPeriode = data.getGrunnlag().getOpptjeningPeriode();

        data.setAntattGodkjentePerioder(antaGodkjent.getAntattGodkjentResultat(opptjeningPeriode));
        data.setUnderkjentePerioder(antaGodkjent.getUnderkjentResultat(opptjeningPeriode));

        Evaluation evaluation = ja();

        evaluation.setEvaluationProperty(OpptjeningsvilkårForeldrepenger.EVAL_RESULT_UNDERKJENTE_PERIODER, data.getUnderkjentePerioder());

        return evaluation;
    }

    private static LocalDate sisteAntattGodkjentDato(OpptjeningsvilkårMellomregning data) {
        if(data.getGrunnlag().behandlingsDato().isAfter(data.getGrunnlag().sisteDatoForOpptjening())) {
            return data.getGrunnlag().behandlingsDato();
        }
        return data.getGrunnlag().sisteDatoForOpptjening();
    }

    private Map<Aktivitet, LocalDateTimeline<AktivitetStatus>> finnPerioderSomIkkeHarNokInntektForOpplystArbeid(OpptjeningsvilkårMellomregning data) {

        var aktiviteter = data.getAktivitetTidslinjer(false, false);
        var inntekter = data.getInntektTidslinjer();
        var grunnlag = data.getGrunnlag();
        var underkjennPerioder = new UnderkjennPerioder(inntekter, data.getRegelParametre().minsteInntekt());

        aktiviteter.entrySet().stream()
            .filter(e -> ARBEID.equals(e.getKey().getAktivitetType()) || FRILANSREGISTER.equals(e.getKey().getAktivitetType()))
            .forEach(underkjennPerioder::underkjennPeriode);

        return underkjennPerioder.getUnderkjentePerioder();
    }

    /**
     * Underkjenn perioder som ikke matcher filter funksjon
     */
    private static class UnderkjennPerioder {
        private final Map<Aktivitet, LocalDateTimeline<AktivitetStatus>> underkjentePerioder = new HashMap<>();
        private Map<Aktivitet, LocalDateTimeline<Long>> inntekter;
        private Long minsteInntekt;

        UnderkjennPerioder(Map<Aktivitet, LocalDateTimeline<Long>> inntekter, Long minsteInntekt) {
            this.inntekter = inntekter;
            this.minsteInntekt = minsteInntekt;
        }

        Map<Aktivitet, LocalDateTimeline<AktivitetStatus>> getUnderkjentePerioder() {
            return Collections.unmodifiableMap(underkjentePerioder);
        }

        void underkjennPeriode(Entry<Aktivitet, LocalDateTimeline<Boolean>> e) {
            var key = e.getKey().forInntekt();
            var arbeid = e.getValue();
            LocalDateTimeline<AktivitetStatus> periode;
            if (!inntekter.containsKey(key)) {
                periode = arbeid.mapValue(a -> AktivitetStatus.IKKE_GODKJENT);
            } else {

                var inntekt = inntekter.get(key);

                var okInntekt = inntekt
                    .filterValue(this::filtrerInntektFunksjon);

                var okArbeid = okInntekt.intersection(arbeid,
                    StandardCombinators::rightOnly);

                var underkjentArbeid = arbeid.disjoint(okArbeid,
                    StandardCombinators::leftOnly);

                periode = underkjentArbeid.mapValue(a -> AktivitetStatus.IKKE_GODKJENT);
            }

            underkjentePerioder.put(e.getKey(), periode);

        }

        private boolean filtrerInntektFunksjon(Long inntekt) {
            /* må ha minst inntekt */
            return inntekt >= minsteInntekt;
        }

    }

    /**
     * FYll antatt godkjente intervaller for arbeid som har blitt underkjent innen angitt interval.
     */
    private static class AntaGodkjent {
        /**
         * Mulig intervall med antatt godkjent.
         */
        private final LocalDateTimeline<AktivitetStatus> antattGodkjent;
        private final Map<Aktivitet, LocalDateTimeline<AktivitetStatus>> medAntattGodkjentFramforIkkeGodkjent = new LinkedHashMap<>();

        /**
         * @param antattGodkjentInterval - interval der arbeid skal antas godkjent selv om det er underkjent av tidligere regler ang. krav
         *            til inntekt.
         * @param aktiviteter
         */
        AntaGodkjent(final LocalDateInterval antattGodkjentInterval, final Map<Aktivitet, LocalDateTimeline<AktivitetStatus>> aktiviteter) {
            antattGodkjent = new LocalDateTimeline<>(antattGodkjentInterval, AktivitetStatus.ANTATT_GODKJENT);

            aktiviteter.entrySet().stream()
                .filter(e -> ARBEID.equals(e.getKey().getAktivitetType()))
                .forEach(this::fyllAntattGodkjent);

            // Denne er for å få med underkjente (aktiviteter)
            // Frilansperioder uten inntekt blir ikke antatt godkjent - inntil videre
            // Obs på 1) ytelser som godkjenner antatt o
            aktiviteter.entrySet().stream()
                .filter(e -> FRILANSREGISTER.equals(e.getKey().getAktivitetType()))
                .filter(e -> !e.getValue().isEmpty())
                .forEach(e -> medAntattGodkjentFramforIkkeGodkjent.put(e.getKey(), e.getValue()));

        }

        private void fyllAntattGodkjent(Entry<Aktivitet, LocalDateTimeline<AktivitetStatus>> e) {
            var key = e.getKey();
            var arbeid = e.getValue();

            // la antatt godkjent overstyre ikke godkjent.
            var medAntattGodkjent = arbeid.combine(antattGodkjent,
                this::antaGodkjentFramforIkkeGodkjent, JoinStyle.LEFT_JOIN);

            if (!medAntattGodkjent.isEmpty()) {
                medAntattGodkjentFramforIkkeGodkjent.put(key, medAntattGodkjent);
            }
        }

        private LocalDateSegment<AktivitetStatus> antaGodkjentFramforIkkeGodkjent(LocalDateInterval di,
                                                                                  LocalDateSegment<AktivitetStatus> lhs,
                                                                                  LocalDateSegment<AktivitetStatus> rhs) {
            var nyStatus = lhs == null || Objects.equals(lhs.getValue(), AktivitetStatus.IKKE_GODKJENT) ?
                rhs == null ? lhs == null ? null : lhs.getValue() : rhs.getValue() : lhs.getValue();
            return new LocalDateSegment<>(di, nyStatus);
        }

        Map<Aktivitet, LocalDateTimeline<Boolean>> getAntattGodkjentResultat(LocalDateInterval interval) {
            var resultat = medAntattGodkjentFramforIkkeGodkjent.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> filtrertForStatus(e.getValue(), AktivitetStatus.ANTATT_GODKJENT)));

            return avgrensTilPeriode(resultat, interval);
        }

        Map<Aktivitet, LocalDateTimeline<Boolean>> getUnderkjentResultat(LocalDateInterval interval) {
            var resultat = medAntattGodkjentFramforIkkeGodkjent.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> filtrertForStatus(e.getValue(), AktivitetStatus.IKKE_GODKJENT)));
            return avgrensTilPeriode(resultat, interval);
        }

        /** avgrens til angitt interval og fjern tomme tidslinjer */
        private static Map<Aktivitet, LocalDateTimeline<Boolean>> avgrensTilPeriode(Map<Aktivitet, LocalDateTimeline<Boolean>> tidslinjer,
                                                                                    LocalDateInterval interval) {

            return tidslinjer
                .entrySet()
                .stream()
                .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), e.getValue().intersection(interval)))
                .filter(e -> !e.getValue().isEmpty())
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
        }

        private static LocalDateTimeline<Boolean> filtrertForStatus(LocalDateTimeline<AktivitetStatus> tidslinje, AktivitetStatus aktivitetStatus) {
            return tidslinje.filterValue(a -> Objects.equals(a, aktivitetStatus)).mapValue(a -> Boolean.TRUE);
        }

    }

}
