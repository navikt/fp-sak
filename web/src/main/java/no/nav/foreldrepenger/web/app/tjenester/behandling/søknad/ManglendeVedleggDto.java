package no.nav.foreldrepenger.web.app.tjenester.behandling.søknad;

import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;

public class ManglendeVedleggDto {
    private DokumentTypeId dokumentType;
    private ArbeidsgiverDto arbeidsgiver;
    private String arbeidsgiverReferanse;
    private boolean brukerHarSagtAtIkkeKommer = false;

    public DokumentTypeId getDokumentType() {
        return dokumentType;
    }

    public void setDokumentType(DokumentTypeId dokumentType) {
        this.dokumentType = dokumentType;
    }

    public ArbeidsgiverDto getArbeidsgiver() {
        return arbeidsgiver;
    }

    public void setArbeidsgiver(ArbeidsgiverDto arbeidsgiver) {
        this.arbeidsgiver = arbeidsgiver;
    }

    public String getArbeidsgiverReferanse() {
        return arbeidsgiverReferanse;
    }

    public void setArbeidsgiverReferanse(String arbeidsgiverReferanse) {
        this.arbeidsgiverReferanse = arbeidsgiverReferanse;
    }

    public boolean getBrukerHarSagtAtIkkeKommer() {
        return brukerHarSagtAtIkkeKommer;
    }

    public void setBrukerHarSagtAtIkkeKommer(boolean brukerHarSagtAtIkkeKommer) {
        this.brukerHarSagtAtIkkeKommer = brukerHarSagtAtIkkeKommer;
    }
}
