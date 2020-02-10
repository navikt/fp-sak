package no.nav.foreldrepenger.dokumentbestiller;

import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.LogLevel;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;

public interface BrevFeil extends DeklarerteFeil {
    BrevFeil FACTORY = FeilFactory.create(BrevFeil.class);

    @TekniskFeil(feilkode = "FP-666915", feilmelding = "Ingen brevmal konfigurert for denne type behandlingen %d.", logLevel = LogLevel.ERROR)
    Feil ingenBrevmalKonfigurert(Long behandlingId);
}
