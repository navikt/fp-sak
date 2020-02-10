package no.nav.foreldrepenger.ytelse.beregning.regelmodell;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import no.nav.fpsak.tidsserie.LocalDateInterval;

public class UttakResultatPeriode {
    private LocalDateInterval periode;
    private List<UttakAktivitet> uttakAktiviteter;
    private boolean erOppholdsPeriode;


    public UttakResultatPeriode(LocalDate fom, LocalDate tom, List<UttakAktivitet> uttakAktiviteter, boolean erOppholdsPeriode) {
        this.periode = new LocalDateInterval(fom, tom);
        if (uttakAktiviteter == null || uttakAktiviteter.isEmpty()) {
            this.uttakAktiviteter = Collections.emptyList();
        } else {
            this.uttakAktiviteter = Collections.unmodifiableList(uttakAktiviteter);
        }
        this.erOppholdsPeriode = erOppholdsPeriode;
    }

    public LocalDate getFom() {
        return periode.getFomDato();
    }

    public LocalDate getTom() {
        return periode.getTomDato();
    }

    public List<UttakAktivitet> getUttakAktiviteter() {
        return uttakAktiviteter;
    }

    public boolean inneholder(LocalDate dato) {
        return periode.encloses(dato);
    }

    public boolean getErOppholdsPeriode() {
        return erOppholdsPeriode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof UttakResultatPeriode)) {
            return false;
        }
        UttakResultatPeriode that = (UttakResultatPeriode) obj;

        return Objects.equals(this.periode, that.periode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(periode);
    }
}
