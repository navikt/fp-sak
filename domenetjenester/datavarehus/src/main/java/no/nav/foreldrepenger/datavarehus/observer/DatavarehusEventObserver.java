package no.nav.foreldrepenger.datavarehus.observer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.events.AksjonspunktStatusEvent;
import no.nav.foreldrepenger.behandlingskontroll.events.BehandlingStatusEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.events.BehandlingEnhetEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.events.BehandlingRelasjonEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.events.BehandlingSaksbehandlerEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.events.BehandlingVedtakEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.events.MottattDokumentPersistertEvent;
import no.nav.foreldrepenger.datavarehus.tjeneste.DatavarehusTjeneste;
import no.nav.foreldrepenger.konfig.Environment;

@ApplicationScoped
public class DatavarehusEventObserver {

    private static final boolean TESTENV = Environment.current().isLocal();

    private DatavarehusTjeneste tjeneste;
    private boolean testObserver;

    public DatavarehusEventObserver() {
        //Cool Devices Installed
    }

    @Inject
    public DatavarehusEventObserver(DatavarehusTjeneste datavarehusTjeneste) {
        this(datavarehusTjeneste, false);
    }

    public DatavarehusEventObserver(DatavarehusTjeneste datavarehusTjeneste, boolean testObserver) {
        this.tjeneste = datavarehusTjeneste;
        this.testObserver = testObserver;
    }

    public void observerAksjonspunktStatusEvent(@Observes AksjonspunktStatusEvent event) {
        var aksjonspunkter = event.getAksjonspunkter();
        // Utvider behandlingStatus i DVH med VenteKategori
        if (aksjonspunkter.stream().anyMatch(a -> a.erAutopunkt() || (a.erUtført() && gjelderKlage(a)))) {
            lagreNedBehandling(event.getBehandlingId());
        }
    }

    private static boolean gjelderKlage(Aksjonspunkt a) {
        return AksjonspunktDefinisjon.VURDERING_AV_FORMKRAV_KLAGE_NFP.equals(a.getAksjonspunktDefinisjon()) ||
            AksjonspunktDefinisjon.MANUELL_VURDERING_AV_KLAGE_NFP.equals(a.getAksjonspunktDefinisjon());
    }

    public void observerBehandlingEnhetEvent(@Observes BehandlingEnhetEvent event) {
        lagreNedBehandling(event.getBehandlingId());
    }

    public void observerBehandlingSaksbehandlerEvent(@Observes BehandlingSaksbehandlerEvent event) {
        lagreNedBehandling(event.getBehandlingId());
    }

    public void observerBehandlingRelasjonEvent(@Observes BehandlingRelasjonEvent event) {
        lagreNedBehandling(event.getBehandlingId());
    }

    public void observerBehandlingStatusEvent(@Observes BehandlingStatusEvent event) {
        lagreNedBehandling(event.getBehandlingId());
    }

    public void observerMottattDokumentPersistert(@Observes MottattDokumentPersistertEvent event) {
        // Lagre behandling med rettighetsdato o.l.
        if (event.getMottattDokument().getDokumentType().erSøknadType() || event.getMottattDokument().getDokumentType().erEndringsSøknadType()) {
            lagreNedBehandling(event.getBehandlingId());
        }
    }

    public void observerBehandlingVedtakEvent(@Observes BehandlingVedtakEvent event) {
        if (event.iverksattVedtak() && skalKalleDatavarehusTjeneste()) {
            tjeneste.lagreNedBehandling(event.behandling(), event.vedtak());
        }
    }

    private void lagreNedBehandling(Long behandlingId) {
        if (skalKalleDatavarehusTjeneste()) {
            tjeneste.lagreNedBehandling(behandlingId);
        }
    }

    private boolean skalKalleDatavarehusTjeneste() {
        return testObserver || !TESTENV;
    }

}
