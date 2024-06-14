package no.nav.foreldrepenger.behandlingslager.behandling.beregning;

import java.util.Objects;
import java.util.Optional;

public class BehandlingBeregningsresultatBuilder {

    private BehandlingBeregningsresultatEntitet kladd;

    private BehandlingBeregningsresultatBuilder(BehandlingBeregningsresultatEntitet kladd) {
        this.kladd = kladd;
    }

    static BehandlingBeregningsresultatBuilder nytt() {
        return new BehandlingBeregningsresultatBuilder(new BehandlingBeregningsresultatEntitet());
    }

    static BehandlingBeregningsresultatBuilder oppdatere(BehandlingBeregningsresultatEntitet kladd) {
        return new BehandlingBeregningsresultatBuilder(new BehandlingBeregningsresultatEntitet(kladd));
    }

    public static BehandlingBeregningsresultatBuilder oppdatere(Optional<BehandlingBeregningsresultatEntitet> kladd) {
        return kladd.map(BehandlingBeregningsresultatBuilder::oppdatere).orElseGet(BehandlingBeregningsresultatBuilder::nytt);
    }

    public BehandlingBeregningsresultatBuilder medBgBeregningsresultatFP(BeregningsresultatEntitet beregningsresultat) {
        kladd.setBgBeregningsresultatFP(beregningsresultat);
        return this;
    }

    public BehandlingBeregningsresultatBuilder medUtbetBeregningsresultatFP(BeregningsresultatEntitet beregningsresultat) {
        kladd.setUtbetBeregningsresultatFP(beregningsresultat);
        return this;
    }

    public BehandlingBeregningsresultatBuilder medBeregningsresultatFeriepenger(BeregningsresultatFeriepenger beregningsresultatFeriepenger) {
        kladd.setBeregningsresultatFeriepenger(beregningsresultatFeriepenger);
        return this;
    }

    public BehandlingBeregningsresultatBuilder medSkalHindreTilbaketrekk(boolean skalHindreTilbaketrekk) {
        kladd.setSkalHindreTilbaketrekk(skalHindreTilbaketrekk);
        return this;
    }

    public BehandlingBeregningsresultatEntitet build(Long behandlingId) {
        Objects.requireNonNull(behandlingId);
        kladd.setBehandling(behandlingId);
        return kladd;
    }

}
