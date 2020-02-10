package no.nav.foreldrepenger.økonomi.økonomistøtte;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.OppdragKvittering;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.OppdragskontrollConstants;

public class OppdragKvitteringTjeneste {

    private OppdragKvitteringTjeneste() {
        // hide public constructor
    }

    public static boolean harPositivKvittering(Oppdrag110 oppdrag110) {
        return oppdrag110.erKvitteringMottatt() && erPositivKvittering(oppdrag110.getOppdragKvittering());
    }

    private static boolean erPositivKvittering(OppdragKvittering oppdragKvittering) {
        int alvorlighetsgrad = Integer.parseInt(oppdragKvittering.getAlvorlighetsgrad());
        return alvorlighetsgrad <= OppdragskontrollConstants.POSITIV_KVITTERING;
    }
}
