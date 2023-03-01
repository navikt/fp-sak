package no.nav.foreldrepenger.ytelse.beregning.regelmodell.feriepenger;

import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;

class SjekkOmBrukerHarFeriekvoteTilgjengelig extends LeafSpecification<BeregningsresultatFeriepengerRegelModell> {
    public static final String ID = "FP_BR 8.8";
    public static final String BESKRIVELSE = "Har bruker flere dager igjen av feriekvoten sin?";


    SjekkOmBrukerHarFeriekvoteTilgjengelig() {
        super(ID, BESKRIVELSE);
    }

    @Override
    public Evaluation evaluate(BeregningsresultatFeriepengerRegelModell regelModell) {
        var harFeriekvoteTilgjengelig = regelModell.getAntallDagerFeriepenger() > 0;
        return harFeriekvoteTilgjengelig ? ja() : nei();
    }
}
