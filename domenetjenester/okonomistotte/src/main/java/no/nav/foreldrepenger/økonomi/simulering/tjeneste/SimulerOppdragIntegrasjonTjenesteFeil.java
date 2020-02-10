package no.nav.foreldrepenger.økonomi.simulering.tjeneste;

import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.LogLevel;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;

public interface SimulerOppdragIntegrasjonTjenesteFeil extends DeklarerteFeil {
    SimulerOppdragIntegrasjonTjenesteFeil FACTORY = FeilFactory.create(SimulerOppdragIntegrasjonTjenesteFeil.class);

    @TekniskFeil(feilkode = "FP-423523", feilmelding = "Start simulering feilet for behandlingId: %s", logLevel = LogLevel.WARN)
    Feil startSimuleringFeiletMedFeilmelding(Long behandlingId, Exception e);
}
