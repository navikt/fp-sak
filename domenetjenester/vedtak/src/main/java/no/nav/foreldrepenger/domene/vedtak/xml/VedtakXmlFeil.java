package no.nav.foreldrepenger.domene.vedtak.xml;

import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.LogLevel;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;

public interface VedtakXmlFeil extends DeklarerteFeil {

    VedtakXmlFeil FACTORY = FeilFactory.create(VedtakXmlFeil.class);

    @TekniskFeil(feilkode = "FP-142918", feilmelding = "Vedtak-XML kan ikke utarbeides for behandling %s i tilstand %s", logLevel = LogLevel.WARN)
    Feil behandlingErIFeilTilstand(Long behandlingId, String statusKode);

    @TekniskFeil(feilkode = "FP-190756", feilmelding = "Vedtak-XML kan ikke utarbeides for behandling %s, serialiseringsfeil", logLevel = LogLevel.ERROR)
    Feil serialiseringsfeil(Long behandlingId, Exception cause);
}
