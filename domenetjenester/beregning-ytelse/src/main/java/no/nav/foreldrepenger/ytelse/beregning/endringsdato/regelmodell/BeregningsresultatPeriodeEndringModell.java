package no.nav.foreldrepenger.ytelse.beregning.endringsdato.regelmodell;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class BeregningsresultatPeriodeEndringModell {
    private LocalDate fom;
    private LocalDate tom;
    private List<BeregningsresultatAndelEndringModell> andeler = new ArrayList<>();

    public BeregningsresultatPeriodeEndringModell() {
        // For JSON-deserialisering i feilsøking
    }

    public BeregningsresultatPeriodeEndringModell(LocalDate fom,
                                                  LocalDate tom,
                                                  List<BeregningsresultatAndelEndringModell> andeler) {
        this.fom = fom;
        this.tom = tom;
        this.andeler = andeler;
    }

    public LocalDate getFom() {
        return fom;
    }

    public LocalDate getTom() {
        return tom;
    }

    public List<BeregningsresultatAndelEndringModell> getAndeler() {
        return andeler;
    }
}
