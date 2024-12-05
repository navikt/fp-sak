package no.nav.foreldrepenger.mottak.hendelser;

import java.util.List;
import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.hendelser.ForretningshendelseType;
import no.nav.foreldrepenger.behandlingslager.task.FagsakProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(value = "hendelser.håndterHendelsePåFagsak", prioritet = 2)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class MottaHendelseFagsakTask extends FagsakProsessTask {

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
    protected void prosesser(ProsessTaskData prosessTaskData, Long fagsakId) {
        var hendelseTypeKode = prosessTaskData.getPropertyValue(PROPERTY_HENDELSE_TYPE);
        var årsakTypeKode = prosessTaskData.getPropertyValue(PROPERTY_ÅRSAK_TYPE);
        Objects.requireNonNull(fagsakId);
        Objects.requireNonNull(hendelseTypeKode);
        Objects.requireNonNull(årsakTypeKode);

        var hendelseType = ForretningshendelseType.fraKode(hendelseTypeKode);
        var behandlingÅrsakType = BehandlingÅrsakType.fraKode(årsakTypeKode);

        forretningshendelseMottak.håndterHendelsePåFagsak(fagsakId, hendelseType, behandlingÅrsakType);
    }

    @Override
    protected List<Long> identifiserBehandling(ProsessTaskData prosessTaskData) {
        return behandlingRepository.hentÅpneBehandlingerIdForFagsakId(prosessTaskData.getFagsakId());
    }
}
