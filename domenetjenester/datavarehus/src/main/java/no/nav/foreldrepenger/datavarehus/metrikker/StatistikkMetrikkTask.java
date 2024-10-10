package no.nav.foreldrepenger.datavarehus.metrikker;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(value = "statistikk.metrikker", cronExpression = "0 */15 * * * *", maxFailedRuns = 1)
public class StatistikkMetrikkTask implements ProsessTaskHandler {

    private BehandlingStatistikkRepository statistikkRepository;
    private BehandlingMetrikker behandlingMetrikker;

    public StatistikkMetrikkTask() {
        // CDI
    }

    @Inject
    public StatistikkMetrikkTask(BehandlingStatistikkRepository statistikkRepository, BehandlingMetrikker behandlingMetrikker) {
        this.statistikkRepository = statistikkRepository;
        this.behandlingMetrikker = behandlingMetrikker;
        hentBehandlingStatistikkOgOppdaterMetrikker();
    }


    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        hentBehandlingStatistikkOgOppdaterMetrikker();
    }

    private void hentBehandlingStatistikkOgOppdaterMetrikker() {
        statistikkRepository.hentAntallBehandlinger()
            .forEach(behandlingStatistikk -> behandlingMetrikker.setAntall(behandlingStatistikk.type(), behandlingStatistikk.antall()));
    }
}
