package no.nav.foreldrepenger.ytelse.beregning.regelmodell.fastsett;

import no.nav.foreldrepenger.ytelse.beregning.regelmodell.Beregningsresultat;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatGrunnlag;

public class BeregningsresultatRegelmodellMellomregning {
    private final BeregningsresultatGrunnlag input;
    private final Beregningsresultat output;

    public BeregningsresultatRegelmodellMellomregning(BeregningsresultatGrunnlag input, Beregningsresultat output) {
        this.input = input;
        this.output = output;
    }

    public BeregningsresultatGrunnlag getInput() {
        return input;
    }

    public Beregningsresultat getOutput() {
        return output;
    }
}
