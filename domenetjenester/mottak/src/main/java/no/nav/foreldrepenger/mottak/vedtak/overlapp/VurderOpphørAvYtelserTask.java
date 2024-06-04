package no.nav.foreldrepenger.mottak.vedtak.overlapp;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.GenerellProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(value = "iverksetteVedtak.vurderOpphørAvYtelser", prioritet = 2, maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class VurderOpphørAvYtelserTask extends GenerellProsessTask {

    private LoggOverlappEksterneYtelserTjeneste overlappsLoggerTjeneste;
    private BehandlingRepository behandlingRepository;

    private VurderOpphørAvYtelser vurderOpphørAvYtelser;

    @Inject
    public VurderOpphørAvYtelserTask(VurderOpphørAvYtelser vurderOpphørAvYtelser,
                                     LoggOverlappEksterneYtelserTjeneste overlappsLoggerTjeneste,
                                     BehandlingRepositoryProvider repositoryProvider) {
        super();
        this.vurderOpphørAvYtelser = vurderOpphørAvYtelser;
        this.overlappsLoggerTjeneste = overlappsLoggerTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
    }

    VurderOpphørAvYtelserTask() {
        // for CDI proxy
    }

    @Override
    public void prosesser(ProsessTaskData prosessTaskData, Long fagsakId, Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        //kjøres for førstegangsvedtak og revurderingsvedtak for fp og SVP
        overlappsLoggerTjeneste.loggOverlappForVedtakFPSAK(behandling);

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(behandling);

    }

}
