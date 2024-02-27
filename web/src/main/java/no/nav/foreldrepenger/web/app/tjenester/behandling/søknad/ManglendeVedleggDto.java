package no.nav.foreldrepenger.web.app.tjenester.behandling.søknad;

import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;

public record ManglendeVedleggDto(DokumentTypeId dokumentType, String dokumentTittel,
                                  String arbeidsgiverReferanse, boolean brukerHarSagtAtIkkeKommer) {

    public ManglendeVedleggDto(DokumentTypeId dokumentType) {
        this(dokumentType, dokumentType.getNavn(), null, false);
    }

    public ManglendeVedleggDto(DokumentTypeId dokumentType, String arbeidsgiverReferanse, boolean brukerHarSagtAtIkkeKommer) {
        this(dokumentType, dokumentType.getNavn(), arbeidsgiverReferanse, brukerHarSagtAtIkkeKommer);
    }

}
