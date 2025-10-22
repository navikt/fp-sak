package no.nav.foreldrepenger.domene.registerinnhenting.task;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.BehandlingProsessTask;
import no.nav.foreldrepenger.domene.registerinnhenting.RegisterdataInnhenter;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask("innhentsaksopplysninger.personopplysninger")
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class InnhentPersonopplysningerTask extends BehandlingProsessTask {

    private static final Logger LOG = LoggerFactory.getLogger(InnhentPersonopplysningerTask.class);
    private BehandlingRepository behandlingRepository;
    private RegisterdataInnhenter registerdataInnhenter;

    InnhentPersonopplysningerTask() {
        // for CDI proxy
    }

    @Inject
    public InnhentPersonopplysningerTask(BehandlingRepository behandlingRepository,
                                         BehandlingLåsRepository låsRepository,
                                         RegisterdataInnhenter registerdataInnhenter) {
        super(låsRepository);
        this.behandlingRepository = behandlingRepository;
        this.registerdataInnhenter = registerdataInnhenter;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData, Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        LOG.info("Innhenter personopplysninger for behandling: {}", behandling.getId());
        registerdataInnhenter.innhentPersonopplysninger(behandling);
    }
}
