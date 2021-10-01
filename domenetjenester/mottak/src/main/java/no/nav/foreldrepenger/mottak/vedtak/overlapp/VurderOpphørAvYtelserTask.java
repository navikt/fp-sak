package no.nav.foreldrepenger.mottak.vedtak.overlapp;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.task.GenerellProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(value = "iverksetteVedtak.vurderOpphørAvYtelser", maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class VurderOpphørAvYtelserTask extends GenerellProsessTask {

    private LoggOverlappEksterneYtelserTjeneste overlappsLoggerTjeneste;
    private BehandlingRepository behandlingRepository;
    private FagsakRepository fagsakRepository;

    private VurderOpphørAvYtelser vurderOpphørAvYtelser;

    VurderOpphørAvYtelserTask() {
        // for CDI proxy
    }

    @Inject
    public VurderOpphørAvYtelserTask(VurderOpphørAvYtelser vurderOpphørAvYtelser,
                                     LoggOverlappEksterneYtelserTjeneste overlappsLoggerTjeneste,
                                     BehandlingRepositoryProvider repositoryProvider) {
        super();
        this.vurderOpphørAvYtelser = vurderOpphørAvYtelser;
        this.overlappsLoggerTjeneste = overlappsLoggerTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
    }

    @Override
    public void prosesser(ProsessTaskData prosessTaskData, Long fagsakId, Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        //kjøres for førstegangsvedtak og revurderingsvedtak for fp og SVP
        overlappsLoggerTjeneste.loggOverlappForVedtakFPSAK(behandlingId, behandling.getFagsak().getSaksnummer(), behandling.getAktørId());
        //kjøres kun for førstegangsvedtak for svp og fp
        if (!behandling.erRevurdering()) {
            vurderOpphørAvYtelser.vurderOpphørAvYtelser(prosessTaskData.getFagsakId(), behandlingId);
        }
    }

}
