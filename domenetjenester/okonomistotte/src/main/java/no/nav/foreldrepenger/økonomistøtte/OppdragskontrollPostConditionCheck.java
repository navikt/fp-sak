package no.nav.foreldrepenger.økonomistøtte;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.vedtak.exception.TekniskException;

public class OppdragskontrollPostConditionCheck {

    private OppdragskontrollPostConditionCheck() {
    }

    public static void valider(Oppdragskontroll oppdragskontroll) {
        for (var oppdrag110 : oppdragskontroll.getOppdrag110Liste()) {
            valider(oppdrag110);
        }
    }

    private static void valider(Oppdrag110 oppdrag110) {
        if (oppdrag110.getOppdragslinje150Liste().isEmpty()) {
            throw new TekniskException("FP-611541", String.format(
                "PostCondition feilet for generert oppdrag. Fagområde=%s fagsystemid=%s. Oppdrag110 inneholder ingen oppdragslinjer. Det er ikke forventet.",
                oppdrag110.getKodeFagomrade(), oppdrag110.getFagsystemId()));
        }
    }
}
