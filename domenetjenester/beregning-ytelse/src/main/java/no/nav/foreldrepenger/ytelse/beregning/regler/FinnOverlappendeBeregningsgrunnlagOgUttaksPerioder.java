package no.nav.foreldrepenger.ytelse.beregning.regler;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.ytelse.beregning.BeregningsgrunnlagUttakArbeidsforholdMatcher;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatAndel;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatPeriode;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatRegelmodellMellomregning;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.UttakAktivitet;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.UttakResultat;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.UttakResultatPeriode;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.AktivitetStatus;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Beregningsgrunnlag;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.BeregningsgrunnlagPrArbeidsforhold;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.BeregningsgrunnlagPrStatus;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

class FinnOverlappendeBeregningsgrunnlagOgUttaksPerioder extends LeafSpecification<BeregningsresultatRegelmodellMellomregning> {
    public static final String ID = "FP_BR 20_1";
    public static final String BESKRIVELSE = "FinnOverlappendeBeregningsgrunnlagOgUttaksPerioder";

    private static final String BRUKER_ANDEL = ".brukerAndel";
    private static final String ARBEIDSGIVERS_ANDEL = ".arbeidsgiverAndel";
    private static final String DAGSATS_BRUKER = ".dagsatsBruker";
    private static final String DAGSATS_ARBEIDSGIVER = ".dagsatsArbeidsgiver";
    private static final String ARBEIDSGIVER_ID = ".arbeidsgiverId";

    private static final BigDecimal FULL_STILLING = BigDecimal.valueOf(100);

    FinnOverlappendeBeregningsgrunnlagOgUttaksPerioder() {
        super(ID, BESKRIVELSE);
    }

    @Override
    public Evaluation evaluate(BeregningsresultatRegelmodellMellomregning mellomregning) {
        //Regelsporing
        Map<String, Object> resultater = new LinkedHashMap<>();

        var regelmodell = mellomregning.getInput();
        var grunnlag = regelmodell.getBeregningsgrunnlag();
        var uttakResultat = regelmodell.getUttakResultat();
        var periodeListe = mapPerioder(grunnlag, uttakResultat, resultater);
        periodeListe.forEach(p -> mellomregning.getOutput().addBeregningsresultatPeriode(p));
        return beregnet(resultater);
    }

    private List<BeregningsresultatPeriode> mapPerioder(Beregningsgrunnlag grunnlag, UttakResultat uttakResultat, Map<String, Object> resultater) {
        var grunnlagTimeline = mapGrunnlagTimeline(grunnlag);
        var uttakTimeline = uttakResultat.getUttakPeriodeTimeline();
        var resultatTimeline = intersectTimelines(grunnlagTimeline, uttakTimeline, resultater)
            .compress();
        return resultatTimeline.toSegments().stream().map(LocalDateSegment::getValue).collect(Collectors.toList());
    }

    private LocalDateTimeline<BeregningsresultatPeriode> intersectTimelines(LocalDateTimeline<BeregningsgrunnlagPeriode> grunnlagTimeline, LocalDateTimeline<UttakResultatPeriode> uttakTimeline, Map<String, Object> resultater) {
        final var i = new int[]{0}; //Periode-teller til regelsporing
        return grunnlagTimeline.intersection(uttakTimeline, (dateInterval, grunnlagSegment, uttakSegment) ->
        {
            var resultatPeriode = BeregningsresultatPeriode.builder()
                .medPeriode(dateInterval).build();

            //Regelsporing
            var periodeNavn = "BeregningsresultatPeriode[" + i[0] + "]";
            resultater.put(periodeNavn + ".fom", dateInterval.getFomDato());
            resultater.put(periodeNavn + ".tom", dateInterval.getTomDato());

            var grunnlag = grunnlagSegment.getValue();
            var uttakResultatPeriode = uttakSegment.getValue();

            grunnlag.getBeregningsgrunnlagPrStatus(AktivitetStatus.ATFL).forEach(gbps -> {
                // for hver arbeidstaker andel: map fra grunnlag til 1-2 resultatAndel
                var arbeidsforholdList = gbps.getArbeidsforhold();
                arbeidsforholdList.forEach(a -> opprettBeregningsresultatAndelerATFL(a, resultatPeriode, resultater, periodeNavn, uttakResultatPeriode));
            });
            grunnlag.getBeregningsgrunnlagPrStatus().stream()
                .filter(bgps -> !AktivitetStatus.ATFL.equals(bgps.getAktivitetStatus()))
                .forEach(bgps -> opprettBeregningsresultatAndelerGenerell(bgps, resultatPeriode, resultater, periodeNavn, uttakResultatPeriode));

            i[0]++;
            return new LocalDateSegment<>(dateInterval, resultatPeriode);
        });
    }

