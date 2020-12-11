package no.nav.foreldrepenger.ytelse.beregning.endringsdato.regelmodell;

import no.nav.foreldrepenger.domene.typer.Beløp;

import java.time.Year;

public class BeregningsresultatFeriepengerPrÅrEndringModell {
    private final Beløp årsbeløp;
    private final Year opptjeningsår;

    public BeregningsresultatFeriepengerPrÅrEndringModell(Beløp årsbeløp,
                                                          Year opptjeningsår) {
        this.årsbeløp = årsbeløp;
        this.opptjeningsår = opptjeningsår;
    }

    public Beløp getÅrsbeløp() {
        return årsbeløp;
    }

    public Year getOpptjeningsår() {
        return opptjeningsår;
    }
}
