package no.nav.foreldrepenger.datavarehus.observer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.events.AutopunktStatusEvent;
import no.nav.foreldrepenger.behandlingskontroll.events.BehandlingStatusEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.events.BehandlingEnhetEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.events.BehandlingRelasjonEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.events.BehandlingSaksbehandlerEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.events.BehandlingVedtakEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.events.KlageOppdatertEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.events.MottattDokumentPersistertEvent;
import no.nav.foreldrepenger.datavarehus.tjeneste.DatavarehusTjeneste;

@ApplicationScoped
public class DatavarehusEventObserver {

    private DatavarehusTjeneste tjeneste;

    public DatavarehusEventObserver() {
        //Cool Devices Installed
    }

    @Inject
    public DatavarehusEventObserver(DatavarehusTjeneste datavarehusTjeneste) {
        this.tjeneste = datavarehusTjeneste;
    }

    public void observerAutopunktStatusEvent(@Observes AutopunktStatusEvent event) {
        if (!event.getAksjonspunkter().isEmpty()) {
            tjeneste.lagreNedBehandling(event.getBehandlingId());
        }
    }

    public void observerKlageOppdatertEvent(@Observes KlageOppdatertEvent event) {
        tjeneste.lagreNedBehandling(event.getBehandlingId());
    }

    public void observerBehandlingEnhetEvent(@Observes BehandlingEnhetEvent event) {
        tjeneste.lagreNedBehandling(event.getBehandlingId());
    }

    public void observerBehandlingSaksbehandlerEvent(@Observes BehandlingSaksbehandlerEvent event) {
        tjeneste.lagreNedBehandling(event.getBehandlingId());
    }

    public void observerBehandlingRelasjonEvent(@Observes BehandlingRelasjonEvent event) {
        tjeneste.lagreNedBehandling(event.getBehandlingId());
    }

    public void observerBehandlingStatusEvent(@Observes BehandlingStatusEvent event) {
        tjeneste.lagreNedBehandling(event.getBehandlingId());
    }

    public void observerMottattDokumentPersistert(@Observes MottattDokumentPersistertEvent event) {
        // Lagre behandling med rettighetsdato o.l.
        if (event.getMottattDokument().getDokumentType().erSøknadType() || event.getMottattDokument().getDokumentType().erEndringsSøknadType()) {
            tjeneste.lagreNedBehandling(event.getBehandlingId());
        }
    }

    public void observerBehandlingVedtakEvent(@Observes BehandlingVedtakEvent event) {
        if (event.iverksattVedtak()) {
            tjeneste.lagreNedBehandling(event.behandling(), event.vedtak());
        }
    }

}