    private void opprettBeregningsresultatAndelerGenerell(BeregningsgrunnlagPrStatus beregningsgrunnlagPrStatus, BeregningsresultatPeriode resultatPeriode,
                                                          Map<String, Object> resultater, String periodeNavn, UttakResultatPeriode uttakResultatPeriode) {
        if (uttakResultatPeriode.getErOppholdsPeriode()) {
            return;
        }
        var uttakAktivitetOpt = matchUttakAktivitetMedBeregningsgrunnlagPrStatus(beregningsgrunnlagPrStatus, uttakResultatPeriode.getUttakAktiviteter());
        if (uttakAktivitetOpt.isEmpty()) {
            return;
        }
        var uttakAktivitet = uttakAktivitetOpt.get();

        //Gradering
        var dagsatser = kalkulerDagsatserForGradering(beregningsgrunnlagPrStatus.getRedusertBrukersAndelPrÅr(), BigDecimal.ZERO,
            uttakAktivitet, resultater, periodeNavn);
        var dagsatsBruker = dagsatser.bruker();

        resultatPeriode.addBeregningsresultatAndel(
            BeregningsresultatAndel.builder()
                .medBrukerErMottaker(true)
                .medDagsats(dagsatsBruker)
                .medDagsatsFraBg(årsbeløpTilDagsats(beregningsgrunnlagPrStatus.getRedusertBrukersAndelPrÅr()))
                .medAktivitetStatus(beregningsgrunnlagPrStatus.getAktivitetStatus())
                .medInntektskategori(beregningsgrunnlagPrStatus.getInntektskategori())
                .medUtbetalingssgrad(uttakAktivitet.getUtbetalingsgrad())
                .medStillingsprosent(uttakAktivitet.getStillingsgrad())
                .build(resultatPeriode));

        // Regelsporing
        var beskrivelse = periodeNavn + BRUKER_ANDEL + "['" + beregningsgrunnlagPrStatus.getAktivitetStatus().name() + "']" + DAGSATS_BRUKER;
        resultater.put(beskrivelse, dagsatsBruker);

    }

    private Optional<UttakAktivitet> matchUttakAktivitetMedBeregningsgrunnlagPrStatus(BeregningsgrunnlagPrStatus beregningsgrunnlagPrStatus, List<UttakAktivitet> uttakAktiviteter) {
        return uttakAktiviteter.stream()
            .filter(uttakAndel -> BeregningsgrunnlagUttakArbeidsforholdMatcher.matcherGenerellAndel(beregningsgrunnlagPrStatus, uttakAndel))
            .findFirst();
    }

