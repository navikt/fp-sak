package no.nav.foreldrepenger.domene.tid;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.LocalDate;

/**
 * Hibernate entitet som modellerer et dato intervall med nullable til og med dato.
 */
@Embeddable
public class ÅpenDatoIntervallEntitet extends AbstractLocalDateInterval {

    @Column(name = "fom")
    private LocalDate fomDato;

    @Column(name = "tom")
    private LocalDate tomDato;

    private ÅpenDatoIntervallEntitet() {
        // Hibernate
    }

    private ÅpenDatoIntervallEntitet(LocalDate fomDato, LocalDate tomDato) {
        if (fomDato == null && tomDato != null) {
            throw new IllegalArgumentException("Fra og med dato må være satt når til og med dato er satt.");
        }
        if (fomDato != null && tomDato != null && tomDato.isBefore(fomDato)) {
            throw new IllegalArgumentException("Til og med dato er før fra og med dato.");
        }
        this.fomDato = fomDato;
        this.tomDato = tomDato;
    }

    public static ÅpenDatoIntervallEntitet fraOgMedTilOgMed(LocalDate fomDato, LocalDate tomDato) {
        return new ÅpenDatoIntervallEntitet(fomDato, tomDato);
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
    public String toString() {
        var fom = fomDato != null ? fomDato.format(FORMATTER) : null;
        var tom = tomDato != null ? tomDato.format(FORMATTER) : null;
        return String.format("Periode: %s - %s", fom, tom);
    }
}
