package no.nav.foreldrepenger.mottak.hendelser;

import java.util.List;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.FagsakProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(MottaHendelseFagsakTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class MottaHendelseFagsakTask extends FagsakProsessTask {

    public static final String TASKTYPE = "hendelser.håndterHendelsePåFagsak";
    static final String PROPERTY_HENDELSE_TYPE = "hendelseType";
    static final String PROPERTY_ÅRSAK_TYPE = "aarsakType";

    private ForretningshendelseMottak forretningshendelseMottak;
    private BehandlingRepository behandlingRepository;

    MottaHendelseFagsakTask() {
        // for CDI proxy
    }

    @Inject
    public MottaHendelseFagsakTask(ForretningshendelseMottak forretningshendelseMottak, BehandlingRepositoryProvider repositoryProvider) {
        super(repositoryProvider.getFagsakLåsRepository(), repositoryProvider.getBehandlingLåsRepository());
        this.forretningshendelseMottak = forretningshendelseMottak;
        this.   behandlingRepository = repositoryProvider.getBehandlingRepository();
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData) {
        Long fagsakId = prosessTaskData.getFagsakId();
        String hendelseTypeKode = prosessTaskData.getPropertyValue(PROPERTY_HENDELSE_TYPE);
        String årsakTypeKode = prosessTaskData.getPropertyValue(PROPERTY_ÅRSAK_TYPE);
        Objects.requireNonNull(fagsakId);
        Objects.requireNonNull(hendelseTypeKode);
        Objects.requireNonNull(årsakTypeKode);

        forretningshendelseMottak.håndterHendelsePåFagsak(fagsakId, hendelseTypeKode, årsakTypeKode);
    }

    @Override
    protected List<Long> identifiserBehandling(ProsessTaskData prosessTaskData) {
        return behandlingRepository.hentÅpneBehandlingerIdForFagsakId(prosessTaskData.getFagsakId());
    }
}
