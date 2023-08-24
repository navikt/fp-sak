package no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;

import java.util.List;

public class BRAndelSammenligning {
    private List<BeregningsresultatAndel> forrigeAndeler;
    private List<BeregningsresultatAndel> bgAndeler;

    public BRAndelSammenligning(List<BeregningsresultatAndel> forrigeAndeler, List<BeregningsresultatAndel> bgAndeler) {
        this.forrigeAndeler = forrigeAndeler;
        this.bgAndeler = bgAndeler;
    }

    public List<BeregningsresultatAndel> getForrigeAndeler() {
        return forrigeAndeler;
    }

    public List<BeregningsresultatAndel> getBgAndeler() {
        return bgAndeler;
    }
}
