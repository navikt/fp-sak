package no.nav.foreldrepenger.kompletthet;

import java.util.Objects;

import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;

public class ManglendeVedlegg {

    private final DokumentTypeId dokumentType;
    private final String arbeidsgiver;
    private Boolean brukerHarSagtAtIkkeKommer = false;

    public ManglendeVedlegg(DokumentTypeId dokumentType) {
        this(dokumentType, null);
    }

    public ManglendeVedlegg(DokumentTypeId dokumentType, String arbeidsgiver) {
        this.dokumentType = dokumentType;
        this.arbeidsgiver = arbeidsgiver;
    }

    public ManglendeVedlegg(DokumentTypeId dokumentType, String arbeidsgiver, Boolean brukerHarSagtAtIkkeKommer) {
        this.dokumentType = dokumentType;
        this.arbeidsgiver = arbeidsgiver;
        this.brukerHarSagtAtIkkeKommer = brukerHarSagtAtIkkeKommer;
    }

    public DokumentTypeId getDokumentType() {
        return dokumentType;
    }

    public String getArbeidsgiver() {
        return arbeidsgiver;
    }

    public Boolean getBrukerHarSagtAtIkkeKommer() {
        return brukerHarSagtAtIkkeKommer;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<"
                + "arbeidsgiver=" + arbeidsgiver +
                ", dokumentType=" + dokumentType +
                ", kommerIkke=" + brukerHarSagtAtIkkeKommer
                + ">";

    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || !obj.getClass().equals(this.getClass())) {
            return false;
        }
        var other = (ManglendeVedlegg) obj;
        return Objects.equals(arbeidsgiver, other.arbeidsgiver)
                && Objects.equals(dokumentType, other.dokumentType);

    }

    @Override
    public int hashCode() {
        return Objects.hash(dokumentType, arbeidsgiver);
    }
}
