package no.nav.foreldrepenger.økonomistøtte;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.OppdragKvittering;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.Alvorlighetsgrad;

import java.util.List;

public class OppdragKvitteringTestUtil {
    public static List<OppdragKvittering> lagPositiveKvitteringer(Oppdragskontroll oppdragskontroll) {
        return oppdragskontroll.getOppdrag110Liste().stream().map(OppdragKvitteringTestUtil::lagPositivKvitting)
            .toList();
    }
    static List<OppdragKvittering> lagNegativeKvitteringer(Oppdragskontroll oppdragskontroll) {
        return oppdragskontroll.getOppdrag110Liste().stream().map(OppdragKvitteringTestUtil::lagNegativKvitting)
            .toList();
    }

    public static OppdragKvittering lagPositivKvitting(Oppdrag110 o110) {
        return lagOppdragKvittering(o110, Alvorlighetsgrad.OK);
    }

    public static OppdragKvittering lagNegativKvitting(Oppdrag110 o110) {
        return lagOppdragKvittering(o110, Alvorlighetsgrad.FEIL);
    }

    private static OppdragKvittering lagOppdragKvittering(Oppdrag110 o110, Alvorlighetsgrad alvorlighetsgrad) {
        return OppdragKvittering.builder()
            .medAlvorlighetsgrad(alvorlighetsgrad)
            .medOppdrag110(o110)
            .build();
    }
}
