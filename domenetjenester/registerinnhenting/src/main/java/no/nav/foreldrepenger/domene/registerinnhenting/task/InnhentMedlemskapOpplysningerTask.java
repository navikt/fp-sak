package no.nav.foreldrepenger.domene.registerinnhenting.task;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.BehandlingProsessTask;
import no.nav.foreldrepenger.domene.registerinnhenting.RegisterdataInnhenter;
import no.nav.foreldrepenger.domene.registerinnhenting.ufo.UføreInnhenter;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask("innhentsaksopplysninger.medlemskap")
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class InnhentMedlemskapOpplysningerTask extends BehandlingProsessTask {

    private static final Logger LOG = LoggerFactory.getLogger(InnhentMedlemskapOpplysningerTask.class);
    private BehandlingRepository behandlingRepository;
    private RegisterdataInnhenter registerdataInnhenter;
    private UføreInnhenter uføreInnhenter;

    InnhentMedlemskapOpplysningerTask() {
        // for CDI proxy
    }

    @Inject
    public InnhentMedlemskapOpplysningerTask(BehandlingRepositoryProvider behandlingRepositoryProvider,
                                             RegisterdataInnhenter registerdataInnhenter,
                                             UføreInnhenter uføreInnhenter) {
        super(behandlingRepositoryProvider.getBehandlingLåsRepository());
        this.behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository();
        this.registerdataInnhenter = registerdataInnhenter;
        this.uføreInnhenter = uføreInnhenter;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData, Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        LOG.info("Innhenter medlemskapsopplysninger for behandling: {}", behandling.getId());
        registerdataInnhenter.innhentMedlemskapsOpplysning(behandling);
        try {
            uføreInnhenter.innhentUføretrygd(behandling);
        } catch (Exception e) {
            // Nevermind
            LOG.info("Innhent UFO: noe gikk galt ", e);
        }

    }
}
