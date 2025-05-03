package no.nav.foreldrepenger.behandlingskontroll.impl.observer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingskontroll.events.BehandlingStatusEvent;

@ApplicationScoped
public class BehandlingStatusEventLogger {
    private static final Logger LOG = LoggerFactory.getLogger(BehandlingStatusEventLogger.class);

    BehandlingStatusEventLogger() {
        // for CDI
    }

    public void loggBehandlingStatusEndring(@Observes BehandlingStatusEvent event) {
        var behandlingId = event.getBehandlingId();
        var saksnummer = event.getSaksnummer();

        var nyStatus = event.getNyStatus();
        var kode = nyStatus == null ? null : nyStatus.getKode();
        LOG.info("Behandling status oppdatert; behandlingId [{}]; saksnummer [{}]; status [{}]]", behandlingId, saksnummer.getVerdi(), kode);
    }
}
