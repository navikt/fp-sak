package no.nav.foreldrepenger.ytelse.beregning.adapter;

import java.math.RoundingMode;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatFeriepenger;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatFeriepengerPrÅr;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.FastsattFeriepengeresultat;
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

        feriepengeresultat.resultat().beregningsresultatFeriepengerPrÅrListe()
            .stream()
            .filter(MapBeregningsresultatFeriepengerFraRegelTilVL::erAvrundetÅrsbeløpUlik0)
            .forEach(ferie -> mapFeriepengerPrÅr(beregningsresultatFeriepenger, ferie));
        return beregningsresultatFeriepenger;
    }

    private static void mapFeriepengerPrÅr(BeregningsresultatFeriepenger beregningsresultatFeriepenger,
                                           no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatFeriepengerPrÅr feriepenger) {
        var årsbeløp = feriepenger.getÅrsbeløp().setScale(0, RoundingMode.HALF_UP).longValue();
        BeregningsresultatFeriepengerPrÅr.builder()
            .medAktivitetStatus(AktivitetStatusMapper.fraRegelTilVl(feriepenger))
            .medBrukerErMottaker(feriepenger.erBrukerMottaker())
            .medArbeidsgiver(finnArbeidsgiver(feriepenger))
            .medArbeidsforholdRef(feriepenger.getArbeidsforhold() == null
                ? null : InternArbeidsforholdRef.ref(feriepenger.getArbeidsforhold().arbeidsforholdId()))
            .medOpptjeningsår(feriepenger.getOpptjeningÅr())
            .medÅrsbeløp(årsbeløp)
            .build(beregningsresultatFeriepenger);
    }

    private static boolean erAvrundetÅrsbeløpUlik0(no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatFeriepengerPrÅr prÅr) {
        var årsbeløp = prÅr.getÅrsbeløp().setScale(0, RoundingMode.HALF_UP).longValue();
        return årsbeløp != 0L;
    }

    private static Arbeidsgiver finnArbeidsgiver(no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatFeriepengerPrÅr feriepenger) {
        if (feriepenger.getArbeidsforhold() == null) {
            return null;
        }
        var identifikator = feriepenger.getArbeidsforhold().identifikator();
        var referanseType = feriepenger.getArbeidsforhold().referanseType();
        if (referanseType == ReferanseType.AKTØR_ID) {
            return Arbeidsgiver.person(new AktørId(identifikator));
        }
        if (referanseType == ReferanseType.ORG_NR) {
            return Arbeidsgiver.virksomhet(identifikator);
        }
        return null;
    }

}
