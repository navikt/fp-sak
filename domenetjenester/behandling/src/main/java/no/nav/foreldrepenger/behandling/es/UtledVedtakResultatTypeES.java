package no.nav.foreldrepenger.behandling.es;

import java.util.Objects;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;

public class UtledVedtakResultatTypeES {
    private UtledVedtakResultatTypeES() {
        // hide public contructor
    }

    public static VedtakResultatType utled(BehandlingType behandlingType, BehandlingResultatType behandlingResultatType) {
        Objects.requireNonNull(behandlingResultatType);

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
        return VedtakResultatType.AVSLAG;
    }
}
