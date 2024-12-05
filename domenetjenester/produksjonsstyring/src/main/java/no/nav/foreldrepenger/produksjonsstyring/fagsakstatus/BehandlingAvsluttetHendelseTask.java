package no.nav.foreldrepenger.produksjonsstyring.fagsakstatus;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.task.FagsakProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;


@ApplicationScoped
@ProsessTask(value = "behandlingskontroll.behandlingAvsluttetFagsak", prioritet = 2)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class BehandlingAvsluttetHendelseTask extends FagsakProsessTask {

    private OppdaterFagsakStatusTjeneste oppdaterFagsakStatusTjeneste;
    private FagsakRepository fagsakRepository;

    BehandlingAvsluttetHendelseTask() {
        // for CDI proxy
    }

    @Inject
    public BehandlingAvsluttetHendelseTask(BehandlingRepositoryProvider repositoryProvider,
                                           OppdaterFagsakStatusTjeneste oppdaterFagsakStatusTjeneste,
                                           FagsakRepository fagsakRepository) {
        super(repositoryProvider.getFagsakLåsRepository(), repositoryProvider.getBehandlingLåsRepository());
        this.oppdaterFagsakStatusTjeneste = oppdaterFagsakStatusTjeneste;
        this.fagsakRepository = fagsakRepository;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData, Long fagsakId) {
        // For å sikre at fagsaken hentes opp i cache - ellers dukker den opp via readonly-query og det blir problem.
        var fagsak = fagsakRepository.finnEksaktFagsak(fagsakId);

        oppdaterFagsakStatusTjeneste.oppdaterFagsakNårBehandlingAvsluttet(fagsak, prosessTaskData.getBehandlingIdAsLong());
    }
}
