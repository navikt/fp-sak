package no.nav.foreldrepenger.datavarehus.metrikker;

import static no.nav.vedtak.log.metrics.MetricsUtil.REGISTRY;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.micrometer.core.instrument.Tag;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(value = "statistikk.metrikker", cronExpression = "0 */15 * * * *", maxFailedRuns = 1)
public class StatistikkMetrikkTask implements ProsessTaskHandler {

    private static final String BEHANDLING_METRIKK_NAVN = "behandlinger.antall";

    private BehandlingStatistikkRepository statistikkRepository;

    public StatistikkMetrikkTask() {
        // CDI
    }

    @Inject
    public StatistikkMetrikkTask(BehandlingStatistikkRepository statistikkRepository) {
        this.statistikkRepository = statistikkRepository;
    }


    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var behandlingerPerTypeYtelseÅrsak = statistikkRepository.hentAntallBehandlinger();

        for (var behandling: behandlingerPerTypeYtelseÅrsak) {
            REGISTRY.gauge(BEHANDLING_METRIKK_NAVN, tilTags(behandling), behandling.antall());
        }
    }

    private List<Tag> tilTags(BehandlingStatistikkRepository.BehandlingStatistikk behandlingStatistikkEntitet) {
        var tags = new ArrayList<Tag>();
        if (behandlingStatistikkEntitet.ytelseType() != null) {
            tags.add(Tag.of("ytelse_type", behandlingStatistikkEntitet.ytelseType().getNavn()));
        }
        if (behandlingStatistikkEntitet.behandlingType() != null) {
            tags.add(Tag.of("behandling_type", behandlingStatistikkEntitet.behandlingType().getNavn()));
        }
        if (behandlingStatistikkEntitet.behandlingsårsak() != null) {
            tags.add(Tag.of("behandling_aarsak_type", behandlingStatistikkEntitet.behandlingsårsak().name()));
        }
        return tags;
    }
}
