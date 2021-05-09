package no.nav.foreldrepenger.ytelse.beregning;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.RegelmodellOversetter;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.domene.json.StandardJsonConfig;
import no.nav.foreldrepenger.ytelse.beregning.adapter.MapBeregningsresultatFraRegelTilVL;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.Beregningsresultat;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatRegelmodell;
import no.nav.foreldrepenger.ytelse.beregning.regler.RegelFastsettBeregningsresultat;

@ApplicationScoped
public class FastsettBeregningsresultatTjeneste {

    private MapBeregningsresultatFraRegelTilVL mapBeregningsresultatFraRegelTilVL;

    FastsettBeregningsresultatTjeneste() {
    }

    @Inject
    public FastsettBeregningsresultatTjeneste(MapBeregningsresultatFraRegelTilVL mapBeregningsresultatFraRegelTilVL) {
        this.mapBeregningsresultatFraRegelTilVL = mapBeregningsresultatFraRegelTilVL;
    }

    public BeregningsresultatEntitet fastsettBeregningsresultat(BeregningsresultatRegelmodell regelmodell) {
        // Kalle regel
        var regel = new RegelFastsettBeregningsresultat();
        var outputContainer = Beregningsresultat.builder().build();
        var evaluation = regel.evaluer(regelmodell, outputContainer);
        var sporing = RegelmodellOversetter.getSporing(evaluation);

        // Map tilbake til domenemodell fra regelmodell
        var beregningsresultat = BeregningsresultatEntitet.builder()
            .medRegelInput(toJson(regelmodell))
            .medRegelSporing(sporing)
            .build();

        mapBeregningsresultatFraRegelTilVL.mapFra(outputContainer, beregningsresultat);

        return beregningsresultat;
    }

    private String toJson(BeregningsresultatRegelmodell grunnlag) {
        return StandardJsonConfig.toJson(grunnlag);
    }
}
