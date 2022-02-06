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
import no.nav.foreldrepenger.domene.registerinnhenting.StønadsperioderInnhenter;
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
    private StønadsperioderInnhenter stønadsperioderInnhenter;

    InnhentMedlemskapOpplysningerTask() {
        // for CDI proxy
    }

    @Inject
    public InnhentMedlemskapOpplysningerTask(BehandlingRepositoryProvider behandlingRepositoryProvider,
                                             RegisterdataInnhenter registerdataInnhenter,
                                             UføreInnhenter uføreInnhenter,
                                             StønadsperioderInnhenter stønadsperioderInnhenter) {
        super(behandlingRepositoryProvider.getBehandlingLåsRepository());
        this.behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository();
        this.registerdataInnhenter = registerdataInnhenter;
        this.uføreInnhenter = uføreInnhenter;
        this.stønadsperioderInnhenter = stønadsperioderInnhenter;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData, Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        LOG.info("Innhenter medlemskapsopplysninger for behandling: {}", behandling.getId());
        try {
            stønadsperioderInnhenter.finnSenereStønadsperioderLoggResultat(behandling);
        } catch (Exception e) {
            LOG.info("NESTEBARN noe gikk galt", e);
        }
        registerdataInnhenter.innhentMedlemskapsOpplysning(behandling);
        uføreInnhenter.innhentUføretrygd(behandling);
    }
}
