package no.nav.foreldrepenger.mottak.vedtak;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(VurderOpphørAvYtelserTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class VurderOpphørAvYtelserTask implements ProsessTaskHandler {
    public static final String TASKTYPE = "iverksetteVedtak.vurderOpphørAvYtelser";
    private VurderOpphørAvYtelser tjeneste;

    VurderOpphørAvYtelserTask() {
        // for CDI proxy
    }

    @Inject
    public VurderOpphørAvYtelserTask( VurderOpphørAvYtelser tjeneste) {
        this.tjeneste = tjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        Long behandlingId = prosessTaskData.getBehandlingId();
        tjeneste.vurderOpphørAvYtelser(prosessTaskData.getFagsakId(), behandlingId);
    }
}
