package no.nav.foreldrepenger.økonomistøtte;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.OppdragKvittering;

public class OppdragKvitteringTjeneste {

    public static final int POSITIV_KVITTERING = 4;

    private OppdragKvitteringTjeneste() {
        // hide public constructor
    }

    public static boolean harPositivKvittering(Oppdrag110 oppdrag110) {
        return oppdrag110.erKvitteringMottatt() && erPositivKvittering(oppdrag110.getOppdragKvittering());
    }

    private static boolean erPositivKvittering(OppdragKvittering oppdragKvittering) {
        var alvorlighetsgrad = Integer.parseInt(oppdragKvittering.getAlvorlighetsgrad());
        return alvorlighetsgrad <= POSITIV_KVITTERING;
    }
}
