package no.nav.foreldrepenger.datavarehus.metrikker;

import io.micrometer.core.instrument.Tag;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static no.nav.vedtak.log.metrics.MetricsUtil.REGISTRY;

public class VedtakMetrikker {

    private static final String VEDTAK_METRIKK_NAVN = "fp.vedtak.antall";
    private static final Map<VedtakStatistikkRepository.VedtakResultat, AtomicLong> VEDTAK_GAUGES = new EnumMap<>(VedtakStatistikkRepository.VedtakResultat.class);

    VedtakMetrikker() {
        // CDI
    }

    public static void setAntall(VedtakStatistikkRepository.VedtakResultat vedtakResultatType, Long antall) {
        if (VEDTAK_GAUGES.containsKey(vedtakResultatType)) {
            VEDTAK_GAUGES.get(vedtakResultatType).set(antall);
        } else {
            var gaugeValue = REGISTRY.gauge(VEDTAK_METRIKK_NAVN, tilTags(vedtakResultatType), new AtomicLong(antall));
            VEDTAK_GAUGES.put(vedtakResultatType, gaugeValue);
        }
    }

    static List<Tag> tilTags(VedtakStatistikkRepository.VedtakResultat vedtakResultatType) {
        return Optional.ofNullable(vedtakResultatType)
            .map(vr -> List.of(Tag.of("vedtak_resultat_type", vr.name())))
            .orElseGet(List::of);
    }

}
