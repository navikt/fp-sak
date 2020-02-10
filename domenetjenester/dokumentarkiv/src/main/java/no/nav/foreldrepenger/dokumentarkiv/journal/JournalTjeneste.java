package no.nav.foreldrepenger.dokumentarkiv.journal;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.dokumentarkiv.ArkivJournalPost;
import no.nav.foreldrepenger.domene.typer.JournalpostId;

@ApplicationScoped
public class JournalTjeneste {

    private InngåendeJournalAdapter inngaaendeJournalAdapter;

    public JournalTjeneste() {
        // NOSONAR: cdi
    }

    @Inject
    public JournalTjeneste(InngåendeJournalAdapter inngaaendeJournalAdapter) {
        this.inngaaendeJournalAdapter = inngaaendeJournalAdapter;
    }

    public List<JournalMetadata> hentMetadata(JournalpostId journalpostId) {
        return inngaaendeJournalAdapter.hentMetadata(journalpostId);
    }

    public ArkivJournalPost hentInngåendeJournalpostHoveddokument(JournalpostId journalpostId) {
        return inngaaendeJournalAdapter.hentInngåendeJournalpostHoveddokument(journalpostId);
    }
}
