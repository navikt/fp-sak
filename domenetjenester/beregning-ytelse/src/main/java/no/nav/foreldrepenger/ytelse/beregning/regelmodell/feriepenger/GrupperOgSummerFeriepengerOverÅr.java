package no.nav.foreldrepenger.ytelse.beregning.regelmodell.feriepenger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatFeriepengerPrÅr;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.AktivitetStatus;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Arbeidsforhold;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;

class GrupperOgSummerFeriepengerOverÅr extends LeafSpecification<BeregningsresultatFeriepengerRegelModell> {
    public static final String ID = "FP_BR 8.9";
    public static final String BESKRIVELSE = "Grupper og summer feriepengerandeler med samme opptjeningsår.";

    GrupperOgSummerFeriepengerOverÅr() {
        super(ID, BESKRIVELSE);
    }

    @Override
    public Evaluation evaluate(BeregningsresultatFeriepengerRegelModell regelModell) {

        var antallFeriepengeAndelerFør = regelModell.getBeregningsresultatFeriepengerPrÅrListe().size();

        var alleBeregneteFeriepenger = regelModell.getBeregningsresultatFeriepengerPrÅrListe().stream()
            .filter(GrupperOgSummerFeriepengerOverÅr::erAvrundetÅrsbeløpUlik0)
            .collect(Collectors.groupingBy(FeriepengerGruppering::fraBeregningRegel,
                Collectors.reducing(BigDecimal.ZERO, BeregningsresultatFeriepengerPrÅr::getÅrsbeløp, BigDecimal::add)));

        regelModell.tømBeregningsresultatFeriepengerPrÅrListe();

        alleBeregneteFeriepenger.forEach((k, v) -> regelModell.addBeregningsresultatFeriepengerPrÅr(mapFeriepengerPrÅr(k, v)));

        var antallFeriepengeAndelerEtter = regelModell.getBeregningsresultatFeriepengerPrÅrListe().size();


        Map<String, Object> resultater = new LinkedHashMap<>();
        resultater.put("Reduksjon i antall feriepengandeler ved summering", antallFeriepengeAndelerFør - antallFeriepengeAndelerEtter);
        return beregnet(resultater);
    }

    private static BeregningsresultatFeriepengerPrÅr mapFeriepengerPrÅr(FeriepengerGruppering feriepenger, BigDecimal beløp) {
        var årsbeløp = beløp.setScale(0, RoundingMode.HALF_UP);
        return BeregningsresultatFeriepengerPrÅr.builder()
            .medAktivitetStatus(feriepenger.aktivitetStatus())
            .medBrukerErMottaker(feriepenger.brukerErMottaker())
            .medArbeidsforhold(feriepenger.arbeidsforhold())
            .medOpptjeningÅr(feriepenger.opptjeningÅr())
            .medÅrsbeløp(årsbeløp)
            .build();
    }

    private static boolean erAvrundetÅrsbeløpUlik0(no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatFeriepengerPrÅr prÅr) {
        var årsbeløp = prÅr.getÅrsbeløp().setScale(0, RoundingMode.HALF_UP).longValue();
        return årsbeløp != 0L;
    }

    private record FeriepengerGruppering(LocalDate opptjeningÅr, AktivitetStatus aktivitetStatus, Boolean brukerErMottaker, Arbeidsforhold arbeidsforhold) {

        static FeriepengerGruppering fraBeregningRegel(BeregningsresultatFeriepengerPrÅr andel) {
            return new FeriepengerGruppering(andel.getOpptjeningÅr(), andel.getAktivitetStatus(),
                andel.erBrukerMottaker(), andel.getArbeidsforhold());
        }
    }
}
