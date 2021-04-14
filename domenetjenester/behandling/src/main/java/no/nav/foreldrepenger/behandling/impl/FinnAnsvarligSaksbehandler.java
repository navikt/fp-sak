package no.nav.foreldrepenger.behandling.impl;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;

public class FinnAnsvarligSaksbehandler {

    private static final String DEFAULT_ANSVARLIG_SAKSBEHANDLER = "VL";

    private FinnAnsvarligSaksbehandler() {
        // hide public contructor
    }

    public static String finn(Behandling behandling) {
        if (behandling.getAnsvarligBeslutter() != null && !behandling.getAnsvarligBeslutter().isBlank()) {
            return behandling.getAnsvarligBeslutter();
        }
        if (behandling.getAnsvarligSaksbehandler() != null && !behandling.getAnsvarligSaksbehandler().isBlank()) {
            return behandling.getAnsvarligSaksbehandler();
        }
        return DEFAULT_ANSVARLIG_SAKSBEHANDLER;
    }
}
