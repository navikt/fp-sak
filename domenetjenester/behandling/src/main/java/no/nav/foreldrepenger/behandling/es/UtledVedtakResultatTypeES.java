package no.nav.foreldrepenger.behandling.es;

import java.util.Objects;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;

public class UtledVedtakResultatTypeES {
    private UtledVedtakResultatTypeES() {
        // hide public contructor
    }

    public static VedtakResultatType utled(Behandling behandling) {
        BehandlingResultatType behandlingResultatType = behandling.getBehandlingsresultat().getBehandlingResultatType();
        Objects.requireNonNull(behandling, "behandling");
        Objects.requireNonNull(behandlingResultatType);
        
        if (BehandlingType.KLAGE.equals(behandling.getType())) {
            return VedtakResultatType.VEDTAK_I_KLAGEBEHANDLING;
        }
        if (BehandlingType.ANKE.equals(behandling.getType())) {
            return VedtakResultatType.VEDTAK_I_ANKEBEHANDLING;
        }
        if (BehandlingType.INNSYN.equals(behandling.getType())) {
            return VedtakResultatType.VEDTAK_I_INNSYNBEHANDLING;
        }
        if (BehandlingResultatType.INNVILGET.equals(behandlingResultatType)) {
            return VedtakResultatType.INNVILGET;
        }
        return VedtakResultatType.AVSLAG;
    }
}
