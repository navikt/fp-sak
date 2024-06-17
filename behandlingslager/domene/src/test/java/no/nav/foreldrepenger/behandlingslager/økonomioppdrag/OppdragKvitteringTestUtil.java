package no.nav.foreldrepenger.behandlingslager.økonomioppdrag;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.Alvorlighetsgrad;

public class OppdragKvitteringTestUtil {
    public static OppdragKvittering lagPositivKvitting(Oppdrag110 o110) {
        return lagOppdragKvittering(o110, Alvorlighetsgrad.OK);
    }

    private static OppdragKvittering lagOppdragKvittering(Oppdrag110 o110, Alvorlighetsgrad alvorlighetsgrad) {
        return OppdragKvittering.builder().medAlvorlighetsgrad(alvorlighetsgrad).medOppdrag110(o110).build();
    }
}
