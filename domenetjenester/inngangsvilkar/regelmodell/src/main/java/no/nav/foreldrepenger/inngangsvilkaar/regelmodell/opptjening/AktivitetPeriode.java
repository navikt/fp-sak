package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.fpsak.tidsserie.LocalDateInterval;

/** Beskriver aktivitet for en angitt periode. */
public class AktivitetPeriode implements Comparable<AktivitetPeriode> {

    @JsonProperty("datoIntervall")
    private LocalDateInterval datoIntervall;

    @JsonProperty("aktivitet")
    private Aktivitet aktivitet;

    @JsonProperty("vurderingsStatus")
    private VurderingsStatus vurderingsStatus;

    @JsonCreator
    protected AktivitetPeriode() {
    }

    public AktivitetPeriode(LocalDateInterval datoIntervall,
                            Aktivitet aktivitet,
                            VurderingsStatus vurderingsStatus) {
        this.datoIntervall = datoIntervall;
        this.aktivitet = aktivitet;
        this.vurderingsStatus = vurderingsStatus;
    }

    /** Returner dag intervall. */
    public LocalDateInterval getDatoIntervall() {
        return datoIntervall;
    }

    public Aktivitet getAktivitet() {
        return aktivitet;
    }

    public VurderingsStatus getVurderingsStatus() {
        return vurderingsStatus;
    }


    @Override
    public int compareTo(AktivitetPeriode o) {
        return this.getDatoIntervall().compareTo(o.getDatoIntervall());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || !getClass().equals(obj.getClass())) {
            return false;
        }
        var other = (AktivitetPeriode) obj;
        return Objects.equals(getAktivitet(), other.getAktivitet())
                && Objects.equals(getDatoIntervall(), other.getDatoIntervall());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getDatoIntervall(), getAktivitet());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<datoIntervall=" + datoIntervall + ", aktivitet=" + getAktivitet() + ">";
    }

     public enum VurderingsStatus {
        TIL_VURDERING,
        VURDERT_GODKJENT,
        VURDERT_UNDERKJENT
    }

    public static AktivitetPeriode periodeTilVurdering(LocalDateInterval datoIntervall, Aktivitet aktivitet) {
        return new AktivitetPeriode(datoIntervall, aktivitet, AktivitetPeriode.VurderingsStatus.TIL_VURDERING);
    }
}
