package no.nav.foreldrepenger.behandling.fp;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;

public class UtledVedtakResultatType {
    private UtledVedtakResultatType() {
        // hide public contructor
    }

    public static VedtakResultatType utled(BehandlingType behandlingType, BehandlingResultatType behandlingResultatType) {
        if (BehandlingType.KLAGE.equals(behandlingType)) {
            return VedtakResultatType.VEDTAK_I_KLAGEBEHANDLING;
        }
        if (BehandlingType.ANKE.equals(behandlingType)) {
            return VedtakResultatType.VEDTAK_I_ANKEBEHANDLING;
        }
        if (BehandlingType.INNSYN.equals(behandlingType)) {
            return VedtakResultatType.VEDTAK_I_INNSYNBEHANDLING;
        }
        if (BehandlingResultatType.INNVILGET.equals(behandlingResultatType)) {
            return VedtakResultatType.INNVILGET;
        }
        if (BehandlingResultatType.FORELDREPENGER_ENDRET.equals(behandlingResultatType)) {
            return VedtakResultatType.INNVILGET;
        }
        if (BehandlingResultatType.INGEN_ENDRING.equals(behandlingResultatType)) {
            throw new IllegalStateException("Utviklerfeil: Kan ikke utlede vedtakresultat fra INGEN ENDRING");
        }
        if (BehandlingResultatType.OPPHØR.equals(behandlingResultatType)) {
            return VedtakResultatType.OPPHØR;
        }
        return VedtakResultatType.AVSLAG;
    }
}
