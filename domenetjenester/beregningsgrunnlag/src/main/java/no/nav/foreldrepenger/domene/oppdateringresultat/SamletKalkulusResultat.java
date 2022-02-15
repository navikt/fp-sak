package no.nav.foreldrepenger.domene.oppdateringresultat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import no.nav.folketrygdloven.beregningsgrunnlag.BgRef;

public class SamletKalkulusResultat {
    private final Map<UUID, KalkulusResultat> resultater;
    private final List<BgRef> bgReferanser;

    public SamletKalkulusResultat(Map<UUID, KalkulusResultat> resultater, Map<UUID, LocalDate> skjæringstidspunkter) {
        this(resultater, skjæringstidspunkter.entrySet().stream().map(v -> new BgRef(v.getKey(), v.getValue())).collect(Collectors.toList()));
    }

    public SamletKalkulusResultat(Map<UUID, KalkulusResultat> resultater, Collection<BgRef> bgReferanser) {

        this.bgReferanser = new ArrayList<>(bgReferanser);

        this.resultater = Collections.unmodifiableMap(resultater);

        var uuids = bgReferanser.stream().map(BgRef::getRef).collect(Collectors.toSet());
        if (!uuids.containsAll(resultater.keySet())) {
            // har færre bgReferanser enn resultater - ikke bra. Kan ha færre resultater enn bgReferanser - Ok
            throw new IllegalArgumentException("Mismatch skjæringstidspunkt, resultater: " + uuids + " vs. " + resultater.keySet());
        }
    }

    public Map<UUID, KalkulusResultat> getResultater() {
        return Collections.unmodifiableMap(resultater);
    }

    public List<BgRef> getBgReferanser() {
        return Collections.unmodifiableList(bgReferanser);
    }

    public LocalDate getStp(UUID eksternReferanse) {
        return bgReferanser.stream().filter(r -> Objects.equals(r.getRef(), eksternReferanse)).findFirst().map(BgRef::getStp).orElseThrow();
    }
}
