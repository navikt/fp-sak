package no.nav.foreldrepenger.web.app.soap.sak.tjeneste;

import static no.nav.vedtak.feil.LogLevel.WARN;

import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;

public interface OpprettSakFeil extends DeklarerteFeil {

    OpprettSakFeil FACTORY = FeilFactory.create(OpprettSakFeil.class);

    @TekniskFeil(feilkode = "FP-827920", feilmelding = "Finner ikke person med aktørID %s", logLevel = WARN, exceptionClass = UkjentPersonException.class)
    Feil finnerIkkePersonMedAktørId(AktørId aktørId);

    @TekniskFeil(feilkode = "FP-106651", feilmelding = "Ukjent behandlingstemakode %s", logLevel = WARN)
    Feil ukjentBehandlingstemaKode(String behandlingstemaKode);

    @TekniskFeil(feilkode = "FP-840572", feilmelding = "Finner ikke fagsak med angitt saksnummer %s", logLevel = WARN)
    Feil finnerIkkeFagsakMedSaksnummer(Saksnummer saksnummer);

    @TekniskFeil(feilkode = "FP-863070", feilmelding = "Journalpost-Fagsak knytning finnes allerede. Journalpost %s er knyttet mot fagsak %s. Forsøkt knyttet mot sak %s", logLevel = WARN)
    Feil JournalpostAlleredeKnyttetTilAnnenFagsak(JournalpostId journalPostId, Saksnummer tilknyttetSak, String forsøktSak);
}
