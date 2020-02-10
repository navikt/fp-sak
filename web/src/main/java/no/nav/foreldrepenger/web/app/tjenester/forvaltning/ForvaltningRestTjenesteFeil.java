package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.LogLevel;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;

public interface ForvaltningRestTjenesteFeil extends DeklarerteFeil {

    ForvaltningRestTjenesteFeil FACTORY = FeilFactory.create(ForvaltningRestTjenesteFeil.class);

    @TekniskFeil(feilkode = "FP-189014", feilmelding = "Fagsak allerede avsluttet %s", logLevel = LogLevel.WARN)
    Feil ugyldigeSakStatus(String saksnummer);

}
