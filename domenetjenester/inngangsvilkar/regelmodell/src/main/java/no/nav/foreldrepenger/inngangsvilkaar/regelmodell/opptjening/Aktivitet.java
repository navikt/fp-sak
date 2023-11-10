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
        if (ARBEID.equals(aktivitetType) || FRILANSREG.equals(aktivitetType) || LØNN.equals(aktivitetType)) {
            Objects.requireNonNull(aktivitetReferanse, "aktivitetReferanse må være satt");
            Objects.requireNonNull(referanseType, "referanseType må være satt");
        }
        this.aktivitetType = aktivitetType;
        this.aktivitetReferanse = aktivitetReferanse;
        this.referanseType = referanseType;
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
        }
        if (obj == null || !obj.getClass().equals(this.getClass())) {
            return false;
        }
        var other = (Aktivitet) obj;

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
        return getClass().getSimpleName() + "<type=" + aktivitetType
                + (aktivitetReferanse == null ? "" : ", referanse=" + aktivitetReferanse.replaceAll("^\\d{5}", "*****"))
                + (referanseType == null ? "" : ", referanseType=" + referanseType)
                + ">";

    }
}
