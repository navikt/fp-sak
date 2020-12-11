package no.nav.foreldrepenger.ytelse.beregning.endringsdato.regelmodell;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class BeregningsresultatPeriodeEndringModell {
    private final LocalDate fom;
    private final LocalDate tom;
    private List<BeregningsresultatAndelEndringModell> andeler = new ArrayList<>();

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
