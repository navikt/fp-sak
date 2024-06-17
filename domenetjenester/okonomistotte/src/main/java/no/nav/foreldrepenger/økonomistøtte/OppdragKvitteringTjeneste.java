package no.nav.foreldrepenger.økonomistøtte;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.OppdragKvittering;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.Alvorlighetsgrad;

public class OppdragKvitteringTjeneste {

    private OppdragKvitteringTjeneste() {
        // hide public constructor
    }

    public static boolean harPositivKvittering(Oppdrag110 oppdrag110) {
        return oppdrag110.erKvitteringMottatt() && erPositivKvittering(oppdrag110.getOppdragKvittering());
    }

    private static boolean erPositivKvittering(OppdragKvittering oppdragKvittering) {
        return oppdragKvittering.getAlvorlighetsgrad().equals(Alvorlighetsgrad.OK) || oppdragKvittering.getAlvorlighetsgrad()
            .equals(Alvorlighetsgrad.OK_MED_MERKNAD);
    }
}
