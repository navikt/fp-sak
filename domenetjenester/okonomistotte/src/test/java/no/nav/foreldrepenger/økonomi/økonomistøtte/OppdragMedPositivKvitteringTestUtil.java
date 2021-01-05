package no.nav.foreldrepenger.økonomi.økonomistøtte;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;

public class OppdragMedPositivKvitteringTestUtil {
    public static Oppdragskontroll opprett(OppdragskontrollTjeneste oppdragskontrollTjeneste, Behandling behandling) {
        Oppdragskontroll oppdragskontroll = oppdragskontrollTjeneste.opprettOppdrag(behandling.getId(), 471L).get();
        oppdragskontrollTjeneste.lagre(oppdragskontroll);
        OppdragKvitteringTestUtil.lagPositiveKvitteringer(oppdragskontroll);
        return oppdragskontroll;
    }

    public static Oppdragskontroll opprett(OppdragskontrollTjeneste oppdragskontrollTjeneste, Behandling behandling, Long ptid) {
        Oppdragskontroll oppdragskontroll = oppdragskontrollTjeneste.opprettOppdrag(behandling.getId(), ptid).get();
        oppdragskontrollTjeneste.lagre(oppdragskontroll);
        OppdragKvitteringTestUtil.lagPositiveKvitteringer(oppdragskontroll);
        return oppdragskontroll;
    }
}
