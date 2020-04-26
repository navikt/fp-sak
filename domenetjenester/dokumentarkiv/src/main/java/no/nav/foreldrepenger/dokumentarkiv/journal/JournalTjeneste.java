package no.nav.foreldrepenger.dokumentarkiv.journal;

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

    public ArkivJournalPost hentInngåendeJournalpostHoveddokument(JournalpostId journalpostId) {
        return inngaaendeJournalAdapter.hentInngåendeJournalpostHoveddokument(journalpostId);
    }
}
