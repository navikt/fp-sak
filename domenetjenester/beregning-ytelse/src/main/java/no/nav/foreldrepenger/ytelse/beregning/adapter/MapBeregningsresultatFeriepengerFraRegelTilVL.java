package no.nav.foreldrepenger.ytelse.beregning.adapter;

import java.math.RoundingMode;
import java.util.Objects;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatFeriepenger;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatFeriepengerPrÅr;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatAndel;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.feriepenger.BeregningsresultatFeriepengerRegelModell;

public class MapBeregningsresultatFeriepengerFraRegelTilVL {
    private MapBeregningsresultatFeriepengerFraRegelTilVL() {
        // unused
    }

    public static void mapFra(BeregningsresultatEntitet resultat, BeregningsresultatFeriepengerRegelModell regelModell, String regelInput, String sporing) {

        if (regelModell.getFeriepengerPeriode() == null) {
            // Lagrer sporing
            BeregningsresultatFeriepenger.builder()
                .medFeriepengerRegelInput(regelInput)
                .medFeriepengerRegelSporing(sporing)
                .build(resultat);
            return;
        }

        var beregningsresultatFeriepenger = BeregningsresultatFeriepenger.builder()
            .medFeriepengerPeriodeFom(regelModell.getFeriepengerPeriode().getFomDato())
            .medFeriepengerPeriodeTom(regelModell.getFeriepengerPeriode().getTomDato())
            .medFeriepengerRegelInput(regelInput)
            .medFeriepengerRegelSporing(sporing)
            .build(resultat);

        regelModell.getBeregningsresultatPerioder().forEach(regelBeregningsresultatPeriode ->
            mapPeriode(resultat, beregningsresultatFeriepenger, regelBeregningsresultatPeriode));
    }

    private static void mapPeriode(BeregningsresultatEntitet resultat, BeregningsresultatFeriepenger beregningsresultatFeriepenger, no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatPeriode regelBeregningsresultatPeriode) {
        var vlBeregningsresultatPeriode = resultat.getBeregningsresultatPerioder().stream()
            .filter(periode -> periode.getBeregningsresultatPeriodeFom().equals(regelBeregningsresultatPeriode.getFom()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Utviklerfeil: Fant ikke BeregningsresultatPeriode"));
        regelBeregningsresultatPeriode.getBeregningsresultatAndelList().forEach(regelAndel ->
            mapAndel(beregningsresultatFeriepenger, vlBeregningsresultatPeriode, regelAndel));
    }

    private static void mapAndel(BeregningsresultatFeriepenger beregningsresultatFeriepenger, BeregningsresultatPeriode vlBeregningsresultatPeriode,
                                 BeregningsresultatAndel regelAndel) {
        if (regelAndel.getBeregningsresultatFeriepengerPrÅrListe().isEmpty()) {
            return;
        }
        var regelAndelAktivitetStatus = AktivitetStatusMapper.fraRegelTilVl(regelAndel);
        var regelArbeidsgiverId = regelAndel.getArbeidsforhold() == null ? null : regelAndel.getArbeidsgiverId();
        var regelArbeidsforholdId = regelAndel.getArbeidsforhold() != null ? regelAndel.getArbeidsforhold().getArbeidsforholdId() : null;
        var andel = vlBeregningsresultatPeriode.getBeregningsresultatAndelList().stream()
            .filter(vlAndel -> {
                var vlArbeidsforholdRef = vlAndel.getArbeidsforholdRef() == null ? null : vlAndel.getArbeidsforholdRef().getReferanse();
                return Objects.equals(vlAndel.getAktivitetStatus(), regelAndelAktivitetStatus)
                    && Objects.equals(vlAndel.getArbeidsgiver().map(Arbeidsgiver::getIdentifikator).orElse(null), regelArbeidsgiverId)
                    && Objects.equals(vlArbeidsforholdRef, regelArbeidsforholdId)
                    && Objects.equals(vlAndel.erBrukerMottaker(), regelAndel.erBrukerMottaker());
            })
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Utviklerfeil: Fant ikke " + regelAndel));
        regelAndel.getBeregningsresultatFeriepengerPrÅrListe()
            .stream()
            .filter(MapBeregningsresultatFeriepengerFraRegelTilVL::erAvrundetÅrsbeløpUlik0)
            .forEach(prÅr ->
        {
            var årsbeløp = prÅr.getÅrsbeløp().setScale(0, RoundingMode.HALF_UP).longValue();
            BeregningsresultatFeriepengerPrÅr.builder()
                .medOpptjeningsår(prÅr.getOpptjeningÅr())
                .medÅrsbeløp(årsbeløp)
                .build(beregningsresultatFeriepenger, andel);
        });
    }

    private static boolean erAvrundetÅrsbeløpUlik0(no.nav.foreldrepenger.ytelse.beregning.regelmodell.feriepenger.BeregningsresultatFeriepengerPrÅr prÅr) {
        var årsbeløp = prÅr.getÅrsbeløp().setScale(0, RoundingMode.HALF_UP).longValue();
        return årsbeløp != 0L;
    }
}
