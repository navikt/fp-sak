package no.nav.foreldrepenger.datavarehus.xml;

import no.nav.vedtak.exception.TekniskException;

public final class VedtakXmlFeil {

    private VedtakXmlFeil() {
    }

    public static TekniskException serialiseringsfeil(Long behandlingId, Exception cause) {
        var msg = String.format("Vedtak-XML kan ikke utarbeides for behandling %s, serialiseringsfeil", behandlingId);
        return new TekniskException("FP-190756", msg, cause);
    }
}
