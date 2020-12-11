package no.nav.foreldrepenger.ytelse.beregning.endringsdato.regelmodell;

import java.util.ArrayList;
import java.util.List;

public class BeregningsresultatEndringModell {
    private BeregningsresultatFeriepengerEndringModell feriepenger;
    private List<BeregningsresultatPeriodeEndringModell> beregningsresultatperioder = new ArrayList<>();

    public BeregningsresultatEndringModell(BeregningsresultatFeriepengerEndringModell feriepenger,
                                           List<BeregningsresultatPeriodeEndringModell> beregningsresultatperioder) {
        this.feriepenger = feriepenger;
        this.beregningsresultatperioder = beregningsresultatperioder;
    }

    public BeregningsresultatFeriepengerEndringModell getFeriepenger() {
        return feriepenger;
    }

    public List<BeregningsresultatPeriodeEndringModell> getBeregningsresultatperioder() {
        return beregningsresultatperioder;
    }

}
