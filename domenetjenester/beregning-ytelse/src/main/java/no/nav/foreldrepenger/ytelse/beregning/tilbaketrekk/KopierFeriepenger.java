package no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.Kopimaskin;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatFeriepenger;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatFeriepengerPrÅr;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;

public class KopierFeriepenger {
    private KopierFeriepenger() {
        // skjul public constructor
    }

    public static void kopier(Long behandlingId, BeregningsresultatEntitet revurderingTY, BeregningsresultatEntitet utbetBR) {
        Optional<BeregningsresultatFeriepenger> bgFeriepengerOpt = revurderingTY.getBeregningsresultatFeriepenger();
        if (!bgFeriepengerOpt.isPresent()) {
            return;
        }

        BeregningsresultatFeriepenger bgFeriepenger = bgFeriepengerOpt.get();
        List<BeregningsresultatFeriepengerPrÅr> bgFeriepengerPrÅrListe = bgFeriepengerOpt
            .map(BeregningsresultatFeriepenger::getBeregningsresultatFeriepengerPrÅrListe)
            .orElse(Collections.emptyList());

        BeregningsresultatFeriepenger beregningsresultatFeriepenger = kopierBeregningsresultatFeriepenger(utbetBR, bgFeriepenger);

        bgFeriepengerPrÅrListe.forEach(prÅr -> {
            BeregningsresultatAndel bgAndel = prÅr.getBeregningsresultatAndel();
            LocalDate fom = bgAndel.getBeregningsresultatPeriode().getBeregningsresultatPeriodeFom();
            BeregningsresultatPeriode beregningsresultatPeriode = utbetBR.getBeregningsresultatPerioder().stream()
                .filter(brp -> brp.getBeregningsresultatPeriodeFom().equals(fom))
                .findFirst()
                .orElseThrow(() ->
                    new IllegalStateException("Utviklerfeil: Fant ikke korresponderende utbet-periode for behandling " + behandlingId));

            List<BeregningsresultatAndel> haystack = beregningsresultatPeriode.getBeregningsresultatAndelList();

            BeregningsresultatAndel utbetAndel = FinnKorresponderendeBeregningsresultatAndel.finn(haystack, bgAndel, bgAndel.erBrukerMottaker())
                .orElseGet(() -> BeregningsresultatAndel.builder(Kopimaskin.deepCopy(bgAndel))
                    .medDagsats(0)
                    .medDagsatsFraBg(0)
                    .build(beregningsresultatPeriode)
                );

            BeregningsresultatFeriepengerPrÅr.builder()
                .medOpptjeningsår(prÅr.getOpptjeningsår())
                .medÅrsbeløp(prÅr.getÅrsbeløp().getVerdi().longValue())
                .build(beregningsresultatFeriepenger, utbetAndel);
        });
    }

    private static BeregningsresultatFeriepenger kopierBeregningsresultatFeriepenger(BeregningsresultatEntitet utbetBR, BeregningsresultatFeriepenger bgFeriepenger) {
        return BeregningsresultatFeriepenger.builder()
            .medFeriepengerPeriodeFom(bgFeriepenger.getFeriepengerPeriodeFom())
            .medFeriepengerPeriodeTom(bgFeriepenger.getFeriepengerPeriodeTom())
            .medFeriepengerRegelSporing(bgFeriepenger.getFeriepengerRegelSporing())
            .medFeriepengerRegelInput(bgFeriepenger.getFeriepengerRegelInput())
            .build(utbetBR);
    }

}
