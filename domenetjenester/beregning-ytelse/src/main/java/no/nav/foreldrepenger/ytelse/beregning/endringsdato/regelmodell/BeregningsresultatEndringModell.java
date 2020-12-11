package no.nav.foreldrepenger.ytelse.beregning.endringsdato.regelmodell;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BeregningsresultatEndringModell {
    private final BeregningsresultatFeriepengerEndringModell feriepenger;
    private List<BeregningsresultatPeriodeEndringModell> beregningsresultatperioder = new ArrayList<>();

    public BeregningsresultatEndringModell(BeregningsresultatFeriepengerEndringModell feriepenger,
                                           List<BeregningsresultatPeriodeEndringModell> beregningsresultatperioder) {
        this.feriepenger = feriepenger;
        this.beregningsresultatperioder = beregningsresultatperioder;
    }

    public Optional<BeregningsresultatFeriepengerEndringModell> getFeriepenger() {
        return Optional.ofNullable(feriepenger);
    }

    public List<BeregningsresultatPeriodeEndringModell> getBeregningsresultatperioder() {
        return beregningsresultatperioder;
    }

}
