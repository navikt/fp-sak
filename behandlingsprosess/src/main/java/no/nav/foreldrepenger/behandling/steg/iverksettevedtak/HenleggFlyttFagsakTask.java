package no.nav.foreldrepenger.behandling.steg.iverksettevedtak;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.FagsakProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;


@ApplicationScoped
@ProsessTask(HenleggFlyttFagsakTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class HenleggFlyttFagsakTask extends FagsakProsessTask {

    public static final String TASKTYPE = "behandlingskontroll.henleggBehandling";

    public static final String HENLEGGELSE_TYPE_KEY = "henleggesGrunn";

    private HenleggBehandlingTjeneste henleggBehandlingTjeneste;


    HenleggFlyttFagsakTask() {
        // for CDI proxy
    }

    @Inject
    public HenleggFlyttFagsakTask(BehandlingRepositoryProvider repositoryProvider,
                                  HenleggBehandlingTjeneste henleggBehandlingTjeneste) {
        super(repositoryProvider.getFagsakLåsRepository(), repositoryProvider.getBehandlingLåsRepository());
        this.henleggBehandlingTjeneste = henleggBehandlingTjeneste;

    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData) {
        Long behandlingId = prosessTaskData.getBehandlingId();
        BehandlingResultatType henleggelseType = BehandlingResultatType.MANGLER_BEREGNINGSREGLER;

        if (prosessTaskData.getPropertyValue(HENLEGGELSE_TYPE_KEY) != null) {
            henleggelseType = BehandlingResultatType.fraKode(prosessTaskData.getPropertyValue(HENLEGGELSE_TYPE_KEY));
        }

        henleggBehandlingTjeneste.henleggBehandlingAvbrytAutopunkter(behandlingId, henleggelseType, "Forvaltning");
    }
}