    private void opprettBeregningsresultatAndelerATFL(BeregningsgrunnlagPrArbeidsforhold arbeidsforhold, BeregningsresultatPeriode resultatPeriode,
                                                      Map<String, Object> resultater, String periodeNavn, UttakResultatPeriode uttakResultatPeriode) {
        if (uttakResultatPeriode.getErOppholdsPeriode()) {
            return;
        }
        var uttakAktivitetOpt = matchUttakAktivitetMedArbeidsforhold(uttakResultatPeriode.getUttakAktiviteter(), arbeidsforhold);
        if (uttakAktivitetOpt.isEmpty()) {
            return;
        }
        var uttakAktivitet = uttakAktivitetOpt.get();
        var arbeidsgiverId = arbeidsforhold.getArbeidsgiverId();

        //Gradering
        var dagsatser = kalkulerDagsatserForGradering(arbeidsforhold.getRedusertBrukersAndelPrÅr(), arbeidsforhold.getRedusertRefusjonPrÅr(),
            uttakAktivitet, resultater, periodeNavn);

        var dagsatsBruker = dagsatser.bruker();
        var dagsatsArbeidsgiver = dagsatser.arbeidsgiver();

        resultatPeriode.addBeregningsresultatAndel(
            BeregningsresultatAndel.builder()
                .medArbeidsforhold(arbeidsforhold.getArbeidsforhold())
                .medBrukerErMottaker(true)
                .medStillingsprosent(uttakAktivitet.getStillingsgrad())
                .medUtbetalingssgrad(uttakAktivitet.getUtbetalingsgrad())
                .medDagsats(dagsatsBruker)
                .medDagsatsFraBg(arbeidsforhold.getDagsatsBruker())
                .medAktivitetStatus(AktivitetStatus.ATFL)
                .medInntektskategori(arbeidsforhold.getInntektskategori())
                .build(resultatPeriode));

        // Regelsporing
        resultater.put(periodeNavn + BRUKER_ANDEL + "['" + arbeidsgiverId + "']" + ARBEIDSGIVER_ID, arbeidsgiverId);
        resultater.put(periodeNavn + BRUKER_ANDEL + "['" + arbeidsgiverId + "']" + DAGSATS_BRUKER, dagsatsBruker);
        resultater.put(periodeNavn + BRUKER_ANDEL + "['" + arbeidsgiverId + "']" + ".dagsatsFraBeregningsgrunnlagBruker", arbeidsforhold.getDagsatsBruker());

        if (arbeidsforhold.getDagsatsArbeidsgiver() != null && arbeidsforhold.getDagsatsArbeidsgiver() > 0) {
            resultatPeriode.addBeregningsresultatAndel(
                BeregningsresultatAndel.builder()
                    .medArbeidsforhold(arbeidsforhold.getArbeidsforhold())
                    .medBrukerErMottaker(false)
                    .medStillingsprosent(uttakAktivitet.getStillingsgrad())
                    .medUtbetalingssgrad(uttakAktivitet.getUtbetalingsgrad())
                    .medDagsats(dagsatsArbeidsgiver)
                    .medDagsatsFraBg(arbeidsforhold.getDagsatsArbeidsgiver())
                    .medInntektskategori(arbeidsforhold.getInntektskategori())
                    .medAktivitetStatus(AktivitetStatus.ATFL)
                    .build(resultatPeriode));

            //Regelsporing
            resultater.put(periodeNavn + ARBEIDSGIVERS_ANDEL + "['" + arbeidsgiverId + "']" + ARBEIDSGIVER_ID, arbeidsgiverId);
            resultater.put(periodeNavn + ARBEIDSGIVERS_ANDEL + "['" + arbeidsgiverId + "']" + DAGSATS_ARBEIDSGIVER, dagsatsArbeidsgiver);
            resultater.put(periodeNavn + ARBEIDSGIVERS_ANDEL + "['" + arbeidsgiverId + "']" + ".dagsatsFraBeregningsgrunnlagArbeidsgiver", arbeidsforhold.getDagsatsArbeidsgiver());
        }
    }

    private Optional<UttakAktivitet> matchUttakAktivitetMedArbeidsforhold(List<UttakAktivitet> uttakAktiviteter, BeregningsgrunnlagPrArbeidsforhold bgAndel) {
        return uttakAktiviteter
            .stream()
            .filter(uttakAktivitet -> BeregningsgrunnlagUttakArbeidsforholdMatcher.matcherArbeidsforhold(uttakAktivitet.getArbeidsforhold(), bgAndel.getArbeidsforhold()))
            .findFirst();
    }

    /*
     * dagsatser gradert for bruker og arbeidsgiver
     */
    private record DagsatsBrukerAG(Long bruker, Long arbeidsgiver) {}

