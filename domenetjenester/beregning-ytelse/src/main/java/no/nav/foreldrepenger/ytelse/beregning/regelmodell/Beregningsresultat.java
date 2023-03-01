package no.nav.foreldrepenger.ytelse.beregning.regelmodell;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Beregningsresultat {

    private final List<BeregningsresultatPeriode> beregningsresultatPerioder;

    private Beregningsresultat(List<BeregningsresultatPeriode> beregningsresultatPerioder) {
        this.beregningsresultatPerioder =  beregningsresultatPerioder;
    }

    public List<BeregningsresultatPeriode> getBeregningsresultatPerioder() {
        return beregningsresultatPerioder;
    }

    public void addBeregningsresultatPeriode(BeregningsresultatPeriode brPeriode){
        Objects.requireNonNull(brPeriode, "beregningsresultatPeriode");
        if (!beregningsresultatPerioder.contains(brPeriode)) {
            beregningsresultatPerioder.add(brPeriode);
        }
    }

    public static Beregningsresultat opprett() {
        return new Beregningsresultat(new ArrayList<>());
    }

}
