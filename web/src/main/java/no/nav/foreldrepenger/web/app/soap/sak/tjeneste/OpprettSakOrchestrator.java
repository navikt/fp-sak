package no.nav.foreldrepenger.web.app.soap.sak.tjeneste;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingTema;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
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
        Personinfo bruker = opprettSakTjeneste.hentBruker(aktørId);
        FagsakYtelseType ytelseType = opprettSakTjeneste.utledYtelseType(behandlingTema);
        Fagsak fagsak = opprettSakTjeneste.opprettSakVL(bruker, ytelseType);
        return opprettEllerFinnGsak(bruker, fagsak);
    }

    public Saksnummer opprettSak(JournalpostId journalpostId, BehandlingTema behandlingTema, AktørId aktørId) {
        Saksnummer saksnummer;
        Personinfo bruker = opprettSakTjeneste.hentBruker(aktørId);
        FagsakYtelseType ytelseType = opprettSakTjeneste.utledYtelseType(behandlingTema);
        Fagsak fagsak = finnEllerOpprettFagSak(journalpostId, ytelseType, bruker);
        if (fagsak.getSaksnummer() != null) {
            saksnummer = fagsak.getSaksnummer();
        } else {
            saksnummer = opprettEllerFinnGsak(bruker, fagsak);
        }
        return saksnummer;
    }

    public boolean harAktivSak(AktørId aktørId, BehandlingTema behandlingTema) {
        return fagsakRepository.hentForBruker(aktørId).stream()
            .filter(fs -> fs.getYtelseType().equals(behandlingTema.getFagsakYtelseType()))
            .anyMatch(fs -> !FagsakStatus.AVSLUTTET.equals(fs.getStatus()));
    }

    private Fagsak finnEllerOpprettFagSak(JournalpostId journalpostId, FagsakYtelseType ytelseType, Personinfo bruker) {
        Optional<Journalpost> journalpost = fagsakRepository.hentJournalpost(journalpostId);
        if (journalpost.isPresent()) {
            return journalpost.get().getFagsak();
        }

        return opprettSakTjeneste.opprettSakVL(bruker, ytelseType, journalpostId);
    }

    private Saksnummer opprettEllerFinnGsak(Personinfo bruker, Fagsak fagsak) {
        Saksnummer saksnummer = opprettSakTjeneste.opprettEllerFinnGsak(fagsak.getId(), bruker);
        opprettSakTjeneste.oppdaterFagsakMedGsakSaksnummer(fagsak.getId(), saksnummer);
        return saksnummer;
    }
}
