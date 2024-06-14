package no.nav.foreldrepenger.ytelse.beregning.adapter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collection;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatFeriepenger;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatFeriepengerPrÅr;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatAndel;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatPeriode;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.FastsattFeriepengeresultat;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Arbeidsforhold;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.ReferanseType;

public class MapBeregningsresultatFeriepengerFraRegelTilVL {
    private MapBeregningsresultatFeriepengerFraRegelTilVL() {
        // unused
    }

    public static BeregningsresultatFeriepenger mapFra(BeregningsresultatEntitet resultat, FastsattFeriepengeresultat feriepengeresultat) {

        if (feriepengeresultat.resultat().feriepengerPeriode() == null) {
            // Lagrer sporing
            return BeregningsresultatFeriepenger.builder()
                .medFeriepengerRegelInput(feriepengeresultat.regelInput())
                .medFeriepengerRegelSporing(feriepengeresultat.regelSporing())
                .build(resultat);
        }

        var beregningsresultatFeriepenger = BeregningsresultatFeriepenger.builder()
            .medFeriepengerPeriodeFom(feriepengeresultat.resultat().feriepengerPeriode().getFomDato())
            .medFeriepengerPeriodeTom(feriepengeresultat.resultat().feriepengerPeriode().getTomDato())
            .medFeriepengerRegelInput(feriepengeresultat.regelInput())
            .medFeriepengerRegelSporing(feriepengeresultat.regelSporing())
            .build(resultat);

        var alleBeregneteFeriepenger = feriepengeresultat.resultat().beregningsresultatPerioder().stream()
            .map(BeregningsresultatPeriode::getBeregningsresultatAndelList)
            .flatMap(Collection::stream)
            .map(BeregningsresultatAndel::getBeregningsresultatFeriepengerPrÅrListe)
            .flatMap(Collection::stream)
            .filter(MapBeregningsresultatFeriepengerFraRegelTilVL::erAvrundetÅrsbeløpUlik0)
            .collect(Collectors.groupingBy(FeriepengerGruppering::fraBeregningRegel,
                Collectors.reducing(BigDecimal.ZERO, prÅr -> prÅr.getÅrsbeløp(), BigDecimal::add)));

        alleBeregneteFeriepenger.forEach((k, v) -> mapFeriepengerPrÅr(beregningsresultatFeriepenger, k, v));
        return beregningsresultatFeriepenger;
    }

    private static void mapFeriepengerPrÅr(BeregningsresultatFeriepenger beregningsresultatFeriepenger,
                                           FeriepengerGruppering feriepenger, BigDecimal beløp) {
        var årsbeløp = beløp.setScale(0, RoundingMode.HALF_UP).longValue();
        BeregningsresultatFeriepengerPrÅr.builder()
            .medAktivitetStatus(feriepenger.aktivitetStatus())
            .medBrukerErMottaker(feriepenger.brukerErMottaker())
            .medArbeidsgiver(finnArbeidsgiver(feriepenger))
            .medArbeidsforholdRef(feriepenger.arbeidsforhold() == null
                ? null : InternArbeidsforholdRef.ref(feriepenger.arbeidsforhold().arbeidsforholdId()))
            .medOpptjeningsår(feriepenger.opptjeningÅr())
            .medÅrsbeløp(årsbeløp)
            .build(beregningsresultatFeriepenger);
    }

    private static boolean erAvrundetÅrsbeløpUlik0(no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatFeriepengerPrÅr prÅr) {
        var årsbeløp = prÅr.getÅrsbeløp().setScale(0, RoundingMode.HALF_UP).longValue();
        return årsbeløp != 0L;
    }

    private static Arbeidsgiver finnArbeidsgiver(FeriepengerGruppering feriepengerGruppering) {
        if (feriepengerGruppering.arbeidsforhold() == null) {
            return null;
        }
        var identifikator = feriepengerGruppering.arbeidsforhold().identifikator();
        var referanseType = feriepengerGruppering.arbeidsforhold().referanseType();
        if (referanseType == ReferanseType.AKTØR_ID) {
            return Arbeidsgiver.person(new AktørId(identifikator));
        }
        if (referanseType == ReferanseType.ORG_NR) {
            return Arbeidsgiver.virksomhet(identifikator);
        }
        return null;
    }
    private record FeriepengerGruppering(LocalDate opptjeningÅr, AktivitetStatus aktivitetStatus, Boolean brukerErMottaker, Arbeidsforhold arbeidsforhold) {

        static FeriepengerGruppering fraBeregningRegel(no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatFeriepengerPrÅr andel) {
            return new FeriepengerGruppering(andel.getOpptjeningÅr(), AktivitetStatusMapper.fraRegelTilVl(andel),
                andel.erBrukerMottaker(), andel.getArbeidsforhold());
        }
    }

}
