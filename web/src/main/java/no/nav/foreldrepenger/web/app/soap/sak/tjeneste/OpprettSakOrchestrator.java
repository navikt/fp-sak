package no.nav.foreldrepenger.web.app.soap.sak.tjeneste;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.aktør.PersoninfoSpråk;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingTema;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Journalpost;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

@ApplicationScoped
public class OpprettSakOrchestrator {
    private OpprettSakTjeneste opprettSakTjeneste;
    private FagsakRepository fagsakRepository;

    @Inject
    public OpprettSakOrchestrator(OpprettSakTjeneste opprettSakTjeneste, FagsakRepository fagsakRepository) {
        this.opprettSakTjeneste = opprettSakTjeneste;
        this.fagsakRepository = fagsakRepository;
    }

    public OpprettSakOrchestrator() { // NOSONAR: cdi
    }

    public Saksnummer opprettSak(BehandlingTema behandlingTema, AktørId aktørId) {
        PersoninfoSpråk bruker = opprettSakTjeneste.hentBruker(aktørId);
        FagsakYtelseType ytelseType = opprettSakTjeneste.utledYtelseType(behandlingTema);
        Fagsak fagsak = opprettSakTjeneste.opprettSakVL(bruker, ytelseType);
        return opprettEllerFinnGsak(aktørId, fagsak);
    }

    public Saksnummer opprettSak(JournalpostId journalpostId, BehandlingTema behandlingTema, AktørId aktørId) {
        Saksnummer saksnummer;
        PersoninfoSpråk bruker = opprettSakTjeneste.hentBruker(aktørId);
        FagsakYtelseType ytelseType = opprettSakTjeneste.utledYtelseType(behandlingTema);
        Fagsak fagsak = finnEllerOpprettFagSak(journalpostId, ytelseType, bruker);
        if (fagsak.getSaksnummer() != null) {
            saksnummer = fagsak.getSaksnummer();
        } else {
            saksnummer = opprettEllerFinnGsak(aktørId, fagsak);
        }
        return saksnummer;
    }

    public boolean harAktivSak(AktørId aktørId, BehandlingTema behandlingTema) {
        var ytelsetype = behandlingTema.getFagsakYtelseType();
        return fagsakRepository.hentForBruker(aktørId).stream()
            .filter(Fagsak::erÅpen)
            .map(Fagsak::getYtelseType)
            .anyMatch(ytelsetype::equals);
    }

    private Fagsak finnEllerOpprettFagSak(JournalpostId journalpostId, FagsakYtelseType ytelseType, PersoninfoSpråk bruker) {
        Optional<Journalpost> journalpost = fagsakRepository.hentJournalpost(journalpostId);
        if (journalpost.isPresent()) {
            return journalpost.get().getFagsak();
        }

        return opprettSakTjeneste.opprettSakVL(bruker, ytelseType, journalpostId);
    }

    private Saksnummer opprettEllerFinnGsak(AktørId aktørId, Fagsak fagsak) {
        Saksnummer saksnummer = opprettSakTjeneste.opprettEllerFinnGsak(aktørId);
        opprettSakTjeneste.oppdaterFagsakMedGsakSaksnummer(fagsak.getId(), saksnummer);
        return saksnummer;
    }
}
