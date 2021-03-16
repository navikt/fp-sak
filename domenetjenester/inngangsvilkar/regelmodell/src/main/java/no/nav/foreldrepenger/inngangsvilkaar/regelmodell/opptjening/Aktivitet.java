package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Aktivitet {

    public enum ReferanseType {
        ORGNR,
        AKTØRID
    }

    //OpptjeningAktivitetType.ARBEID
    private static String ARBEID = "ARBEID";

    @JsonProperty("aktivitetType")
    private String aktivitetType;

    @JsonProperty("aktivitetReferanse")
    private AktivitetIdentifikator aktivitetReferanse;

    @JsonProperty("referanseType")
    private ReferanseType referanseType;

    @JsonCreator
    Aktivitet(){
        // for json
    }

    public Aktivitet(String aktivitetType, AktivitetIdentifikator aktivitetReferanse, ReferanseType referanseType) {
        Objects.requireNonNull(aktivitetType, "aktivitetType må være satt");
        Objects.requireNonNull(aktivitetReferanse, "aktivitetReferanse må være satt");
        Objects.requireNonNull(referanseType, "referanseType må være satt");
        this.aktivitetType = aktivitetType;
        this.aktivitetReferanse = aktivitetReferanse;
        this.referanseType = referanseType;
    }

    public Aktivitet(String aktivitetType) {
        if (ARBEID.equals(aktivitetType)) {
            throw new IllegalArgumentException("Utvikler-feil: aktivitet ARBEID må ha referanse");
        }
        this.aktivitetType = aktivitetType;
    }

    public ReferanseType getReferanseType() {
        return referanseType;
    }

    public String getAktivitetType() {
        return aktivitetType;
    }

    public AktivitetIdentifikator getAktivitetReferanse() {
        return aktivitetReferanse;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj == null || !obj.getClass().equals(this.getClass())) {
            return false;
        }
        Aktivitet other = (Aktivitet) obj;

        return Objects.equals(aktivitetType, other.aktivitetType)
                && Objects.equals(aktivitetReferanse, other.aktivitetReferanse)
                && Objects.equals(referanseType, other.referanseType);

    }

    @Override
    public int hashCode() {
        return Objects.hash(aktivitetType, aktivitetReferanse, referanseType);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<type=" + aktivitetType //$NON-NLS-1$
                + (aktivitetReferanse == null ? "" : ", referanse=" + aktivitetReferanse) //$NON-NLS-1$ //$NON-NLS-2$
                + (referanseType == null ? "" : ", referanseType=" + referanseType) //$NON-NLS-1$ //$NON-NLS-2$
                + ">"; //$NON-NLS-1$
    }
}
