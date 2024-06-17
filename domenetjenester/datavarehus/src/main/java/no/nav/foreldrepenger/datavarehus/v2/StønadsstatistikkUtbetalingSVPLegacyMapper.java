package no.nav.foreldrepenger.datavarehus.v2;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;

class StønadsstatistikkUtbetalingSVPLegacyMapper {

    private static final BigDecimal HUNDRE = new BigDecimal(100);

    private StønadsstatistikkUtbetalingSVPLegacyMapper() {
    }

    static List<StønadsstatistikkUtbetalingPeriode> mapTilkjent(List<BeregningsresultatPeriode> perioder,
                                                                SvangerskapspengerUttakResultatEntitet uttak) {
        // Grupper på konto, arbeidsgiver og utbetaling - komprimer tidslinjer
        return perioder.stream()
            .filter(p -> p.getDagsats() > 0)
            .flatMap(periode -> mapTilkjentPeriode(periode, uttak))
            .collect(Collectors.groupingBy(s -> new Gruppering(s.getValue())))
            .values()
            .stream()
            .map(LocalDateTimeline::new)
            .map(ldt -> ldt.compress(LocalDateInterval::abutsWorkdays, StønadsstatistikkUtbetalingSVPLegacyMapper::likeNaboer,
                StandardCombinators::leftOnly))
            .flatMap(LocalDateTimeline::stream)
            .map(s -> new StønadsstatistikkUtbetalingPeriode(s.getFom(), s.getTom(), s.getValue().inntektskategori(), s.getValue().arbeidsgiver(),
                s.getValue().mottaker(), s.getValue().dagsats(), s.getValue().utbetalingsgrad()))
            .toList();
    }

    static Stream<LocalDateSegment<UtbetalingSammenlign>> mapTilkjentPeriode(BeregningsresultatPeriode periode,
                                                                             SvangerskapspengerUttakResultatEntitet uttak) {
        // Konverterer alle andeler, grupperer på konto, arbeidsgiver og utbetaling, summerer dagsats og returnerer segment
        return periode.getBeregningsresultatAndelList()
            .stream()
            .filter(a -> a.getDagsats() > 0)
            .map(a -> mapTilkjentAndel(a, uttak))
            .collect(Collectors.groupingBy(Gruppering::new))
            .values()
            .stream()
            .filter(l -> !l.isEmpty())
            .map(l -> mapAndelTilSegment(periode, l));
    }

    private static LocalDateSegment<UtbetalingSammenlign> mapAndelTilSegment(BeregningsresultatPeriode periode,
                                                                             List<UtbetalingSammenlign> utbetalinger) {
        var any = utbetalinger.stream().findFirst().orElseThrow();
        var sumDagsats = utbetalinger.stream().map(UtbetalingSammenlign::dagsats).reduce(0, Integer::sum);
        return new LocalDateSegment<>(periode.getBeregningsresultatPeriodeFom(), periode.getBeregningsresultatPeriodeTom(),
            new UtbetalingSammenlign(any, sumDagsats));
    }

    private static UtbetalingSammenlign mapTilkjentAndel(BeregningsresultatAndel andel, SvangerskapspengerUttakResultatEntitet uttak) {

        var aktuellPeriode = andel.getBeregningsresultatPeriode().getBeregningsresultatPeriodeFom();
        var aktuellUttakPeriode = uttak.getUttaksResultatArbeidsforhold()
            .stream()
            .filter(arbeidsforhold -> Objects.equals(arbeidsforhold.getArbeidsgiver(), andel.getArbeidsgiver().orElse(null)))
            .filter(arbeidsforhold -> andel.getAktivitetStatus().equals(map(arbeidsforhold.getUttakArbeidType())))
            .filter(arbeidsforhold -> arbeidsforhold.getArbeidsforholdRef().gjelderFor(andel.getArbeidsforholdRef()))
            .flatMap(arbeidsforhold -> arbeidsforhold.getPerioder().stream())
            .filter(periode -> periode.getTidsperiode().inkluderer(aktuellPeriode))
            .findFirst()
            .orElseThrow();

        return new UtbetalingSammenlign(mapInntektskategori(andel), andel.getArbeidsgiver().map(Arbeidsgiver::getIdentifikator).orElse(null),
            mapMottaker(andel), andel.getDagsats(), aktuellUttakPeriode.getUtbetalingsgrad().decimalValue());
    }

