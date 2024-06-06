package no.nav.foreldrepenger.datavarehus.observer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingskontroll.events.AksjonspunktStatusEvent;
import no.nav.foreldrepenger.behandlingskontroll.events.BehandlingStatusEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.events.BehandlingEnhetEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.events.BehandlingRelasjonEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.events.BehandlingVedtakEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.events.MottattDokumentPersistertEvent;
import no.nav.foreldrepenger.datavarehus.tjeneste.DatavarehusTjeneste;

@ApplicationScoped
public class DatavarehusEventObserver {
    protected final Logger LOG = LoggerFactory.getLogger(this.getClass());

    private DatavarehusTjeneste tjeneste;

    public DatavarehusEventObserver() {
        //Cool Devices Installed
    }

    @Inject
    public DatavarehusEventObserver(DatavarehusTjeneste datavarehusTjeneste) {
        this.tjeneste = datavarehusTjeneste;
    }

    public void observerAksjonspunktStatusEvent(@Observes AksjonspunktStatusEvent event) {
        var aksjonspunkter = event.getAksjonspunkter();
        // Utvider behandlingStatus i DVH med VenteKategori
        if (aksjonspunkter.stream().anyMatch(Aksjonspunkt::erAutopunkt)) {
            tjeneste.lagreNedBehandling(event.getBehandlingId());
        }
        tjeneste.oppdaterHvisKlageEllerAnke(event.getBehandlingId(), aksjonspunkter);
    }

    public void observerBehandlingEnhetEvent(@Observes BehandlingEnhetEvent event) {
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
            tjeneste.lagreNedVedtak(event.vedtak(), event.behandling());
        }
    }

}
