package no.nav.foreldrepenger.ytelse.beregning;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.RegelmodellOversetter;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.ytelse.beregning.adapter.MapBeregningsresultatFraRegelTilVL;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.Beregningsresultat;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatRegelmodell;
import no.nav.foreldrepenger.ytelse.beregning.regler.RegelFastsettBeregningsresultat;
import no.nav.vedtak.exception.TekniskException;

@ApplicationScoped
public class FastsettBeregningsresultatTjeneste {

    private final JacksonJsonConfig jacksonJsonConfig = new JacksonJsonConfig();
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
        return jacksonJsonConfig.toJson(grunnlag, e -> new TekniskException("FP-563791", "JSON mapping feilet", e));
    }
}
