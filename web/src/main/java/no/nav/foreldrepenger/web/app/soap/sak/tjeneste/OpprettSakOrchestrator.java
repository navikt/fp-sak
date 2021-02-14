package no.nav.foreldrepenger.web.app.soap.sak.tjeneste;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

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
        FagsakYtelseType ytelseType = opprettSakTjeneste.utledYtelseType(behandlingTema);
        Fagsak fagsak = opprettSakTjeneste.opprettSakVL(aktørId, ytelseType);
        return fagsak.getSaksnummer();
    }

    public Saksnummer opprettSak(JournalpostId journalpostId, BehandlingTema behandlingTema, AktørId aktørId) {
        FagsakYtelseType ytelseType = opprettSakTjeneste.utledYtelseType(behandlingTema);
        Fagsak fagsak = finnEllerOpprettFagSak(journalpostId, ytelseType, aktørId);
        return fagsak.getSaksnummer();
    }

    public boolean harAktivSak(AktørId aktørId, BehandlingTema behandlingTema) {
        var ytelsetype = behandlingTema.getFagsakYtelseType();
        return fagsakRepository.hentForBruker(aktørId).stream()
            .filter(Fagsak::erÅpen)
            .map(Fagsak::getYtelseType)
            .anyMatch(ytelsetype::equals);
    }

    private Fagsak finnEllerOpprettFagSak(JournalpostId journalpostId, FagsakYtelseType ytelseType, AktørId bruker) {
        return fagsakRepository.hentJournalpost(journalpostId).map(Journalpost::getFagsak)
            .orElseGet(() -> opprettSakTjeneste.opprettSakVL(bruker, ytelseType, journalpostId));
    }
}
