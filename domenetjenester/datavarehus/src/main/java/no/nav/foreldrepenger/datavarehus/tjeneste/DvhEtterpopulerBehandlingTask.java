package no.nav.foreldrepenger.datavarehus.tjeneste;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.GenerellProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@Dependent
@ProsessTask(value = "iverksetteVedtak.repopulerdvhbehandling", maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class DvhEtterpopulerBehandlingTask extends GenerellProsessTask {

    private static final Logger LOG = LoggerFactory.getLogger(DvhEtterpopulerBehandlingTask.class);
    public static final String LOG_HENDELSE_KEY = "hendelse";


    private DatavarehusTjenesteImpl datavarehusTjeneste;
    private BehandlingRepository behandlingRepository;

    DvhEtterpopulerBehandlingTask() {
        // for CDI proxy
    }

    @Inject
    public DvhEtterpopulerBehandlingTask(DatavarehusTjenesteImpl datavarehusTjeneste, BehandlingRepository behandlingRepository) {
        super();
        this.datavarehusTjeneste = datavarehusTjeneste;
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public void prosesser(ProsessTaskData prosessTaskData, Long fagsakId, Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var funksjonellTid = behandling.getÅpneAksjonspunkter(AksjonspunktType.AUTOPUNKT).stream()
            .min(Comparator.comparing(Aksjonspunkt::getOpprettetTidspunkt))
            .map(ap -> Optional.ofNullable(ap.getEndretTidspunkt()).orElseGet(ap::getOpprettetTidspunkt))
            .orElseGet(LocalDateTime::now);
        datavarehusTjeneste.lagreNedBehandlingHistorisk(behandling, funksjonellTid);
    }


}
