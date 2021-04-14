package no.nav.foreldrepenger.dokumentbestiller.forlengelsesbrev.task;

import java.time.LocalDate;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.BehandlingProsessTask;
import no.nav.foreldrepenger.dokumentbestiller.forlengelsesbrev.SendForlengelsesbrevTaskProperties;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(SendForlengelsesbrevTaskProperties.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class SendForlengelsesbrevTask extends BehandlingProsessTask {

    private static final Logger LOG = LoggerFactory.getLogger(SendForlengelsesbrevTask.class);

    private BehandlingRepository behandlingRepository;

    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;

    SendForlengelsesbrevTask() {
        // for CDI proxy
    }

    @Inject
    public SendForlengelsesbrevTask(BehandlingRepositoryProvider behandlingRepositoryProvider,
                                    BehandlingskontrollTjeneste behandlingskontrollTjeneste) {
        super(behandlingRepositoryProvider.getBehandlingLåsRepository());
        this.behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository();
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData, Long behandlingId) {
        var kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandlingId);
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        if (behandlingsfristUtløpt(behandling)) {
            sendForlengelsesbrevOgOppdaterBehandling(behandling, kontekst);
            LOG.info("Utført for behandling: {}", behandlingId);
        } else {
            LOG.info("Ikke utført for behandling: {}, behandlingsfrist ikke utløpt", behandlingId);
        }
    }

    private void sendForlengelsesbrevOgOppdaterBehandling(Behandling behandling, BehandlingskontrollKontekst kontekst) {
        behandling.setBehandlingstidFrist(LocalDate.now().plusWeeks(behandling.getType().getBehandlingstidFristUker()));
        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
        LOG.info("Brev ikke sendt for behandling: {}, automatisk utsendelse slått av for alle", behandling.getId());
        // Pr mars 2020 er det ikke ønsket å sende ut forengelsesbrev da det tidligere skapte for mye støy.
        // Dersom det skal gjeninnføres må brevet bestilles via fp-formidling.
    }

    private boolean behandlingsfristUtløpt(Behandling behandling) {
        return LocalDate.now().isAfter(behandling.getBehandlingstidFrist());
    }
}
