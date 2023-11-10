package no.nav.foreldrepenger.mottak.publiserer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.events.MottattDokumentPersistertEvent;

@ApplicationScoped
public class MottattDokumentPersistertPubliserer {

    private Event<MottattDokumentPersistertEvent> mottattDokumentPersistertEvent;

    MottattDokumentPersistertPubliserer() {
        //Cyclopedia Drainage Invariant
    }

    @Inject
    public MottattDokumentPersistertPubliserer(Event<MottattDokumentPersistertEvent> mottattDokumentPersistertEvent) {
        this.mottattDokumentPersistertEvent = mottattDokumentPersistertEvent;
    }

    public void fireEvent(MottattDokument mottattDokument, Behandling behandling) {

        var event = new MottattDokumentPersistertEvent(mottattDokument, behandling);
        mottattDokumentPersistertEvent.fire(event);
    }
}
