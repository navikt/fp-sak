package no.nav.foreldrepenger.domene.tid;

import java.time.LocalDate;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import jakarta.validation.constraints.NotNull;

import no.nav.vedtak.konfig.Tid;

/**
 * Hibernate entitet som modellerer et dato intervall.
 */
@Embeddable
public class DatoIntervallEntitet extends AbstractLocalDateInterval {

    @NotNull @Column(name = "fom")
    private LocalDate fomDato;

    @NotNull @Column(name = "tom")
    private LocalDate tomDato;

    private DatoIntervallEntitet() {
        // Hibernate
    }

    private DatoIntervallEntitet(LocalDate fomDato, LocalDate tomDato) {
        if (fomDato == null) {
            throw new IllegalArgumentException("Fra og med dato må være satt.");
        }
        if (tomDato == null) {
            throw new IllegalArgumentException("Til og med dato må være satt.");
        }
        if (tomDato.isBefore(fomDato)) {
            throw new IllegalArgumentException("Til og med dato før fra og med dato. Fom: " + fomDato + ", Tom: " + tomDato);
        }
        this.fomDato = fomDato;
        this.tomDato = tomDato;
    }

    public static DatoIntervallEntitet fraOgMedTilOgMed(LocalDate fomDato, LocalDate tomDato) {
        return new DatoIntervallEntitet(fomDato, tomDato);
    }

    public static DatoIntervallEntitet enDag(LocalDate fomTomDato) {
        return new DatoIntervallEntitet(fomTomDato, fomTomDato);
    }

    public static DatoIntervallEntitet fraOgMed(LocalDate fomDato) {
        return new DatoIntervallEntitet(fomDato, Tid.TIDENES_ENDE);
    }

    public static DatoIntervallEntitet fraOgMedPlusArbeidsdager(LocalDate fom, int antallArbeidsdager) {
        return DatoIntervallEntitet.fraOgMedTilOgMed(fom, finnTomDato(fom, antallArbeidsdager));
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
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var that = (DatoIntervallEntitet) o;
        return Objects.equals(getFomDato(), that.getFomDato()) &&
            Objects.equals(getTomDato(), that.getTomDato());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFomDato(), getTomDato());
    }
}
