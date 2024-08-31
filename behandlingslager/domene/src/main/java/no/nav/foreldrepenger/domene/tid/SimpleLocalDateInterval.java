package no.nav.foreldrepenger.domene.tid;

import java.time.LocalDate;
import java.util.Objects;

import no.nav.vedtak.konfig.Tid;

public class SimpleLocalDateInterval extends AbstractLocalDateInterval{

    private final LocalDate fomDato;
    private final LocalDate tomDato;

    public SimpleLocalDateInterval(LocalDate fomDato, LocalDate tomDato) {
        Objects.requireNonNull(fomDato, "fomDato");
        Objects.requireNonNull(tomDato, "tomDato");
        this.fomDato = fomDato;
        this.tomDato = tomDato;
    }

    public static SimpleLocalDateInterval fraOgMedTomNotNull(LocalDate fomDato, LocalDate tomDato) {
        return tomDato != null ? new SimpleLocalDateInterval(fomDato, tomDato) : new SimpleLocalDateInterval(fomDato, Tid.TIDENES_ENDE);
    }

    public static SimpleLocalDateInterval enDag(LocalDate fomTomDato) {
        return new SimpleLocalDateInterval(fomTomDato, fomTomDato);
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
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        if (!super.equals(o))
            return false;
        var that = (SimpleLocalDateInterval) o;
        return fomDato.equals(that.fomDato) && tomDato.equals(that.tomDato);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), fomDato, tomDato);
    }
}
