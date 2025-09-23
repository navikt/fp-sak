package no.nav.foreldrepenger.web.app.tjenester.behandling.søknad;

import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;

public record ManglendeVedleggDto(@NotNull DokumentTypeId dokumentType, @NotNull String dokumentTittel,
                                  String arbeidsgiverReferanse, @NotNull boolean brukerHarSagtAtIkkeKommer) {

    public ManglendeVedleggDto(DokumentTypeId dokumentType) {
        this(dokumentType, dokumentType.getNavn(), null, false);
    }

    public ManglendeVedleggDto(DokumentTypeId dokumentType, String arbeidsgiverReferanse, boolean brukerHarSagtAtIkkeKommer) {
        this(dokumentType, dokumentType.getNavn(), arbeidsgiverReferanse, brukerHarSagtAtIkkeKommer);
    }

}
