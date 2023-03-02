package no.nav.foreldrepenger.ytelse.beregning;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.ytelse.beregning.adapter.MapBeregningsresultatFraRegelTilVL;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatGrunnlag;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatRegler;

public final class FastsettBeregningsresultatTjeneste {

    private FastsettBeregningsresultatTjeneste() {
    }

    public static BeregningsresultatEntitet fastsettBeregningsresultat(BeregningsresultatGrunnlag regelmodell) {
        // Kjør regel
        var resultat = BeregningsresultatRegler.fastsettBeregningsresultat(regelmodell);

        // Map tilbake til domenemodell fra regelmodell
        var beregningsresultat = BeregningsresultatEntitet.builder()
            .medRegelInput(resultat.regelInput())
            .medRegelSporing(resultat.regelSporing())
            .build();

        MapBeregningsresultatFraRegelTilVL.mapFra(resultat.beregningsresultat(), beregningsresultat);

        return beregningsresultat;
    }

}
