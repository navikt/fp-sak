package no.nav.foreldrepenger.domene.uttak;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

/**
 * Hjelpeklasse som tilbyr bygging av en mapping mellom kodelisteinnslag og noe annet.
 *
 * Kodelisteinnslag kan ikke brukes som case i en switch, så denne klassen er et kompakt og effektivt alternativ
 * som også støtter toveis mapping.
 */
public class KodeMapper<K extends Kodeverdi, O> {
    private final List<Kodemapping<K,O>> mappinger;

    private KodeMapper(List<Kodemapping<K,O>> mappinger) {
        this.mappinger = Collections.unmodifiableList(mappinger);
    }

    public Optional<O> map(K k) {
        return mappinger.stream()
            .filter(mapping -> mapping.key().equals(k))
            .map(Kodemapping::mapped)
            .findAny();
    }

    public static <R extends Kodeverdi, T> Builder<R, T> medMapping(R r, T t) {
        return new Builder<R, T>().medMapping(r, t);
    }

    public static class Builder<R extends Kodeverdi, T> {
        private final ArrayList<R> rs;
        private final ArrayList<T> ts;

        private Builder() {
            rs = new ArrayList<>();
            ts = new ArrayList<>();
        }

        public Builder<R, T> medMapping(R r, T t) {
            Objects.requireNonNull(r);
            Objects.requireNonNull(t);
            if (rs.contains(r)) {
                throw new IllegalArgumentException(String.format("Har allerede mapping for %s", r));
            }
            if (ts.contains(t)) {
                throw new IllegalArgumentException(String.format("Har allerede mapping for %s", t));
            }
            rs.add(r);
            ts.add(t);
            return this;
        }

        public KodeMapper<R, T> build() {
            return new KodeMapper<>(IntStream.range(0, rs.size())
                .mapToObj(i -> new Kodemapping<R,T>(rs.get(i), ts.get(i)))
                .toList());
        }
    }

    private record Kodemapping<R extends Kodeverdi, O>(R key, O mapped) {}
}
