package no.nav.foreldrepenger.web.app.soap.sak.tjeneste;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
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

    public OpprettSakOrchestrator() {
        // CDI
    }

    @Deprecated(forRemoval = true, since = "TFP-4124")
    public Saksnummer opprettSak(BehandlingTema behandlingTema, AktørId aktørId) {
        var ytelseType = opprettSakTjeneste.utledYtelseType(behandlingTema);
        var fagsak = opprettSakTjeneste.opprettSakVL(aktørId, ytelseType);
        return fagsak.getSaksnummer();
    }

    @Deprecated(forRemoval = true, since = "TFP-4124")
    public Saksnummer opprettSak(JournalpostId journalpostId, BehandlingTema behandlingTema, AktørId aktørId) {
        var ytelseType = opprettSakTjeneste.utledYtelseType(behandlingTema);
        var fagsak = finnEllerOpprettFagSak(journalpostId, ytelseType, aktørId);
        return fagsak.getSaksnummer();
    }

    @Deprecated(forRemoval = true, since = "TFP-4124")
    public boolean harAktivSak(AktørId aktørId, BehandlingTema behandlingTema) {
        var ytelsetype = behandlingTema.getFagsakYtelseType();
        return fagsakRepository.hentForBruker(aktørId).stream()
            .filter(Fagsak::erÅpen)
            .map(Fagsak::getYtelseType)
            .anyMatch(ytelsetype::equals);
    }

    public Saksnummer opprettSak(FagsakYtelseType ytelseType, AktørId aktørId, JournalpostId journalpostId) {
        if (journalpostId == null) {
            return opprettSakTjeneste.opprettSakVL(aktørId, ytelseType).getSaksnummer();
        }
        return finnEllerOpprettFagSak(journalpostId, ytelseType, aktørId).getSaksnummer();
    }

    private Fagsak finnEllerOpprettFagSak(JournalpostId journalpostId, FagsakYtelseType ytelseType, AktørId bruker) {
        return fagsakRepository.hentJournalpost(journalpostId).map(Journalpost::getFagsak)
            .orElseGet(() -> opprettSakTjeneste.opprettSakVL(bruker, ytelseType, journalpostId));
    }
}
