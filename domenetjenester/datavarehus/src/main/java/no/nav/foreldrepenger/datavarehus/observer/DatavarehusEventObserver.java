package no.nav.foreldrepenger.datavarehus.observer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingskontroll.events.AksjonspunktStatusEvent;
import no.nav.foreldrepenger.behandlingskontroll.events.AutopunktStatusEvent;
import no.nav.foreldrepenger.behandlingskontroll.events.BehandlingStatusEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.events.BehandlingEnhetEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.events.BehandlingRelasjonEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.events.BehandlingSaksbehandlerEvent;
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
        if (aksjonspunkter.stream().anyMatch(a -> a.erUtført() && gjelderKlage(a))) {
            tjeneste.lagreNedBehandling(event.getBehandlingId());
        }
    }

    public void observerAutopunktStatusEvent(@Observes AutopunktStatusEvent event) {
        if (!event.getAksjonspunkter().isEmpty()) {
            tjeneste.lagreNedBehandling(event.getBehandlingId());
        }
    }

    private static boolean gjelderKlage(Aksjonspunkt a) {
        return AksjonspunktDefinisjon.VURDERING_AV_FORMKRAV_KLAGE_NFP.equals(a.getAksjonspunktDefinisjon()) ||
            AksjonspunktDefinisjon.MANUELL_VURDERING_AV_KLAGE_NFP.equals(a.getAksjonspunktDefinisjon());
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
