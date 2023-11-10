package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.fpsak.tidsserie.LocalDateInterval;

public class InntektPeriode {

    @JsonProperty("datoIntervall")
    private LocalDateInterval datoIntervall;

    @JsonProperty("aktivitet")
    private Aktivitet aktivitet;

    @JsonProperty("inntektBelop")
    private Long inntektBeløp;

    @JsonCreator
    InntektPeriode() {
        // for JSON
    }

    public InntektPeriode(LocalDateInterval datoIntervall, Aktivitet aktivitet, Long inntektBeløp) {
        this.datoIntervall = datoIntervall;
        this.aktivitet = aktivitet;
        this.inntektBeløp = inntektBeløp;
    }

    public LocalDateInterval getDatoInterval() {
        return datoIntervall;
    }

    public Long getInntektBeløp() {
        return inntektBeløp;
    }

    public Aktivitet getAktivitet() {
        return aktivitet;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || !getClass().equals(obj.getClass())) {
            return false;
        }
        var other = (InntektPeriode) obj;
        return Objects.equals(aktivitet, other.aktivitet)
                && Objects.equals(getDatoInterval(), other.getDatoInterval());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getDatoInterval(), aktivitet);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<datoIntervall=" + getDatoInterval() + ", aktivitetType=" + aktivitet + ">";
    }

}
