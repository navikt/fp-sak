package no.nav.foreldrepenger.datavarehus.metrikker;

import static no.nav.vedtak.log.metrics.MetricsUtil.REGISTRY;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import io.micrometer.core.instrument.Tag;

public class DokumentMetrikker {

    private static final String DOKUMENT_METRIKK_NAVN = "fp.dokument.antall";
    private static final Map<DokumentStatistikkRepository.Dokumenttype, AtomicLong> DOKUMENT_GAUGES = new HashMap<>();

    DokumentMetrikker() {
        // CDI
    }

    public static void setAntall(DokumentStatistikkRepository.Dokumenttype dokumenttype, Long antall) {
        if (DOKUMENT_GAUGES.containsKey(dokumenttype)) {
            DOKUMENT_GAUGES.get(dokumenttype).set(antall);
        } else {
            var gaugeValue = REGISTRY.gauge(DOKUMENT_METRIKK_NAVN, tilTags(dokumenttype), new AtomicLong(antall));
            DOKUMENT_GAUGES.put(dokumenttype, gaugeValue);
        }
    }

    static List<Tag> tilTags(DokumentStatistikkRepository.Dokumenttype dokumenttype) {
        return Optional.ofNullable(dokumenttype)
            .map(dok -> List.of(Tag.of("dokument_type", dok.name())))
            .orElseGet(List::of);
    }
}
