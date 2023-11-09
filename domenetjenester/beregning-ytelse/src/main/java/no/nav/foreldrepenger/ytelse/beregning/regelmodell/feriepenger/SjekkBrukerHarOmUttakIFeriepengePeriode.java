package no.nav.foreldrepenger.ytelse.beregning.regelmodell.feriepenger;

import java.time.LocalDate;
import java.util.List;

import no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatPeriode;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;

class SjekkBrukerHarOmUttakIFeriepengePeriode extends LeafSpecification<BeregningsresultatFeriepengerRegelModell> {
    public static final String ID = "FP_BR 8.4";
    public static final String BESKRIVELSE = "Har bruker uttak i feriepengeperiode?";


    SjekkBrukerHarOmUttakIFeriepengePeriode() {
        super(ID, BESKRIVELSE);
    }

    @Override
    public Evaluation evaluate(BeregningsresultatFeriepengerRegelModell regelModell) {
        var førsteUttaksDato = finnFørsteUttaksdag(regelModell.getBeregningsresultatPerioder());
        return regelModell.getFeriepengerPeriode().encloses(førsteUttaksDato) ? ja() : nei();
    }

    private LocalDate finnFørsteUttaksdag(List<BeregningsresultatPeriode> beregningsresultatPerioder) {
        return FinnBrukersFeriepengePeriode.finnFørsteUttaksdagArbeidstaker(beregningsresultatPerioder)
            .orElseThrow(() -> new IllegalStateException("Fant ingen perioder med utbetaling for bruker"));
    }
}
