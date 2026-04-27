package no.nav.foreldrepenger.behandlingslager.behandling.dokument;

import no.nav.foreldrepenger.behandlingslager.kodeverk.DatabaseKode;

public enum MellomlagringType implements DatabaseKode {
    VARSEL_REVURDERING,
    INNHENT_OPPLYSNINGER,
    VEDTAKSBREV,
    PAPIRSØKNAD,
    ;

    public static MellomlagringType fraDokumentMalType(DokumentMalType dokumentMalType) {
        if (dokumentMalType == null) {
            return null;
        }
        return switch (dokumentMalType) {
            case VARSEL_OM_REVURDERING -> VARSEL_REVURDERING;
            case INNHENTE_OPPLYSNINGER -> INNHENT_OPPLYSNINGER;
            default -> null;
        };
    }
}
