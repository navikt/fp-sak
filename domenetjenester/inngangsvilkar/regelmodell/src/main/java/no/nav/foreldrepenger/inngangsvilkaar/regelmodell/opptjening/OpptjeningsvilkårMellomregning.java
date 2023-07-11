package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening;

import java.time.LocalDate;
import java.time.Period;
import java.util.AbstractMap;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;

/**
 * Data underlag inkludert mellomregning og mellomresultater brukt i vilkårsvurderingen.
 */
public class OpptjeningsvilkårMellomregning {

    private final Map<Aktivitet, AktivitetMellomregning> mellomregning = new HashMap<>();

    /** Beregnet total opptjening (inklusiv bekreftet og antatt) */
    private OpptjentTidslinje antattTotalOpptjening;

    /** Beregnet total opptjening (kun bekreftet). */
    private OpptjentTidslinje bekreftetTotalOpptjening;

    /** Beregnet total opptjening. */
    private OpptjentTidslinje totalOpptjening;

    /**
     * Opprinnelig grunnlag.
     */
    private Opptjeningsgrunnlag grunnlag;

    /**
     * Parametre gitt av ytelse.
     */
    private OpptjeningsvilkårParametre regelParametre;

    public OpptjeningsvilkårMellomregning(Opptjeningsgrunnlag grunnlag, OpptjeningsvilkårParametre regelParametre) {
        this.grunnlag = grunnlag;
        this.regelParametre = regelParametre;
        var maxIntervall = grunnlag.getOpptjeningPeriode();

        // grupper aktivitet perioder etter aktivitet og avkort i forhold til angitt startDato/skjæringstidspunkt
        splitAktiviter(
            a -> a.getVurderingsStatus() == null)
                .forEach(e -> mellomregning.computeIfAbsent(e.getKey(),
                    a -> new AktivitetMellomregning(a, e.getValue())));

        splitAktiviter(
            a -> Objects.equals(AktivitetPeriode.VurderingsStatus.TIL_VURDERING, a.getVurderingsStatus()))
                .forEach(e -> mellomregning.computeIfAbsent(e.getKey(),
                    a -> new AktivitetMellomregning(a, e.getValue())));

        splitAktiviter(
            a -> Objects.equals(AktivitetPeriode.VurderingsStatus.VURDERT_GODKJENT, a.getVurderingsStatus()))
                .forEach(e -> mellomregning.computeIfAbsent(e.getKey(),
                    AktivitetMellomregning::new).setAktivitetManueltGodkjent(e.getValue()));

        splitAktiviter(
            a -> Objects.equals(AktivitetPeriode.VurderingsStatus.VURDERT_UNDERKJENT, a.getVurderingsStatus()))
                .forEach(
                    e -> mellomregning.computeIfAbsent(e.getKey(),
                        AktivitetMellomregning::new).setAktivitetManueltUnderkjent(e.getValue()));

        // grupper inntektperioder etter aktivitet og avkort i forhold til angitt startDato/skjæringstidspunkt
        var grupperInntekterEtterAktiitet = Optional.ofNullable(grunnlag.inntektPerioder()).orElse(List.of()).stream().collect(
            Collectors.groupingBy(InntektPeriode::getAktivitet,
                Collectors.mapping(a1 -> new LocalDateSegment<>(a1.getDatoInterval(), a1.getInntektBeløp()), Collectors.toSet())));

        LocalDateSegmentCombinator<Long, Long, Long> inntektOverlapDuplikatCombinator = StandardCombinators::sum;

        var inntektsIntervall = new LocalDateInterval(maxIntervall.getFomDato().minusMonths(1).withDayOfMonth(1), maxIntervall.getTomDato());
        grupperInntekterEtterAktiitet
            .entrySet().stream()
            .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(),
                new LocalDateTimeline<>(e.getValue(), inntektOverlapDuplikatCombinator).intersection(inntektsIntervall)))
            .filter(e -> !e.getValue().isEmpty())
            .forEach(
                e -> mellomregning.computeIfAbsent(e.getKey(),
                    AktivitetMellomregning::new).setInntektTidslinjer(e.getValue()));

    }

    private Stream<Map.Entry<Aktivitet, LocalDateTimeline<Boolean>>> splitAktiviter(Predicate<AktivitetPeriode> filter) {
        var aktiviteter = Optional.ofNullable(grunnlag.aktivitetPerioder()).orElse(List.of()).stream()
            .filter(filter)
            .collect(
                Collectors.groupingBy(AktivitetPeriode::getAktivitet,
                    Collectors.mapping(a -> new LocalDateSegment<>(a.getDatoIntervall(), Boolean.TRUE), Collectors.toSet())));

        LocalDateSegmentCombinator<Boolean, Boolean, Boolean> aktivitetOverlappDuplikatCombinator = StandardCombinators::alwaysTrueForMatch;

        return aktiviteter
            .entrySet().stream()
            .map(e -> (Map.Entry<Aktivitet, LocalDateTimeline<Boolean>>) new AbstractMap.SimpleEntry<>(e.getKey(),
                new LocalDateTimeline<>(e.getValue().stream().sorted(Comparator.comparing(LocalDateSegment::getLocalDateInterval)).toList(), aktivitetOverlappDuplikatCombinator)))
            .filter(e -> !e.getValue().isEmpty());
    }

    public Map<Aktivitet, LocalDateTimeline<Boolean>> getAkseptertMellomliggendePerioder() {
        return getMellomregningTidslinje(AktivitetMellomregning::getAkseptertMellomliggendePerioder);
    }

    private <V> Map<Aktivitet, LocalDateTimeline<V>> getMellomregningTidslinje(Function<AktivitetMellomregning, LocalDateTimeline<V>> fieldGetter) {
        return mellomregning.entrySet().stream()
            .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), fieldGetter.apply(e.getValue())))
            .filter(e -> !e.getValue().isEmpty())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public void setAkseptertMellomliggendePerioder(Map<Aktivitet, LocalDateTimeline<Boolean>> perioder) {
        perioder.forEach((key, value) -> mellomregning.get(key).setAkseptertMellomliggendePerioder(value));
    }

    /**
     * Returnerer aktivitet tidslinjer, uten underkjente perioder (hvis satt), og med valgfritt med/uten antatt
     * godkjente perioder.
     */
    public Map<Aktivitet, LocalDateTimeline<Boolean>> getAktivitetTidslinjer(boolean medAntattGodkjentePerioder, boolean medIkkebekreftedeGodkjentePerioder) {

        return mellomregning
            .entrySet().stream()
            .map(
                e -> new AbstractMap.SimpleEntry<>(e.getKey(),
                    e.getValue().getAktivitetTidslinje(medAntattGodkjentePerioder, medIkkebekreftedeGodkjentePerioder)))
            .filter(e -> !e.getValue().isEmpty())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    }

    public boolean splitOgUnderkjennSegmenterEtterDatoForAktivitet(Aktivitet aktivitet, LocalDate splitDato) {

        if (splitDato.equals(grunnlag.sisteDatoForOpptjening()) || mellomregning.get(aktivitet) == null) {
            return false;
        }
        var underkjennIntervall = new LocalDateInterval(splitDato.plusDays(1), grunnlag.sisteDatoForOpptjening());
        var underkjennTimeline = new LocalDateTimeline<Boolean>(splitDato.plusDays(1), grunnlag.sisteDatoForOpptjening(), Boolean.TRUE);
        var aktivitetMellomregning = mellomregning.get(aktivitet);

        aktivitetMellomregning.setAktivitetUnderkjent(underkjennTimeline);
        if (!AktivitetMellomregning.EMPTY.equals(aktivitetMellomregning.getAktivitetManueltGodkjent())) {
            // Må overskrive manuell godkjenning da annen aktivitet gjerne er vurdert i aksjonspunkt i steg 82
            aktivitetMellomregning.setAktivitetManueltGodkjent(aktivitetMellomregning.getAktivitetManueltGodkjent().disjoint(underkjennIntervall));
        }
        return true;
    }

    private Map<Aktivitet, LocalDateTimeline<Boolean>> getAntattGodkjentPerioder() {
        return getMellomregningTidslinje(AktivitetMellomregning::getAktivitetAntattGodkjent);
    }

    OpptjentTidslinje getAntattTotalOpptjening() {
        return antattTotalOpptjening;
    }

    OpptjentTidslinje getBekreftetOpptjening() {
        return bekreftetTotalOpptjening;
    }

    public Opptjeningsgrunnlag getGrunnlag() {
        return grunnlag;
    }

    public OpptjeningsvilkårParametre getRegelParametre() {
        return regelParametre;
    }

    Map<Aktivitet, LocalDateTimeline<Long>> getInntektTidslinjer() {
        return getMellomregningTidslinje(AktivitetMellomregning::getInntektTidslinjer);
    }

    OpptjentTidslinje getTotalOpptjening() {
        return totalOpptjening;
    }

    public Map<Aktivitet, LocalDateTimeline<Boolean>> getUnderkjentePerioder() {
        return getMellomregningTidslinje(AktivitetMellomregning::getAktivitetUnderkjent);
    }

    public void oppdaterOutputResultat(OpptjeningsvilkårResultat outputResultat) {
        /*
         * tar ikke med antatt godkjent, mellomliggende akseptert eller underkjent i aktivitet returnert her. De angis
         * separat under.
         */
        var opptjeningPeriode = getGrunnlag().getOpptjeningPeriode();
        outputResultat.setBekreftetGodkjentAktivitet(trimTidslinje(this.getAktivitetTidslinjer(false, false), opptjeningPeriode));

        outputResultat.setUnderkjentePerioder(trimTidslinje(this.getUnderkjentePerioder(), opptjeningPeriode));
        outputResultat.setAntattGodkjentePerioder(trimTidslinje(this.getAntattGodkjentPerioder(), opptjeningPeriode));
        outputResultat.setAkseptertMellomliggendePerioder(trimTidslinje(this.getAkseptertMellomliggendePerioder(), opptjeningPeriode));

        /* hvis Oppfylt/Ikke Oppfylt (men ikke "Ikke Vurdert"), så angis total opptjening som er kalkulert. */
        outputResultat.setTotalOpptjening(this.getTotalOpptjening());
    }

    public void setAntattOpptjening(OpptjentTidslinje antattOpptjening) {
        this.antattTotalOpptjening = antattOpptjening;
    }

    public void setBekreftetTotalOpptjening(OpptjentTidslinje opptjening) {
        this.bekreftetTotalOpptjening = opptjening;
    }

    /**
     * Endelig valt opptjeningperiode.
     */
    void setTotalOpptjening(OpptjentTidslinje totalOpptjening) {
        this.totalOpptjening = totalOpptjening;
    }

    void setAntattGodkjentePerioder(Map<Aktivitet, LocalDateTimeline<Boolean>> perioder) {
        perioder.forEach((key, value) -> mellomregning.get(key).setAktivitetAntattGodkjent(value));
    }

    void setUnderkjentePerioder(Map<Aktivitet, LocalDateTimeline<Boolean>> perioder) {
        perioder.forEach((key, value) -> mellomregning.get(key).setAktivitetUnderkjent(value));
    }

    /**
     * Sjekker om opptjening er nok ifht. konfigurert minste periode.
     */
    boolean sjekkErInnenforMinstePeriodeGodkjent(Period opptjeningPeriode) {
        var minsteAntallMåneder = regelParametre.minsteAntallMånederGodkjent();
        var minsteAntallDager = regelParametre.minsteAntallDagerGodkjent();
        return sjekkErErOverAntallPåkrevd(opptjeningPeriode, minsteAntallMåneder, minsteAntallDager);
    }

    private static boolean sjekkErErOverAntallPåkrevd(Period opptjentPeriode, int minsteAntallMåneder,
                                                      int minsteAntallDager) {
        return opptjentPeriode.getMonths() > minsteAntallMåneder
            || opptjentPeriode.getMonths() == minsteAntallMåneder && opptjentPeriode.getDays() >= minsteAntallDager;
    }

    private static Map<Aktivitet, LocalDateTimeline<Boolean>> trimTidslinje(Map<Aktivitet, LocalDateTimeline<Boolean>> tidslinjer,
                                                                            LocalDateInterval maxInterval) {
        return tidslinjer.entrySet().stream()
            .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), e.getValue().intersection(maxInterval)))
            .filter(e -> !e.getValue().isEmpty())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

}
