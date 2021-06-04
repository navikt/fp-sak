package no.nav.foreldrepenger.datavarehus.observer;

import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.FagsakRelasjonEvent;
import no.nav.foreldrepenger.behandling.FagsakStatusEvent;
import no.nav.foreldrepenger.behandling.impl.BehandlingEnhetEvent;
import no.nav.foreldrepenger.behandlingskontroll.events.AksjonspunktStatusEvent;
import no.nav.foreldrepenger.behandlingskontroll.events.BehandlingStatusEvent;
import no.nav.foreldrepenger.behandlingskontroll.events.BehandlingStegTilstandEndringEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokumentPersistertEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.IverksettingStatus;
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

    public void observerFagsakRelasjonEvent(@Observes FagsakRelasjonEvent event) {
        LOG.debug("Lagrer FagsakRelasjon i DVH {} ", event.getFagsakRelasjon().getId());//NOSONAR
        tjeneste.lagreNedFagsakRelasjon(event.getFagsakRelasjon());
    }

    public void observerAksjonspunktStatusEvent(@Observes AksjonspunktStatusEvent event) {
        var aksjonspunkter = event.getAksjonspunkter();
        LOG.debug("Lagrer {} aksjonspunkter i DVH datavarehus, for behandling {} og steg {}", aksjonspunkter.size(), event.getBehandlingId(), event.getBehandlingStegType());//NOSONAR
        tjeneste.lagreNedAksjonspunkter(aksjonspunkter, event.getBehandlingId(), event.getBehandlingStegType());
        tjeneste.oppdaterHvisKlageEllerAnke(event.getBehandlingId(), aksjonspunkter);
    }

    public void observerFagsakStatus(@Observes FagsakStatusEvent event) {
        LOG.debug("Lagrer fagsak {} i DVH mellomalger", event.getFagsakId());//NOSONAR
        tjeneste.lagreNedFagsak(event.getFagsakId());
    }

    public void observerBehandlingStegTilstandEndringEvent(@Observes BehandlingStegTilstandEndringEvent event) {
        var fraTilstand = event.getFraTilstand();
        if (fraTilstand.isPresent()) {
            var tilstand = fraTilstand.get();
            LOG.debug("Lagrer behandligsteg endring fra tilstand {} i DVH datavarehus for behandling {}; behandlingStegTilstandId {}", //NOSONAR
                tilstand.getSteg().getKode(), event.getBehandlingId(), tilstand.getId());
            tjeneste.lagreNedBehandlingStegTilstand(event.getBehandlingId(), tilstand);
        }
        var tilTilstand = event.getTilTilstand();
        if (tilTilstand.isPresent() && !Objects.equals(tilTilstand.orElse(null), fraTilstand.orElse(null))) {
            var tilstand = tilTilstand.get();
            LOG.debug("Lagrer behandligsteg endring til tilstand {} i DVH datavarehus for behandlingId {}; behandlingStegTilstandId {}", //NOSONAR
                tilstand.getSteg().getKode(), event.getBehandlingId(), tilstand.getId());
            tjeneste.lagreNedBehandlingStegTilstand(event.getBehandlingId(), tilstand);
        }
    }

    public void observerBehandlingEnhetEvent(@Observes BehandlingEnhetEvent event) {
        LOG.debug("Lagrer behandling {} i DVH datavarehus", event.getBehandlingId());//NOSONAR
        tjeneste.lagreNedBehandling(event.getBehandlingId());
    }

    public void observerBehandlingStatusEvent(@Observes BehandlingStatusEvent event) {
        LOG.debug("Lagrer behandling {} i DVH datavarehus", event.getBehandlingId());//NOSONAR
        tjeneste.lagreNedBehandling(event.getBehandlingId());
    }

    public void observerMottattDokumentPersistert(@Observes MottattDokumentPersistertEvent event) {
        // Lagre behandling med rettighetsdato o.l.
        if (event.getMottattDokument().getDokumentType().erSøknadType() || event.getMottattDokument().getDokumentType().erEndringsSøknadType()) {
            tjeneste.lagreNedBehandling(event.getBehandlingId());
        }
    }

    public void observerBehandlingVedtakEvent(@Observes BehandlingVedtakEvent event) {
        if (IverksettingStatus.IVERKSATT.equals(event.getVedtak().getIverksettingStatus())) {
            LOG.info("DVH Behandlingvedtakevent inngang behandling {}", event.getBehandlingId());
            tjeneste.lagreNedVedtak(event.getVedtak(), event.getBehandling());
            LOG.info("DVH Behandlingvedtakevent utgang behandling {}", event.getBehandlingId());
        }
    }

}
