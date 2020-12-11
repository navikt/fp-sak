package no.nav.foreldrepenger.ytelse.beregning.endringsdato.regelmodell;

import no.nav.foreldrepenger.domene.typer.Beløp;

public class BeregningsresultatFeriepengerPrÅrEndringModell {
    private final Beløp årsbeløp;
    private final int opptjeningsår;

    public BeregningsresultatFeriepengerPrÅrEndringModell(Beløp årsbeløp,
                                                          int opptjeningsår) {
        this.årsbeløp = årsbeløp;
        this.opptjeningsår = opptjeningsår;
    }

    public Beløp getÅrsbeløp() {
        return årsbeløp;
    }

    public int getOpptjeningsår() {
        return opptjeningsår;
    }
}
