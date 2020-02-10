package no.nav.foreldrepenger.behandling.kriterie;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

@ApplicationScoped
public class BehandlingsfristUtløptTjeneste {

    public static final String FORLENGELSESBREV_TASK = "behandlingsstotte.sendForlengelsesbrev";

    private ProsessTaskRepository prosessTaskRepository;

    BehandlingsfristUtløptTjeneste() {
        // for CDI proxy
    }
    
    @Inject
    public BehandlingsfristUtløptTjeneste(ProsessTaskRepository prosessTaskRepository) {
        this.prosessTaskRepository = prosessTaskRepository;
    }

    public void behandlingsfristUtløpt(Behandling behandling) {
        ProsessTaskData prosessTaskData = new ProsessTaskData(FORLENGELSESBREV_TASK);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        prosessTaskData.setCallIdFraEksisterende();
        prosessTaskRepository.lagre(prosessTaskData);
    }
}
