package no.nav.foreldrepenger.ytelse.beregning.regelmodell.feriepenger;

import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;

class SjekkOmFeriepengePeriodenGårOverFlereÅr extends LeafSpecification<BeregningsresultatFeriepengerRegelModell> {
    public static final String ID = "FP_BR 8.5";
    public static final String BESKRIVELSE = "Går feriepengeperioden over flere kalenderår?";


    SjekkOmFeriepengePeriodenGårOverFlereÅr() {
        super(ID, BESKRIVELSE);
    }

    @Override
    public Evaluation evaluate(BeregningsresultatFeriepengerRegelModell regelModell) {
        var feriepengerPeriode = regelModell.getFeriepengerPeriode();
        return feriepengerPeriode.getFomDato().getYear() < feriepengerPeriode.getTomDato().getYear() ? ja() : nei();
    }
}
