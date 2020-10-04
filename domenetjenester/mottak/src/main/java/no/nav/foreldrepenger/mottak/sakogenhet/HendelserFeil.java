package no.nav.foreldrepenger.mottak.sakogenhet;

import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.LogLevel;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;

public interface HendelserFeil extends DeklarerteFeil {
    HendelserFeil FACTORY = FeilFactory.create(HendelserFeil.class);

    @TekniskFeil(feilkode = "FP-330623",
            feilmelding = "Fagsak allerede koblet, saksnummer: %s",
            logLevel = LogLevel.WARN)
    Feil fagsakAlleredeKoblet(Saksnummer saksnummer);

    @TekniskFeil(feilkode = "FP-388501",
            feilmelding = "OBS varsle produkteier: : Familiehendelse uten dato, saksnummer: %s",
            logLevel = LogLevel.WARN)
    Feil familiehendelseUtenDato(Saksnummer saksnummer);

    @TekniskFeil(feilkode = "FP-059216",
            feilmelding = "OBS varsle produkteier: Flere mulige fagsaker å koble til for saksnummer: %s kandidater: %s",
            logLevel = LogLevel.WARN)
    Feil flereMuligeFagsakerÅKobleTil(Saksnummer saksnummer, String kandidater);
}
