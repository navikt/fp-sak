package no.nav.foreldrepenger.ytelse.beregning.regelmodell.fastsett;

import no.nav.foreldrepenger.ytelse.beregning.BeregningsgrunnlagUttakArbeidsforholdMatcher;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatAndel;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatPeriode;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.*;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.uttakresultat.UttakAktivitet;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.uttakresultat.UttakResultat;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.uttakresultat.UttakResultatPeriode;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
        var grunnlag = regelmodell.beregningsgrunnlag();
        var uttakResultat = regelmodell.uttakResultat();
        var periodeListe = mapPerioder(grunnlag, uttakResultat, resultater);
        periodeListe.forEach(p -> mellomregning.getOutput().addBeregningsresultatPeriode(p));
        return beregnet(resultater);
    }

    private List<BeregningsresultatPeriode> mapPerioder(Beregningsgrunnlag grunnlag, UttakResultat uttakResultat, Map<String, Object> resultater) {
        var grunnlagTimeline = mapGrunnlagTimeline(grunnlag);
        var uttakTimeline = uttakResultat.getUttakPeriodeTimeline();
        var resultatTimeline = intersectTimelines(grunnlagTimeline, uttakTimeline, resultater)
            .compress();
        return resultatTimeline.toSegments().stream().map(LocalDateSegment::getValue).toList();
    }

    private LocalDateTimeline<BeregningsresultatPeriode> intersectTimelines(LocalDateTimeline<BeregningsgrunnlagPeriode> grunnlagTimeline, LocalDateTimeline<UttakResultatPeriode> uttakTimeline, Map<String, Object> resultater) {
        var i = new int[]{0}; //Periode-teller til regelsporing
        return grunnlagTimeline.intersection(uttakTimeline, (dateInterval, grunnlagSegment, uttakSegment) ->
        {
            var resultatPeriode = new BeregningsresultatPeriode(dateInterval);

            //Regelsporing
            var periodeNavn = "BeregningsresultatPeriode[" + i[0] + "]";
            resultater.put(periodeNavn + ".fom", dateInterval.getFomDato());
            resultater.put(periodeNavn + ".tom", dateInterval.getTomDato());

            var grunnlag = grunnlagSegment.getValue();
            var uttakResultatPeriode = uttakSegment.getValue();

            grunnlag.getBeregningsgrunnlagPrStatus(AktivitetStatus.ATFL).forEach(gbps -> {
                // for hver arbeidstaker andel: map fra grunnlag til 1-2 resultatAndel
                var arbeidsforholdList = gbps.arbeidsforhold();
                arbeidsforholdList.forEach(a -> opprettBeregningsresultatAndelerATFL(a, resultatPeriode, resultater, periodeNavn, uttakResultatPeriode));
            });
            grunnlag.beregningsgrunnlagPrStatus().stream()
                .filter(bgps -> !AktivitetStatus.ATFL.equals(bgps.aktivitetStatus()))
                .forEach(bgps -> opprettBeregningsresultatAndelerGenerell(bgps, resultatPeriode, resultater, periodeNavn, uttakResultatPeriode));

            i[0]++;
            return new LocalDateSegment<>(dateInterval, resultatPeriode);
        });
    }

    private void opprettBeregningsresultatAndelerGenerell(BeregningsgrunnlagPrStatus beregningsgrunnlagPrStatus, BeregningsresultatPeriode resultatPeriode,
                                                          Map<String, Object> resultater, String periodeNavn, UttakResultatPeriode uttakResultatPeriode) {
        if (uttakResultatPeriode.erOppholdsPeriode()) {
            return;
        }
        var uttakAktivitetOpt = matchUttakAktivitetMedBeregningsgrunnlagPrStatus(beregningsgrunnlagPrStatus, uttakResultatPeriode.uttakAktiviteter());
        if (uttakAktivitetOpt.isEmpty()) {
            return;
        }
        var uttakAktivitet = uttakAktivitetOpt.get();

        //Gradering
        var dagsatser = kalkulerDagsatserForGradering(beregningsgrunnlagPrStatus.redusertBrukersAndelPrÅr(), BigDecimal.ZERO,
            uttakAktivitet, resultater, periodeNavn);
        var dagsatsBruker = dagsatser.bruker();

        resultatPeriode.addBeregningsresultatAndel(
            BeregningsresultatAndel.builder()
                .medBrukerErMottaker(true)
                .medDagsats(dagsatsBruker)
                .medDagsatsFraBg(årsbeløpTilDagsats(beregningsgrunnlagPrStatus.redusertBrukersAndelPrÅr()))
                .medAktivitetStatus(beregningsgrunnlagPrStatus.aktivitetStatus())
                .medInntektskategori(beregningsgrunnlagPrStatus.inntektskategori())
                .medUtbetalingssgrad(uttakAktivitet.utbetalingsgrad())
                .medStillingsprosent(uttakAktivitet.stillingsgrad())
                .build());

        // Regelsporing
        var beskrivelse = periodeNavn + BRUKER_ANDEL + "['" + beregningsgrunnlagPrStatus.aktivitetStatus().name() + "']" + DAGSATS_BRUKER;
        resultater.put(beskrivelse, dagsatsBruker);

    }

    private Optional<UttakAktivitet> matchUttakAktivitetMedBeregningsgrunnlagPrStatus(BeregningsgrunnlagPrStatus beregningsgrunnlagPrStatus, List<UttakAktivitet> uttakAktiviteter) {
        return uttakAktiviteter.stream()
            .filter(uttakAndel -> BeregningsgrunnlagUttakArbeidsforholdMatcher.matcherGenerellAndel(beregningsgrunnlagPrStatus, uttakAndel))
            .findFirst();
    }

    private void opprettBeregningsresultatAndelerATFL(BeregningsgrunnlagPrArbeidsforhold arbeidsforhold, BeregningsresultatPeriode resultatPeriode,
                                                      Map<String, Object> resultater, String periodeNavn, UttakResultatPeriode uttakResultatPeriode) {
        if (uttakResultatPeriode.erOppholdsPeriode()) {
            return;
        }
        var uttakAktivitetOpt = matchUttakAktivitetMedArbeidsforhold(uttakResultatPeriode.uttakAktiviteter(), arbeidsforhold);
        if (uttakAktivitetOpt.isEmpty()) {
            return;
        }
        var uttakAktivitet = uttakAktivitetOpt.get();
        var arbeidsgiverId = arbeidsforhold.getArbeidsgiverId();

        //Gradering
        var dagsatser = kalkulerDagsatserForGradering(arbeidsforhold.redusertBrukersAndelPrÅr(), arbeidsforhold.redusertRefusjonPrÅr(),
            uttakAktivitet, resultater, periodeNavn);

        var dagsatsBruker = dagsatser.bruker();
        var dagsatsArbeidsgiver = dagsatser.arbeidsgiver();

        resultatPeriode.addBeregningsresultatAndel(
            BeregningsresultatAndel.builder()
                .medArbeidsforhold(arbeidsforhold.arbeidsforhold())
                .medBrukerErMottaker(true)
                .medStillingsprosent(uttakAktivitet.stillingsgrad())
                .medUtbetalingssgrad(uttakAktivitet.utbetalingsgrad())
                .medDagsats(dagsatsBruker)
                .medDagsatsFraBg(arbeidsforhold.getDagsatsBruker())
                .medAktivitetStatus(AktivitetStatus.ATFL)
                .medInntektskategori(arbeidsforhold.inntektskategori())
                .build());

        // Regelsporing
        resultater.put(periodeNavn + BRUKER_ANDEL + "['" + arbeidsgiverId + "']" + ARBEIDSGIVER_ID, arbeidsgiverId);
        resultater.put(periodeNavn + BRUKER_ANDEL + "['" + arbeidsgiverId + "']" + DAGSATS_BRUKER, dagsatsBruker);
        resultater.put(periodeNavn + BRUKER_ANDEL + "['" + arbeidsgiverId + "']" + ".dagsatsFraBeregningsgrunnlagBruker", arbeidsforhold.getDagsatsBruker());

        if (arbeidsforhold.getDagsatsArbeidsgiver() != null && arbeidsforhold.getDagsatsArbeidsgiver() > 0) {
            resultatPeriode.addBeregningsresultatAndel(
                BeregningsresultatAndel.builder()
                    .medArbeidsforhold(arbeidsforhold.arbeidsforhold())
                    .medBrukerErMottaker(false)
                    .medStillingsprosent(uttakAktivitet.stillingsgrad())
                    .medUtbetalingssgrad(uttakAktivitet.utbetalingsgrad())
                    .medDagsats(dagsatsArbeidsgiver)
                    .medDagsatsFraBg(arbeidsforhold.getDagsatsArbeidsgiver())
                    .medInntektskategori(arbeidsforhold.inntektskategori())
                    .medAktivitetStatus(AktivitetStatus.ATFL)
                    .build());

            //Regelsporing
            resultater.put(periodeNavn + ARBEIDSGIVERS_ANDEL + "['" + arbeidsgiverId + "']" + ARBEIDSGIVER_ID, arbeidsgiverId);
            resultater.put(periodeNavn + ARBEIDSGIVERS_ANDEL + "['" + arbeidsgiverId + "']" + DAGSATS_ARBEIDSGIVER, dagsatsArbeidsgiver);
            resultater.put(periodeNavn + ARBEIDSGIVERS_ANDEL + "['" + arbeidsgiverId + "']" + ".dagsatsFraBeregningsgrunnlagArbeidsgiver", arbeidsforhold.getDagsatsArbeidsgiver());
        }
    }

    private Optional<UttakAktivitet> matchUttakAktivitetMedArbeidsforhold(List<UttakAktivitet> uttakAktiviteter, BeregningsgrunnlagPrArbeidsforhold bgAndel) {
        return uttakAktiviteter
            .stream()
            .filter(uttakAktivitet -> BeregningsgrunnlagUttakArbeidsforholdMatcher.matcherArbeidsforhold(uttakAktivitet.arbeidsforhold(), bgAndel.arbeidsforhold()))
            .findFirst();
    }

    /*
     * dagsatser gradert for bruker og arbeidsgiver
     */
    private record DagsatsBrukerAG(Long bruker, Long arbeidsgiver) {}

    private static DagsatsBrukerAG kalkulerDagsatserForGradering(BigDecimal redusertBrukersAndelPrÅr, BigDecimal redusertRefusjonPrÅr,
                                                                   UttakAktivitet uttakAktivitet, Map<String, Object> resultater, String periodenavn) {
        if (uttakAktivitet.utbetalingsgrad().compareTo(BigDecimal.ZERO) == 0) {
            resultater.put(periodenavn + ".utbetalingsgrad", uttakAktivitet.utbetalingsgrad());
            return new DagsatsBrukerAG(0L, 0L);
        }

        var utbetalingsgrad = uttakAktivitet.utbetalingsgrad().scaleByPowerOfTen(-2);
        var andelArbeidsgiver = Optional.ofNullable(redusertRefusjonPrÅr).orElse(BigDecimal.ZERO);
        var andelBruker = Optional.ofNullable(redusertBrukersAndelPrÅr).orElse(BigDecimal.ZERO);
        // FP skal videre reduseres med gradering, mens SVP er ferdig redusert
        var redusertAndelArb = uttakAktivitet.reduserDagsatsMedUtbetalingsgrad() ? andelArbeidsgiver.multiply(utbetalingsgrad) : andelArbeidsgiver;
        var redusertAndelBruker = uttakAktivitet.reduserDagsatsMedUtbetalingsgrad() ? andelBruker.multiply(utbetalingsgrad) : andelBruker;

        if (skalGjøreOverkompensasjon(uttakAktivitet)) {
            var permisjonsProsent = finnPermisjonsprosent(uttakAktivitet);
            var stillingsRefusjon = BigDecimal.ZERO.max(andelArbeidsgiver.multiply(permisjonsProsent));
            var overkompensertRefusjon = stillingsRefusjon.min(redusertAndelArb);
            var utbetalingBruker = redusertAndelArb.subtract(overkompensertRefusjon).add(redusertAndelBruker);

            var dagsatsArbeidsgiver = årsbeløpTilDagsats(overkompensertRefusjon);
            var dagsatsBruker = årsbeløpTilDagsats(utbetalingBruker);

            // Regelsporing
            resultater.put(periodenavn + ".utbetalingsgrad", uttakAktivitet.utbetalingsgrad());
            resultater.put(periodenavn + ".stillingsprosent", uttakAktivitet.stillingsgrad());

            return new DagsatsBrukerAG(dagsatsBruker, dagsatsArbeidsgiver);
        }
        var dagsatsArbeidsgiver = årsbeløpTilDagsats(redusertAndelArb);
        var dagsatsBruker = årsbeløpTilDagsats(redusertAndelBruker);

        // Regelsporing
        resultater.put(periodenavn + ".utbetalingsgrad", uttakAktivitet.utbetalingsgrad());
        resultater.put(periodenavn + ".stillingsprosent", uttakAktivitet.stillingsgrad());

        return new DagsatsBrukerAG(dagsatsBruker, dagsatsArbeidsgiver);
    }

    private static boolean skalGjøreOverkompensasjon(UttakAktivitet uttakAktivitet) {
        if (uttakAktivitet.totalStillingsgradHosAG().compareTo(FULL_STILLING) >= 0) {
            // Jobber mer enn 100%, skal aldri overkompenseres
            return false;
        }
        return uttakAktivitet.erGradering();
    }

    private static BigDecimal finnPermisjonsprosent(UttakAktivitet uttakAktivitet) {
        if (!uttakAktivitet.erGradering()) {
            return BigDecimal.ONE;
        }
        var stillingsandel = uttakAktivitet.stillingsgrad().scaleByPowerOfTen(-2);
        var arbeidstidsAndel = uttakAktivitet.arbeidstidsprosent().scaleByPowerOfTen(-2);

        if (stillingsandel.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return  BigDecimal.ONE.subtract(arbeidstidsAndel.divide(stillingsandel, 10, RoundingMode.HALF_UP));
    }

    private LocalDateTimeline<BeregningsgrunnlagPeriode> mapGrunnlagTimeline(Beregningsgrunnlag grunnlag) {
        var grunnlagPerioder = grunnlag.beregningsgrunnlagPerioder().stream()
            .map(p -> new LocalDateSegment<>(p.periode(), p))
            .toList();
        return new LocalDateTimeline<>(grunnlagPerioder);
    }

    private static long årsbeløpTilDagsats(BigDecimal årsbeløp) {
        var toHundreSeksti = BigDecimal.valueOf(260);
        return årsbeløp.divide(toHundreSeksti, 0, RoundingMode.HALF_UP).longValue();
    }
}
