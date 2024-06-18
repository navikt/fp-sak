package no.nav.foreldrepenger.ytelse.beregning.regelmodell;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import no.nav.fpsak.tidsserie.LocalDateInterval;

public class BeregningsresultatPeriode {
    private final List<BeregningsresultatAndel> beregningsresultatAndelList = new ArrayList<>();
    private final LocalDateInterval periode;

    public BeregningsresultatPeriode(LocalDateInterval periode) {
        this.periode = periode;
    }

    public BeregningsresultatPeriode(LocalDate fom, LocalDate tom) {
        this(new LocalDateInterval(fom, tom));
    }

    public LocalDate getFom() {
        return periode.getFomDato();
    }

    public LocalDate getTom() {
        return periode.getTomDato();
    }

    public LocalDateInterval getPeriode() {
        return periode;
    }

    public List<BeregningsresultatAndel> getBeregningsresultatAndelList() {
        return beregningsresultatAndelList;
    }

    public boolean inneholder(LocalDate dato) {
        return periode.encloses(dato);
    }

    public void addBeregningsresultatAndel(BeregningsresultatAndel beregningsresultatAndel) {
        Objects.requireNonNull(beregningsresultatAndel, "beregningsresultatAndel");
        if (!beregningsresultatAndelList.contains(beregningsresultatAndel)) {
            beregningsresultatAndelList.add(beregningsresultatAndel);
        }
    }

}

