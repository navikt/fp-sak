package no.nav.foreldrepenger.økonomistøtte;

import java.util.List;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.OppdragKvittering;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.Alvorlighetsgrad;

public class OppdragKvitteringTestUtil {
    public static List<OppdragKvittering> lagPositiveKvitteringer(Oppdragskontroll oppdragskontroll) {
        return oppdragskontroll.getOppdrag110Liste().stream().map(OppdragKvitteringTestUtil::lagPositivKvitting)
            .collect(Collectors.toList());
    }
    static List<OppdragKvittering> lagNegativeKvitteringer(Oppdragskontroll oppdragskontroll) {
        return oppdragskontroll.getOppdrag110Liste().stream().map(OppdragKvitteringTestUtil::lagNegativKvitting)
            .collect(Collectors.toList());
    }

    public static OppdragKvittering lagPositivKvitting(Oppdrag110 o110) {
        return lagOppdragKvittering(o110, Alvorlighetsgrad.OK);
    }

    public static OppdragKvittering lagNegativKvitting(Oppdrag110 o110) {
        return lagOppdragKvittering(o110, Alvorlighetsgrad.AVSLAG);
    }

    private static OppdragKvittering lagOppdragKvittering(Oppdrag110 o110, Alvorlighetsgrad alvorlighetsgrad) {
        return OppdragKvittering.builder()
            .medAlvorlighetsgrad(alvorlighetsgrad)
            .medOppdrag110(o110)
            .build();
    }
}
