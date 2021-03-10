package no.nav.foreldrepenger.domene.tid;

import java.time.LocalDate;
import java.util.Objects;

public class SimpleLocalDateInterval extends AbstractLocalDateInterval{

    private LocalDate fomDato;
    private LocalDate tomDato;

    public SimpleLocalDateInterval(LocalDate fomDato, LocalDate tomDato) {
        this.fomDato = fomDato;
        this.tomDato = tomDato;
    }

    public static SimpleLocalDateInterval fraOgMedTomNotNull(LocalDate fomDato, LocalDate tomDato) {
        return tomDato != null ? new SimpleLocalDateInterval(fomDato, tomDato) : new SimpleLocalDateInterval(fomDato, TIDENES_ENDE);
    }

    public static SimpleLocalDateInterval fraOgMed(LocalDate fomDato) {
        return new SimpleLocalDateInterval(fomDato, TIDENES_ENDE);
    }


    @Override
    public LocalDate getFomDato() {
        return fomDato;
    }

    @Override
    public LocalDate getTomDato() {
        return tomDato;
    }

    @Override
    protected AbstractLocalDateInterval lagNyPeriode(LocalDate fomDato, LocalDate tomDato) {
        return new SimpleLocalDateInterval(fomDato, tomDato);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        SimpleLocalDateInterval that = (SimpleLocalDateInterval) o;
        return Objects.equals(fomDato, that.fomDato) && Objects.equals(tomDato, that.tomDato);
    }
}
