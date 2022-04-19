package no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.samlinger;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.Betalingsmottaker;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.KjedeNøkkel;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.Ytelse;

public class GruppertYtelse {

    public static final GruppertYtelse TOM = new GruppertYtelse(Collections.emptyMap());

    private final Map<KjedeNøkkel, Ytelse> ytelsePrNøkkel;

    public GruppertYtelse(Map<KjedeNøkkel, Ytelse> ytelsePrNøkkel) {
        this.ytelsePrNøkkel = ytelsePrNøkkel;
    }

    public Set<Betalingsmottaker> getBetalingsmottakere() {
        return ytelsePrNøkkel.keySet().stream()
            .map(KjedeNøkkel::getBetalingsmottaker)
            .collect(Collectors.toSet());
    }

    public Map<KjedeNøkkel, Ytelse> finnYtelse(Betalingsmottaker betalingsmottaker) {
        return ytelsePrNøkkel.entrySet().stream()
            .filter(e -> e.getKey().getBetalingsmottaker().equals(betalingsmottaker))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public Map<KjedeNøkkel, Ytelse> getYtelsePrNøkkel() {
        return Collections.unmodifiableMap(ytelsePrNøkkel);
    }

    public Set<KjedeNøkkel> getNøkler() {
        return ytelsePrNøkkel.keySet();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Map<KjedeNøkkel, Ytelse> ytelsePrNøkkel = new HashMap<>();

        private Builder() {
        }

        public Builder leggTilKjede(KjedeNøkkel nøkkel, Ytelse ytelse) {
            if (ytelsePrNøkkel.containsKey(nøkkel)) {
                throw new IllegalArgumentException("Har allerede kjede for denne nøkkelen");
            }
            ytelsePrNøkkel.put(nøkkel, ytelse);
            return this;
        }

        public GruppertYtelse build() {
            return new GruppertYtelse(ytelsePrNøkkel);
        }
    }
}
