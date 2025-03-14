package no.nav.foreldrepenger.domene.registerinnhenting.task;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.BehandlingProsessTask;
import no.nav.foreldrepenger.domene.registerinnhenting.MorsAktivitetInnhenter;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask("innhentsaksopplysninger.morsAktivitet")
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class InnhentMorsAktivitetTask extends BehandlingProsessTask {
    private static final Logger LOG = LoggerFactory.getLogger(InnhentMorsAktivitetTask.class);
    private BehandlingRepository behandlingRepository;
    private MorsAktivitetInnhenter morsAktivitetInnhenter;

    InnhentMorsAktivitetTask() {
        //CDI
    }

    @Inject
    public InnhentMorsAktivitetTask(BehandlingLåsRepository behandlingLåsRepository,
                                    BehandlingRepository behandlingRepository,
                                    MorsAktivitetInnhenter morsAktivitetInnhenter) {
        super(behandlingLåsRepository);
        this.behandlingRepository = behandlingRepository;
        this.morsAktivitetInnhenter = morsAktivitetInnhenter;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData, Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        LOG.info("Innhenter mors aktivitet for behandling: {}", behandling.getId());
        morsAktivitetInnhenter.innhentMorsAktivitet(behandling);
    }
}
