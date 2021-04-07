package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.fp.OpptjeningsvilkårForeldrepenger;

public class Aktivitet {

    public enum ReferanseType {
        ORGNR,
        AKTØRID;
    }

    //OpptjeningAktivitetType.ARBEID , FRILOPP
    static private String ARBEID = OpptjeningsvilkårForeldrepenger.ARBEID;
    static private String FRILANSREG = OpptjeningsvilkårForeldrepenger.FRILANSREGISTER;
    static private String LØNN = OpptjeningsvilkårForeldrepenger.LØNN;

    @JsonProperty("aktivitetType")
    private String aktivitetType;

    @JsonProperty("aktivitetReferanse")
    private String aktivitetReferanse;

    @JsonProperty("referanseType")
    private ReferanseType referanseType;

    @JsonCreator
    Aktivitet(){
        // for json
    }

    public Aktivitet(String aktivitetType, String aktivitetReferanse, ReferanseType referanseType) {
        Objects.requireNonNull(aktivitetType, "aktivitetType må være satt");
        Objects.requireNonNull(aktivitetReferanse, "aktivitetReferanse må være satt");
        Objects.requireNonNull(referanseType, "referanseType må være satt");
        this.aktivitetType = aktivitetType;
        this.aktivitetReferanse = aktivitetReferanse;
        this.referanseType = referanseType;
    }

    public Aktivitet(String aktivitetType) {
        if (ARBEID.equals(aktivitetType) || FRILANSREG.equals(aktivitetType) || LØNN.equals(aktivitetType)) {
            throw new IllegalArgumentException("Utvikler-feil: aktivitet ARBEID/FRILOPP/LØNN må ha referanse");
        }
        this.aktivitetType = aktivitetType;
    }

    public ReferanseType getReferanseType() {
        return referanseType;
    }

    public String getAktivitetType() {
        return aktivitetType;
    }

    public String getAktivitetReferanse() {
        return aktivitetReferanse;
    }

    public Aktivitet forInntekt() {
        return new Aktivitet(LØNN, aktivitetReferanse, referanseType);
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
                + (aktivitetReferanse == null ? "" : ", referanse=" + aktivitetReferanse.replaceAll("^\\d{5}", "*****")) //$NON-NLS-1$ //$NON-NLS-2$
                + (referanseType == null ? "" : ", referanseType=" + referanseType) //$NON-NLS-1$ //$NON-NLS-2$
                + ">"; //$NON-NLS-1$

    }
}
