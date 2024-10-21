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
    private DokumentStatistikkRepository dokumentStatistikkRepository;

    public StatistikkMetrikkTask() {
        // CDI
    }

    @Inject
    public StatistikkMetrikkTask(BehandlingStatistikkRepository statistikkRepository,
                                 DokumentStatistikkRepository dokumentStatistikkRepository) {
        this.statistikkRepository = statistikkRepository;
        this.dokumentStatistikkRepository = dokumentStatistikkRepository;
    }


    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var behandlingerPerÅrsak = statistikkRepository.hentAntallBehandlingsårsaker();
        for (var behandling: behandlingerPerÅrsak) {
            BehandlingMetrikker.setAntall(behandling.behandlingsårsak(), behandling.antall());
        }
        var dokumentPerType = dokumentStatistikkRepository.hentAntallDokumenttyper();
        for (var dokumenttype: dokumentPerType) {
            DokumentMetrikker.setAntall(dokumenttype.dokumenttype(), dokumenttype.antall());
        }
    }
}
