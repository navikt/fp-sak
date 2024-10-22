package no.nav.foreldrepenger.datavarehus.metrikker;

import static no.nav.vedtak.log.metrics.MetricsUtil.REGISTRY;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import io.micrometer.core.instrument.Tag;

public class BehandlingMetrikker {

    private static final String BEHANDLING_METRIKK_NAVN = "fp.behandlinger.antall";
    private static final Map<BehandlingStatistikkRepository.Behandlingsårsak, AtomicLong> BEHANDLING_GAUGES = new HashMap<>();

    BehandlingMetrikker() {
        // CDI
    }

    public static void setAntall(BehandlingStatistikkRepository.Behandlingsårsak behandlingsårsak, Long antall) {
        if (BEHANDLING_GAUGES.containsKey(behandlingsårsak)) {
            BEHANDLING_GAUGES.get(behandlingsårsak).set(antall);
        } else {
            var gaugeValue = REGISTRY.gauge(BEHANDLING_METRIKK_NAVN, tilTags(behandlingsårsak), new AtomicLong(antall));
            BEHANDLING_GAUGES.put(behandlingsårsak, gaugeValue);
        }
    }

    static List<Tag> tilTags(BehandlingStatistikkRepository.Behandlingsårsak behandlingsårsak) {
        return Optional.ofNullable(behandlingsårsak)
            .map(ba -> List.of(Tag.of("behandling_aarsak_type", behandlingsårsak.name())))
            .orElseGet(List::of);
    }


}
