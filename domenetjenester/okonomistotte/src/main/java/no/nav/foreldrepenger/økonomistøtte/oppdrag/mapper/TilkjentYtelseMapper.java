package no.nav.foreldrepenger.økonomistøtte.oppdrag.mapper;

import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatFeriepengerPrÅr;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.FamilieYtelseType;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.Betalingsmottaker;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.KjedeNøkkel;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.Periode;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.Satsen;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.Utbetalingsgrad;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.Ytelse;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.YtelsePeriode;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.YtelseVerdi;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.samlinger.GruppertYtelse;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;

public class TilkjentYtelseMapper {

    private final FamilieYtelseType ytelseType;
    private final LocalDate feriepengerDødsdato; // Feriepenger utbetales umiddelbart dersom bruker dør

    public TilkjentYtelseMapper(FamilieYtelseType ytelseType) {
        this(ytelseType, null);
    }

    public TilkjentYtelseMapper(FamilieYtelseType ytelseType, LocalDate feriepengerDødsdato) {
        this.ytelseType = ytelseType;
        this.feriepengerDødsdato = feriepengerDødsdato;
    }

    public static TilkjentYtelseMapper lagFor(FamilieYtelseType familieYtelseType) {
        return new TilkjentYtelseMapper(familieYtelseType, null);
    }

    public static TilkjentYtelseMapper lagFor(FamilieYtelseType familieYtelseType, LocalDate feriepengerDødsdato) {
        return new TilkjentYtelseMapper(familieYtelseType, feriepengerDødsdato);
    }

    public GruppertYtelse fordelPåNøkler(BeregningsresultatEntitet tilkjentYtelse) {
        if (tilkjentYtelse == null) {
            return GruppertYtelse.TOM;
        }
        return fordelPåNøkler(tilkjentYtelse.getBeregningsresultatPerioder());
    }

    public GruppertYtelse fordelPåNøkler(List<BeregningsresultatPeriode> tilkjentYtelsePerioder) {
        Map<KjedeNøkkel, Ytelse> resultat = new HashMap<>();
        resultat.putAll(fordelYtelsePåNøkler(tilkjentYtelsePerioder));
        resultat.putAll(fordelFeriepengerPåNøkler(tilkjentYtelsePerioder));

        return new GruppertYtelse(resultat);
    }

    public Map<KjedeNøkkel, Ytelse> fordelYtelsePåNøkler(List<BeregningsresultatPeriode> tilkjentYtelsePerioder) {
        Map<KjedeNøkkel, Ytelse.Builder> buildere = new HashMap<>();

        for (var tyPeriode : sortert(tilkjentYtelsePerioder)) {
            var andelPrNøkkel = tyPeriode.getBeregningsresultatAndelList()
                .stream()
                .filter(andel -> andel.getDagsats() != 0)
                .map(andel -> tilYtelsePeriodeMedNøkkel(tyPeriode, andel))
                .collect(Collectors.groupingBy(YtelsePeriodeMedNøkkel::getNøkkel));
            for (var entry : andelPrNøkkel.entrySet()) {
                var nøkkel = entry.getKey();
                var ytelsePeriode = YtelsePeriode.summer(entry.getValue(), YtelsePeriodeMedNøkkel::getYtelsePeriode);
                var builder = buildere.computeIfAbsent(nøkkel, kjedeNøkkel -> Ytelse.builder());
                builder.leggTilPeriode(ytelsePeriode);
            }
        }

        return build(buildere);
    }

    public Map<KjedeNøkkel, Ytelse> fordelFeriepengerPåNøkler(Collection<BeregningsresultatPeriode> tilkjentYtelsePerioder) {
        List<YtelsePeriodeMedNøkkel> alleFeriepenger = new ArrayList<>();
        for (var periode : sortert(tilkjentYtelsePerioder)) {
            for (var andel : periode.getBeregningsresultatAndelList()) {
                for (var feriepenger : andel.getBeregningsresultatFeriepengerPrÅrListe()) {
                    var nøkkel = tilNøkkelFeriepenger(andel, feriepenger.getOpptjeningsåret());
                    var ytelsePeriode = lagYtelsePeriodeForFeriepenger(andel, feriepenger);
                    alleFeriepenger.add(new YtelsePeriodeMedNøkkel(nøkkel, ytelsePeriode));
                }
            }
        }
        var feriepengerPrNøkkel = alleFeriepenger.stream().collect(Collectors.groupingBy(YtelsePeriodeMedNøkkel::getNøkkel));

        Map<KjedeNøkkel, Ytelse> resultat = new HashMap<>();
        for (var entry : feriepengerPrNøkkel.entrySet()) {
            var nøkkel = entry.getKey();
            var ytelsePeriode = YtelsePeriode.summer(entry.getValue(), YtelsePeriodeMedNøkkel::getYtelsePeriode);
            resultat.put(nøkkel, Ytelse.builder().leggTilPeriode(ytelsePeriode).build());
        }
        return resultat;
    }

    private YtelsePeriode lagYtelsePeriodeForFeriepenger(BeregningsresultatAndel andel, BeregningsresultatFeriepengerPrÅr feriepenger) {
        var tilBruker = andel.skalTilBrukerEllerPrivatperson();
        return new YtelsePeriode(beregnFeriepengePeriode(feriepenger.getOpptjeningsåret(), tilBruker),
            Satsen.engang(feriepenger.getÅrsbeløp().getVerdi().intValueExact()));
    }

