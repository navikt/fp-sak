package no.nav.foreldrepenger.domene.registerinnhenting.task;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.domene.registerinnhenting.RegisterdataInnhenter;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(InnhentIAYIAbakusTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class InnhentIAYIAbakusTask implements ProsessTaskHandler {

    public static final String TASKTYPE = "innhentsaksopplysninger.abakus";
    public static final String OVERSTYR_KEY = "overstyrt";
    public static final String OVERSTYR_VALUE = "overstyrt";

    private static final Logger LOGGER = LoggerFactory.getLogger(InnhentIAYIAbakusTask.class);

    private BehandlingRepository behandlingRepository;
    private RegisterdataInnhenter registerdataInnhenter;

    InnhentIAYIAbakusTask() {
        // for CDI proxy
    }

    @Inject
    public InnhentIAYIAbakusTask(BehandlingRepository behandlingRepository,
                                 RegisterdataInnhenter registerdataInnhenter) {
        this.behandlingRepository = behandlingRepository;
        this.registerdataInnhenter = registerdataInnhenter;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        boolean overstyr = prosessTaskData.getPropertyValue(OVERSTYR_KEY) != null && OVERSTYR_VALUE.equals(prosessTaskData.getPropertyValue(OVERSTYR_KEY));
        Behandling behandling = behandlingRepository.hentBehandling(prosessTaskData.getBehandlingId());
        LOGGER.info("Innhenter IAY-opplysninger i abakus for behandling: {}", behandling.getId());
        if (overstyr) {
            registerdataInnhenter.innhentFullIAYIAbakus(behandling);
            return;
        }
        registerdataInnhenter.innhentIAYIAbakus(behandling);
    }
}
