package no.nav.foreldrepenger.produksjonsstyring.behandlinghendelse;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingskontroll.events.BehandlingStatusEvent;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@ApplicationScoped
public class BehandlingAvsluttetEventObserver {

    private static final Logger LOG = LoggerFactory.getLogger(BehandlingAvsluttetEventObserver.class);

    private ProsessTaskTjeneste taskTjeneste;

    public BehandlingAvsluttetEventObserver() {
    }

    @Inject
    public BehandlingAvsluttetEventObserver(ProsessTaskTjeneste taskTjeneste) {
        this.taskTjeneste = taskTjeneste;
    }

    public void observerBehandlingAvsluttetEvent(@Observes BehandlingStatusEvent.BehandlingAvsluttetEvent event) {
        LOG.info("Observerte hendelse {} for behandling {}. Lager task for å avslutte eksterne koblinger", event.getNyStatus(), event.getBehandlingId());
        var prosessTaskData = ProsessTaskData.forProsessTask(AvsluttEksterneGrunnlagTask.class);
        prosessTaskData.setBehandling(event.getSaksnummer().getVerdi(), event.getFagsakId(), event.getBehandlingId());
        taskTjeneste.lagre(prosessTaskData);
    }
}
