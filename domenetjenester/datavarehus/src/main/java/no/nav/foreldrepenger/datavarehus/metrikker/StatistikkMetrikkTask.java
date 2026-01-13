package no.nav.foreldrepenger.datavarehus.metrikker;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(value = "statistikk.metrikker", cronExpression = "0 */15 * * * *", maxFailedRuns = 1)
public class StatistikkMetrikkTask implements ProsessTaskHandler {

    private BehandlingStatistikkRepository behandlingStatistikkRepository;
    private DokumentStatistikkRepository dokumentStatistikkRepository;
    private VedtakStatistikkRepository vedtakStatistikkRepository;

    public StatistikkMetrikkTask() {
        // CDI
    }

    @Inject
    public StatistikkMetrikkTask(BehandlingStatistikkRepository behandlingStatistikkRepository,
                                 DokumentStatistikkRepository dokumentStatistikkRepository,
                                 VedtakStatistikkRepository vedtakStatistikkRepository) {
        this.behandlingStatistikkRepository = behandlingStatistikkRepository;
        this.dokumentStatistikkRepository = dokumentStatistikkRepository;
        this.vedtakStatistikkRepository = vedtakStatistikkRepository;
    }


    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var behandlingerPerÅrsak = behandlingStatistikkRepository.hentAntallBehandlingsårsaker();
        for (var behandling: behandlingerPerÅrsak) {
            BehandlingMetrikker.setAntall(behandling.behandlingsårsak(), behandling.antall());
        }
        var dokumentPerType = dokumentStatistikkRepository.hentAntallDokumenttyper();
        for (var dokumenttype: dokumentPerType) {
            DokumentMetrikker.setAntall(dokumenttype.dokumenttype(), dokumenttype.antall());
        }
        var vedtakPerType = vedtakStatistikkRepository.hent();
        for (var vedtakType: vedtakPerType) {
            VedtakMetrikker.setAntall(vedtakType.vedtakResultatType(), vedtakType.antall());
        }
    }
}