    public static AktivitetStatus map(UttakArbeidType uttakArbeidType) {
        return switch (uttakArbeidType) {
            case ORDINÆRT_ARBEID -> AktivitetStatus.ARBEIDSTAKER;
            case SELVSTENDIG_NÆRINGSDRIVENDE -> AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE;
            case FRILANS -> AktivitetStatus.FRILANSER;
            case ANNET -> throw new IllegalArgumentException("UttakArbeidType er ugyldig!" + uttakArbeidType);
        };
    }

    // For å få til compress - må ha equals som gjøre BigDecimal.compareTo
    private static boolean likeNaboer(UtbetalingSammenlign u1, UtbetalingSammenlign u2) {
        return u1.inntektskategori() == u2.inntektskategori() && Objects.equals(u1.arbeidsgiver(), u2.arbeidsgiver())
            && u1.mottaker() == u2.mottaker() && u1.dagsats() == u2.dagsats() && u1.utbetalingsgrad().compareTo(u2.utbetalingsgrad()) == 0;
    }

    private static StønadsstatistikkUtbetalingPeriode.Inntektskategori mapInntektskategori(BeregningsresultatAndel andel) {
        return switch (andel.getInntektskategori()) {
            case ARBEIDSTAKER -> StønadsstatistikkUtbetalingPeriode.Inntektskategori.ARBEIDSTAKER;
            case FRILANSER -> StønadsstatistikkUtbetalingPeriode.Inntektskategori.FRILANSER;
            case SELVSTENDIG_NÆRINGSDRIVENDE -> StønadsstatistikkUtbetalingPeriode.Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE;
            case DAGPENGER -> StønadsstatistikkUtbetalingPeriode.Inntektskategori.DAGPENGER;
            case ARBEIDSAVKLARINGSPENGER -> StønadsstatistikkUtbetalingPeriode.Inntektskategori.ARBEIDSAVKLARINGSPENGER;
            case SJØMANN -> StønadsstatistikkUtbetalingPeriode.Inntektskategori.SJØMANN;
            case DAGMAMMA -> StønadsstatistikkUtbetalingPeriode.Inntektskategori.DAGMAMMA;
            case JORDBRUKER -> StønadsstatistikkUtbetalingPeriode.Inntektskategori.JORDBRUKER;
            case FISKER -> StønadsstatistikkUtbetalingPeriode.Inntektskategori.FISKER;
            case ARBEIDSTAKER_UTEN_FERIEPENGER -> StønadsstatistikkUtbetalingPeriode.Inntektskategori.ARBEIDSTAKER_UTEN_FERIEPENGER;
            case UDEFINERT -> throw new IllegalStateException("Skal ikke forekomme");
        };
    }

    private static StønadsstatistikkUtbetalingPeriode.Mottaker mapMottaker(BeregningsresultatAndel andel) {
        return andel.erBrukerMottaker() ? StønadsstatistikkUtbetalingPeriode.Mottaker.BRUKER : StønadsstatistikkUtbetalingPeriode.Mottaker.ARBEIDSGIVER;
    }

    // Bruker int for å slippe hash-problematikk rundt BigDecimal
    private record Gruppering(StønadsstatistikkUtbetalingPeriode.Inntektskategori klassekode, String arbeidsgiver,
                              StønadsstatistikkUtbetalingPeriode.Mottaker mottaker, int utbetalingsgrad) {
        Gruppering(UtbetalingSammenlign utbetaling) {
            this(utbetaling.inntektskategori(), utbetaling.arbeidsgiver(), utbetaling.mottaker,
                utbetaling.utbetalingsgrad().multiply(HUNDRE).setScale(0, RoundingMode.HALF_EVEN).intValue());
        }
    }

    private record UtbetalingSammenlign(StønadsstatistikkUtbetalingPeriode.Inntektskategori inntektskategori, String arbeidsgiver,
                                        StønadsstatistikkUtbetalingPeriode.Mottaker mottaker, int dagsats, BigDecimal utbetalingsgrad) {
        UtbetalingSammenlign(UtbetalingSammenlign utbetaling, int dagsats) {
            this(utbetaling.inntektskategori(), utbetaling.arbeidsgiver(), utbetaling.mottaker(), dagsats, utbetaling.utbetalingsgrad());
        }
    }

}
