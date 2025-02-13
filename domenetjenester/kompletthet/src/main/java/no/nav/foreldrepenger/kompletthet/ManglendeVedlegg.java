package no.nav.foreldrepenger.kompletthet;

import java.util.Objects;

import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;

public record ManglendeVedlegg(DokumentTypeId dokumentType, String arbeidsgiver, Boolean brukerHarSagtAtIkkeKommer) {

    public ManglendeVedlegg(DokumentTypeId dokumentType) {
        this(dokumentType, null, false);
    }

    public ManglendeVedlegg(DokumentTypeId dokumentType, String arbeidsgiver) {
        this(dokumentType, arbeidsgiver, false);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        var that = (ManglendeVedlegg) o;
        return Objects.equals(arbeidsgiver, that.arbeidsgiver)
            && dokumentType == that.dokumentType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(dokumentType, arbeidsgiver);
    }
}
