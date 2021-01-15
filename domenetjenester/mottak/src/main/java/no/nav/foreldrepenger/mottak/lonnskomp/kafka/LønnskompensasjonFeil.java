package no.nav.foreldrepenger.mottak.lonnskomp.kafka;

import java.io.IOException;

import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.LogLevel;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;

public interface LønnskompensasjonFeil extends DeklarerteFeil {

    LønnskompensasjonFeil FACTORY = FeilFactory.create(LønnskompensasjonFeil.class);

    @TekniskFeil(feilkode = "FP-328774",
        feilmelding = "Feil under parsing av vedtak. key={%s} payload={%s}",
        logLevel = LogLevel.WARN)
    Feil parsingFeil(String key, String payload, IOException e);

    @TekniskFeil(feilkode = "FP-328775", feilmelding = "Feil ved oppslag av aktørID for mottaker av lønnskomp sak={%s}", logLevel = LogLevel.ERROR)
    Feil finnerIkkeAktørIdForPermittert(String sak);
}
