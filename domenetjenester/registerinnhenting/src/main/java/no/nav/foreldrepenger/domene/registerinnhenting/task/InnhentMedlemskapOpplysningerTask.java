package no.nav.foreldrepenger.domene.registerinnhenting.task;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.BehandlingProsessTask;
import no.nav.foreldrepenger.domene.registerinnhenting.RegisterdataInnhenter;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(InnhentMedlemskapOpplysningerTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class InnhentMedlemskapOpplysningerTask extends BehandlingProsessTask {

    public static final String TASKTYPE = "innhentsaksopplysninger.medlemskap";
    private static final Logger LOGGER = LoggerFactory.getLogger(InnhentMedlemskapOpplysningerTask.class);
    private BehandlingRepository behandlingRepository;
    private RegisterdataInnhenter registerdataInnhenter;

    InnhentMedlemskapOpplysningerTask() {
        // for CDI proxy
    }

    @Inject
    public InnhentMedlemskapOpplysningerTask(BehandlingRepositoryProvider behandlingRepositoryProvider,
                                             RegisterdataInnhenter registerdataInnhenter) {
        super(behandlingRepositoryProvider.getBehandlingLåsRepository());
        this.behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository();
        this.registerdataInnhenter = registerdataInnhenter;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData) {
        Behandling behandling = behandlingRepository.hentBehandling(prosessTaskData.getBehandlingId());
        LOGGER.info("Innhenter medlemskapsopplysninger for behandling: {}", behandling.getId());
        registerdataInnhenter.innhentMedlemskapsOpplysning(behandling);
    }
}
