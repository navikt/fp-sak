package no.nav.foreldrepenger.domene.registerinnhenting.task;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.BehandlingProsessTask;
import no.nav.foreldrepenger.domene.registerinnhenting.RegisterdataInnhenter;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(value = "innhentsaksopplysninger.oppdaterttidspunkt", maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class SettRegisterdataInnhentetTidspunktTask extends BehandlingProsessTask {

    private static final Logger LOG = LoggerFactory.getLogger(SettRegisterdataInnhentetTidspunktTask.class);
    private BehandlingRepository behandlingRepository;
    private RegisterdataInnhenter registerdataInnhenter;

    SettRegisterdataInnhentetTidspunktTask() {
        // for CDI proxy
    }

    @Inject
    public SettRegisterdataInnhentetTidspunktTask(BehandlingRepositoryProvider behandlingRepositoryProvider,
                                                  RegisterdataInnhenter registerdataInnhenter) {
        super(behandlingRepositoryProvider.getBehandlingLåsRepository());
        this.behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository();
        this.registerdataInnhenter = registerdataInnhenter;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData, Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        LOG.info("Oppdaterer registerdata innhentet tidspunkt behandling med id={} og uuid={}", behandling.getId(), behandling.getUuid());
        registerdataInnhenter.oppdaterSistOppdatertTidspunkt(behandling);
    }
}
