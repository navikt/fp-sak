package no.nav.foreldrepenger.behandlingslager.behandling.anke;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;

public final class AnkeVurderingBehandlingResultat {

    public static BehandlingResultatType tolkBehandlingResultatType(AnkeVurdering vurdering, AnkeVurderingOmgjør omgjør) {
        return switch (vurdering) {
            case UDEFINERT -> null;
            case ANKE_AVVIS -> BehandlingResultatType.ANKE_AVVIST;
            case ANKE_STADFESTE_YTELSESVEDTAK -> BehandlingResultatType.ANKE_YTELSESVEDTAK_STADFESTET;
            case ANKE_HJEMSEND_UTEN_OPPHEV -> BehandlingResultatType.ANKE_HJEMSENDE_UTEN_OPPHEV;
            case ANKE_OPPHEVE_OG_HJEMSENDE -> BehandlingResultatType.ANKE_OPPHEVE_OG_HJEMSENDE;
            case ANKE_OMGJOER -> switch (omgjør) {
                case ANKE_TIL_GUNST -> BehandlingResultatType.ANKE_MEDHOLD;
                case ANKE_DELVIS_OMGJOERING_TIL_GUNST -> BehandlingResultatType.ANKE_DELVIS_MEDHOLD;
                case ANKE_TIL_UGUNST -> BehandlingResultatType.ANKE_OMGJORT_UGUNST;
                case UDEFINERT -> throw new IllegalStateException("Ankevurdering omgjør uten spesifisert omgjøring");
            };
        };
    }

}
