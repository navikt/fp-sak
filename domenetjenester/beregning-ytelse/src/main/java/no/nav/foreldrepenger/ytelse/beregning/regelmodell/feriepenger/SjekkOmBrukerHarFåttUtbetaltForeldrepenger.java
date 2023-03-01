package no.nav.foreldrepenger.ytelse.beregning.regelmodell.feriepenger;

import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;

class SjekkOmBrukerHarFåttUtbetaltForeldrepenger extends LeafSpecification<BeregningsresultatFeriepengerRegelModell> {
    public static final String ID = "FP_BR 8.2";
    public static final String BESKRIVELSE = "Har bruker fått utbetalt foreldrepenger i den totale stønadsperioden?";


    SjekkOmBrukerHarFåttUtbetaltForeldrepenger() {
        super(ID, BESKRIVELSE);
    }

    @Override
    public Evaluation evaluate(BeregningsresultatFeriepengerRegelModell regelModell) {
        var utbetaltForeldrepenger = regelModell.getBeregningsresultatPerioder().stream()
            .flatMap(p -> p.getBeregningsresultatAndelList().stream())
            .anyMatch(andel -> andel.getDagsats() > 0);

        return utbetaltForeldrepenger ? ja() : nei();
    }
}
