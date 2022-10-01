package no.nav.foreldrepenger.behandlingslager.behandling.klage;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;

public final class KlageVurderingBehandlingResultat {

    public static BehandlingResultatType tolkBehandlingResultatType(KlageVurdering vurdering, KlageVurderingOmgjør omgjør, boolean erPåklagdEksternBehandling) {
        if (erPåklagdEksternBehandling && KlageVurdering.STADFESTE_YTELSESVEDTAK.equals(vurdering)) {
            return BehandlingResultatType.KLAGE_TILBAKEKREVING_VEDTAK_STADFESTET;
        }
        return switch (vurdering) {
            case OPPHEVE_YTELSESVEDTAK -> BehandlingResultatType.KLAGE_YTELSESVEDTAK_OPPHEVET;
            case STADFESTE_YTELSESVEDTAK -> BehandlingResultatType.KLAGE_YTELSESVEDTAK_STADFESTET;
            case AVVIS_KLAGE -> BehandlingResultatType.KLAGE_AVVIST;
            case HJEMSENDE_UTEN_Å_OPPHEVE -> BehandlingResultatType.HJEMSENDE_UTEN_OPPHEVE;
            case UDEFINERT -> null;
            case MEDHOLD_I_KLAGE -> switch (omgjør) {
                case GUNST_MEDHOLD_I_KLAGE -> BehandlingResultatType.KLAGE_MEDHOLD;
                case DELVIS_MEDHOLD_I_KLAGE -> BehandlingResultatType.KLAGE_DELVIS_MEDHOLD;
                case UGUNST_MEDHOLD_I_KLAGE -> BehandlingResultatType.KLAGE_OMGJORT_UGUNST;
                case UDEFINERT -> throw new IllegalStateException("Klagevurdering omgjør uten spesifisert omgjøring");
            };
        };
    }


}
