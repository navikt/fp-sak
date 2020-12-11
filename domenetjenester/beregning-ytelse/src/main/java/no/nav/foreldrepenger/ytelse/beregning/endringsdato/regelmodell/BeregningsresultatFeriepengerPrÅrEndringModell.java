package no.nav.foreldrepenger.ytelse.beregning.endringsdato.regelmodell;

import no.nav.foreldrepenger.domene.typer.Beløp;

import java.time.Year;

public class BeregningsresultatFeriepengerPrÅrEndringModell {
    private Beløp årsbeløp;
    private Year opptjeningsår;

    public BeregningsresultatFeriepengerPrÅrEndringModell() {
        // For JSON-Deserialisering i feilsøking
    }

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
