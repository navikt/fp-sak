package no.nav.foreldrepenger.mottak.publiserer.publish;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokumentPersistertEvent;

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

        MottattDokumentPersistertEvent event = new MottattDokumentPersistertEvent(mottattDokument, behandling);
        mottattDokumentPersistertEvent.fire(event);
    }
}