    private YtelsePeriodeMedNøkkel tilYtelsePeriodeMedNøkkel(BeregningsresultatPeriode periode, BeregningsresultatAndel andel) {
        return new YtelsePeriodeMedNøkkel(tilNøkkel(andel), tilYtelsePeriode(periode, andel));
    }

    private YtelsePeriode tilYtelsePeriode(BeregningsresultatPeriode periode, BeregningsresultatAndel andel) {
        return new YtelsePeriode(Periode.of(periode.getBeregningsresultatPeriodeFom(), periode.getBeregningsresultatPeriodeTom()),
            Satsen.dagsats(andel.getDagsats()), tilUtbetalingsgrad(andel));
    }

    private Utbetalingsgrad tilUtbetalingsgrad(BeregningsresultatAndel andel) {
        if (andel.getUtbetalingsgrad() == null) {
            return null;
        }
        return new Utbetalingsgrad(andel.getUtbetalingsgrad().intValue());
    }

    private KjedeNøkkel tilNøkkel(BeregningsresultatAndel andel) {
        var tilBruker = andel.skalTilBrukerEllerPrivatperson();
        return tilBruker ? KjedeNøkkel.lag(KlassekodeUtleder.utled(andel, ytelseType), Betalingsmottaker.BRUKER) :
            KjedeNøkkel.lag(KlassekodeUtleder.utled(andel, ytelseType), Betalingsmottaker.forArbeidsgiver(andel.getArbeidsgiver().orElseThrow().getOrgnr()));
    }

    private KjedeNøkkel tilNøkkelFeriepenger(BeregningsresultatAndel andel, int opptjeningsår) {
        var tilBruker = andel.skalTilBrukerEllerPrivatperson();
        var brukferiepengerMaksdato = beregnFeriepengePeriode(opptjeningsår, tilBruker).getTom();
        var klasseKode = tilBruker ? KlassekodeUtleder.utledForFeriepenger(ytelseType, opptjeningsår, feriepengerDødsdato) :
            KlassekodeUtleder.utledForFeriepengeRefusjon(ytelseType);
        return tilBruker ? KjedeNøkkel.lag(klasseKode, Betalingsmottaker.BRUKER, brukferiepengerMaksdato) :
            KjedeNøkkel.lag(klasseKode, Betalingsmottaker.forArbeidsgiver(andel.getArbeidsgiver().orElseThrow().getOrgnr()), brukferiepengerMaksdato);
    }

    private Periode beregnFeriepengePeriode(int opptjeningsår, boolean tilBruker) {
        var feriepengerMaksdato = LocalDate.ofYearDay(opptjeningsår + 1 ,1 ).with(KjedeNøkkel.SLUTT_FERIEPENGER);
        // Midlertidig disable logikk for privatpersoner inntil avklart klassekode for tilfelle dødsfall
        // Når enables - så fjerne Ignore på 3 tester i NyOppdragskontrollTjenesteFeriepengerMedFlereRevurderingerTest
        if (!tilBruker && feriepengerDødsdato != null && !feriepengerDødsdato.isAfter(feriepengerMaksdato)) {
            // For å sikre korrekt opphør og lage riktige oppdrag ved oppstart okt-des og dødsfall påfølgende mai.
            var brukMåned = Month.MAY.equals(feriepengerDødsdato.getMonth()) ? feriepengerDødsdato.minusMonths(1) : feriepengerDødsdato;
            return new Periode(brukMåned.with(TemporalAdjusters.firstDayOfMonth()), brukMåned.with(TemporalAdjusters.lastDayOfMonth()));
        }
        return new Periode(feriepengerMaksdato.with(TemporalAdjusters.firstDayOfMonth()), feriepengerMaksdato);
    }

    private Map<KjedeNøkkel, Ytelse> build(Map<KjedeNøkkel, Ytelse.Builder> buildere) {
        Map<KjedeNøkkel, Ytelse> kjeder = new HashMap<>();
        for (var entry : buildere.entrySet()) {
            var komprimertYtelse = Ytelse.builder();
            entry.getValue().build().getPerioder().stream()
                .map(p -> new LocalDateSegment<>(p.getPeriode().fom(), p.getPeriode().tom(), p.getVerdi()))
                .collect(Collectors.collectingAndThen(Collectors.toList(), TilkjentYtelseMapper::komprimerPerioder)) // Samle og komprimer og strøm
                .map(s -> new YtelsePeriode(new Periode(s.getFom(), s.getTom()), s.getValue()))
                .forEach(komprimertYtelse::leggTilPeriode);
            kjeder.put(entry.getKey(), komprimertYtelse.build());
        }
        return kjeder;
    }

    private static Stream<LocalDateSegment<YtelseVerdi>> komprimerPerioder(List<LocalDateSegment<YtelseVerdi>> segmenter) {
        return new LocalDateTimeline<>(segmenter).compress(LocalDateInterval::abutsWorkdays, YtelseVerdi::equals, StandardCombinators::leftOnly).stream();
    }

    private static List<BeregningsresultatPeriode> sortert(Collection<BeregningsresultatPeriode> usortert) {
        return usortert.stream().sorted(Comparator.comparing(BeregningsresultatPeriode::getBeregningsresultatPeriodeFom)).toList();
    }

}
