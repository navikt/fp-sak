package no.nav.foreldrepenger.mottak.vedtak.overlapp;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.task.GenerellProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(value = "iverksetteVedtak.håndterOverlappPleiepenger", maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class HåndterOverlappPleiepengerTask extends GenerellProsessTask {

    private HåndterOpphørAvYtelser tjeneste;
    private FagsakRepository fagsakRepository;

    @Inject
    public HåndterOverlappPleiepengerTask(HåndterOpphørAvYtelser tjeneste, FagsakRepository fagsakRepository) {
        super();
        this.tjeneste = tjeneste;
        this.fagsakRepository = fagsakRepository;
    }

    HåndterOverlappPleiepengerTask() {
        // for CDI proxy
    }

    @Override
    public void prosesser(ProsessTaskData prosessTaskData, Long fagsakId, Long behandlingId) {
        var fagsak = fagsakRepository.finnEksaktFagsak(fagsakId);
        var behandlingÅrsak = BehandlingÅrsakType.RE_VEDTAK_PLEIEPENGER;
        tjeneste.oppdaterEllerOpprettRevurdering(fagsak, null, behandlingÅrsak);
    }
}