    private static DagsatsBrukerAG kalkulerDagsatserForGradering(BigDecimal redusertBrukersAndelPrÅr, BigDecimal redusertRefusjonPrÅr,
                                                                   UttakAktivitet uttakAktivitet, Map<String, Object> resultater, String periodenavn) {
        if (uttakAktivitet.getUtbetalingsgrad().compareTo(BigDecimal.ZERO) == 0) {
            resultater.put(periodenavn + ".utbetalingsgrad", uttakAktivitet.getUtbetalingsgrad());
            return new DagsatsBrukerAG(0L, 0L);
        }

        var utbetalingsgrad = uttakAktivitet.getUtbetalingsgrad().scaleByPowerOfTen(-2);
        var andelArbeidsgiver = Optional.ofNullable(redusertRefusjonPrÅr).orElse(BigDecimal.ZERO);
        var andelBruker = Optional.ofNullable(redusertBrukersAndelPrÅr).orElse(BigDecimal.ZERO);
        var redusertAndelArb = andelArbeidsgiver.multiply(utbetalingsgrad);
        var redusertAndelBruker = andelBruker.multiply(utbetalingsgrad);

        if (skalGjøreOverkompensasjon(uttakAktivitet)) {
            var permisjonsProsent = finnPermisjonsprosent(uttakAktivitet);
            var stillingsRefusjon = BigDecimal.ZERO.max(andelArbeidsgiver.multiply(permisjonsProsent));
            var overkompensertRefusjon = stillingsRefusjon.min(redusertAndelArb);
            var utbetalingBruker = redusertAndelArb.subtract(overkompensertRefusjon).add(redusertAndelBruker);

            var dagsatsArbeidsgiver = årsbeløpTilDagsats(overkompensertRefusjon);
            var dagsatsBruker = årsbeløpTilDagsats(utbetalingBruker);

            // Regelsporing
            resultater.put(periodenavn + ".utbetalingsgrad", uttakAktivitet.getUtbetalingsgrad());
            resultater.put(periodenavn + ".stillingsprosent", uttakAktivitet.getStillingsgrad());

            return new DagsatsBrukerAG(dagsatsBruker, dagsatsArbeidsgiver);
        }
        var dagsatsArbeidsgiver = årsbeløpTilDagsats(redusertAndelArb);
        var dagsatsBruker = årsbeløpTilDagsats(redusertAndelBruker);

        // Regelsporing
        resultater.put(periodenavn + ".utbetalingsgrad", uttakAktivitet.getUtbetalingsgrad());
        resultater.put(periodenavn + ".stillingsprosent", uttakAktivitet.getStillingsgrad());

        return new DagsatsBrukerAG(dagsatsBruker, dagsatsArbeidsgiver);
    }

    private static boolean skalGjøreOverkompensasjon(UttakAktivitet uttakAktivitet) {
        if (uttakAktivitet.getTotalStillingsgradHosAG().compareTo(FULL_STILLING) >= 0) {
            // Jobber mer enn 100%, skal aldri overkompenseres
            return false;
        }
        return uttakAktivitet.isErGradering();
    }

    private static BigDecimal finnPermisjonsprosent(UttakAktivitet uttakAktivitet) {
        if (!uttakAktivitet.isErGradering()) {
            return BigDecimal.ONE;
        }
        var stillingsandel = uttakAktivitet.getStillingsgrad().scaleByPowerOfTen(-2);
        var arbeidstidsAndel = uttakAktivitet.getArbeidstidsprosent().scaleByPowerOfTen(-2);

        if (stillingsandel.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return  BigDecimal.ONE.subtract(arbeidstidsAndel.divide(stillingsandel, 10, RoundingMode.HALF_UP));
    }

    private LocalDateTimeline<BeregningsgrunnlagPeriode> mapGrunnlagTimeline(Beregningsgrunnlag grunnlag) {
        var grunnlagPerioder = grunnlag.getBeregningsgrunnlagPerioder().stream()
            .map(p -> new LocalDateSegment<>(p.getBeregningsgrunnlagPeriode().getFom(), p.getBeregningsgrunnlagPeriode().getTom(), p))
            .collect(Collectors.toList());
        return new LocalDateTimeline<>(grunnlagPerioder);
    }

    private static long årsbeløpTilDagsats(BigDecimal årsbeløp) {
        final var toHundreSeksti = BigDecimal.valueOf(260);
        return årsbeløp.divide(toHundreSeksti, 0, RoundingMode.HALF_UP).longValue();
    }
}
