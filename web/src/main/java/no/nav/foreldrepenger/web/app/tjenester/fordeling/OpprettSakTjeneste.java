package no.nav.foreldrepenger.web.app.tjenester.fordeling;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.FagsakTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingTema;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Journalpost;
import no.nav.foreldrepenger.domene.bruker.NavBrukerTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.vedtak.exception.TekniskException;

@ApplicationScoped
public class OpprettSakTjeneste {

    private FagsakTjeneste fagsakTjeneste;
    private NavBrukerTjeneste brukerTjeneste;

    public OpprettSakTjeneste() {
        //For CDI
    }

    @Inject
    public OpprettSakTjeneste(FagsakTjeneste fagsakTjeneste, NavBrukerTjeneste brukerTjeneste) {
        this.fagsakTjeneste = fagsakTjeneste;
        this.brukerTjeneste = brukerTjeneste;
    }

    Saksnummer opprettSak(FagsakYtelseType ytelseType, AktørId aktørId, JournalpostId journalpostId) {
        if (journalpostId == null) {
            return opprettSakVL(aktørId, ytelseType).getSaksnummer();
        }
        return finnEllerOpprettFagSak(journalpostId, ytelseType, aktørId).getSaksnummer();
    }

    private Fagsak finnEllerOpprettFagSak(JournalpostId journalpostId, FagsakYtelseType ytelseType, AktørId bruker) {
        return fagsakTjeneste.hentJournalpost(journalpostId).map(Journalpost::getFagsak)
            .orElseGet(() -> opprettSakVL(bruker, ytelseType, journalpostId));
    }

    private Fagsak opprettSakVL(AktørId bruker, FagsakYtelseType ytelseType) {
        var navBruker = brukerTjeneste.hentEllerOpprettFraAktørId(bruker);
        return fagsakTjeneste.opprettFagsak(ytelseType, navBruker);
    }

    private Fagsak opprettSakVL(AktørId bruker, FagsakYtelseType ytelseType, JournalpostId journalpostId) {
        var fagsak = opprettSakVL(bruker, ytelseType);
        knyttSakOgJournalpost(fagsak.getSaksnummer(), journalpostId);
        return fagsak;
    }

    FagsakYtelseType utledYtelseType(BehandlingTema behandlingTema) {
        var fagsakYtelseType = behandlingTema.getFagsakYtelseType();
        if (FagsakYtelseType.UDEFINERT.equals(fagsakYtelseType)) {
            throw new TekniskException("FP-106651", "Ukjent behandlingstemakode " + behandlingTema.getOffisiellKode());
        }
        return fagsakYtelseType;
    }

    Optional<Fagsak> finnSak(Saksnummer saksnummer) {
        return fagsakTjeneste.finnFagsakGittSaksnummer(saksnummer, false);
    }

    void knyttSakOgJournalpost(Saksnummer saksnummer, JournalpostId journalpostId) {
        //Sjekk om det allerede finnes knytning.
        var journalpost = fagsakTjeneste.hentJournalpost(journalpostId);
        if (journalpost.isPresent()) {
            var knyttetTilSaksnummer = journalpost.get().getFagsak().getSaksnummer();
            if (knyttetTilSaksnummer.equals(saksnummer)) {
                //Vi har knytning mot samme sak. Vi er HAPPY og returnerer herfra.
                return;
            }
            //Knyttet til en annen fagsak
            throw journalpostAlleredeKnyttetTilAnnenFagsak(journalpostId, knyttetTilSaksnummer,
                saksnummer.getVerdi());
        }

        //HER: Finnes ikke knytnign mellom journalpost og sak. La oss oprpette en:
        var fagsak = fagsakTjeneste.finnFagsakGittSaksnummer(saksnummer, true);
        fagsak.ifPresent(fagsakId -> fagsakTjeneste.lagreJournalPost(new Journalpost(journalpostId, fagsakId)));
    }

    public void flyttJournalpostTilSak(JournalpostId journalPostId, Saksnummer saksnummer) {
        var journalpost = fagsakTjeneste.hentJournalpost(journalPostId).orElse(null);
        var fagsak = fagsakTjeneste.finnFagsakGittSaksnummer(saksnummer, true);
        if (journalpost != null && fagsak.isPresent()) {
            journalpost.knyttJournalpostTilFagsak(fagsak.get());
            fagsakTjeneste.lagreJournalPost(journalpost);
        }
    }

    private static TekniskException journalpostAlleredeKnyttetTilAnnenFagsak(JournalpostId journalPostId,
                                                                             Saksnummer tilknyttetSak,
                                                                             String forsøktSak) {
        var msg = String.format("Journalpost-Fagsak knytning finnes allerede. Journalpost %s er knyttet "
            + "mot fagsak %s. Forsøkt knyttet mot sak %s", journalPostId, tilknyttetSak, forsøktSak);
        return new TekniskException("FP-863070", msg);
    }

}
