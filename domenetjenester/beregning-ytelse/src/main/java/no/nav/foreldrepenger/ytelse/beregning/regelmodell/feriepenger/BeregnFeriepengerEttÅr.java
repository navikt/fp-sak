package no.nav.foreldrepenger.ytelse.beregning.regelmodell.feriepenger;

import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;

import java.util.LinkedHashMap;
import java.util.Map;

class BeregnFeriepengerEttÅr extends LeafSpecification<BeregningsresultatFeriepengerRegelModell> {
    public static final String ID = "FP_BR 8.7";
    public static final String BESKRIVELSE = "Beregn feriepenger for periode som går over ett kalenderår.";

    BeregnFeriepengerEttÅr() {
        super(ID, BESKRIVELSE);
    }

    @Override
    public Evaluation evaluate(BeregningsresultatFeriepengerRegelModell regelModell) {
        Map<String, Object> resultater = new LinkedHashMap<>();

        var feriepengerPeriode = regelModell.getFeriepengerPeriode();
        BeregnFeriepengerForPeriode.beregn(resultater, regelModell.getBeregningsresultatPerioder(), feriepengerPeriode);

        return beregnet(resultater);
    }
}
