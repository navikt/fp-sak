package no.nav.foreldrepenger.økonomistøtte;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.mapper.OppdragInput;

public class OppdragMedPositivKvitteringTestUtil {

    public static Oppdragskontroll opprett(OppdragskontrollTjeneste oppdragskontrollTjeneste, OppdragInput input) {
        var oppdragskontroll = oppdragskontrollTjeneste.opprettOppdrag(input).get();
        oppdragskontrollTjeneste.lagre(oppdragskontroll);
        OppdragKvitteringTestUtil.lagPositiveKvitteringer(oppdragskontroll);
        return oppdragskontroll;
    }
}
