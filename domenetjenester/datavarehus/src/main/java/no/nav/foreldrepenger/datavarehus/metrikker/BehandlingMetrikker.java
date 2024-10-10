package no.nav.foreldrepenger.datavarehus.metrikker;

import static no.nav.vedtak.log.metrics.MetricsUtil.REGISTRY;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.enterprise.context.ApplicationScoped;

import io.micrometer.core.instrument.Tag;
import no.nav.foreldrepenger.datavarehus.metrikker.BehandlingStatistikkRepository.BehandlingStatistikk.Type;

@ApplicationScoped
public class BehandlingMetrikker {

    private static final String BEHANDLING_METRIKK_NAVN = "behandlinger.antall";
    private static final Map<Type, AtomicLong> BEHANDLING_GAUGES = new HashMap<>();

    BehandlingMetrikker() {
        // CDI
    }

    static List<Tag> tilTags(Type type) {
        var tags = new ArrayList<Tag>();
        if (type.ytelseType() != null) {
            tags.add(Tag.of("ytelse_type", type.ytelseType().getNavn()));
        }
        if (type.behandlingType() != null) {
            tags.add(Tag.of("behandling_type", type.behandlingType().getNavn()));
        }
        if (type.behandlingsårsak() != null) {
            tags.add(Tag.of("behandling_aarsak_type", type.behandlingsårsak().name()));
        }
        return tags;
    }

    public void setAntall(Type type, Long antall) {
        if (BEHANDLING_GAUGES.containsKey(type)) {
            BEHANDLING_GAUGES.get(type).set(antall);
        } else {
            var gaugeValue = REGISTRY.gauge(BEHANDLING_METRIKK_NAVN, tilTags(type), new AtomicLong(antall));
            BEHANDLING_GAUGES.put(type, gaugeValue);
        }
    }
}
