package no.nav.foreldrepenger.ytelse.beregning.regelmodell;

public class BeregningsresultatRegelmodellMellomregning {
    private final BeregningsresultatRegelmodell input;
    private final Beregningsresultat output;

    public BeregningsresultatRegelmodellMellomregning(BeregningsresultatRegelmodell input, Beregningsresultat output) {
        this.input = input;
        this.output = output;
    }

    public BeregningsresultatRegelmodell getInput() {
        return input;
    }

    public Beregningsresultat getOutput() {
        return output;
    }
}
