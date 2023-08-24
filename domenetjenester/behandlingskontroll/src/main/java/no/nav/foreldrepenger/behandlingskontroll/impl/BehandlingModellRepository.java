package no.nav.foreldrepenger.behandlingskontroll.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@ApplicationScoped
public class BehandlingModellRepository implements AutoCloseable {

    private final ConcurrentMap<Object, BehandlingModell> cachedModell = new ConcurrentHashMap<>();

    @Inject
    public BehandlingModellRepository() {
        // Plattform trenger tom Ctor (Hibernate, CDI, etc)
    }

    /**
     * Finn modell for angitt behandling type.
     * <p>
     * Når modellen ikke lenger er i bruk må {@link BehandlingModellImpl#close()}
     * kalles slik at den ikke fortsetter å holde på referanser til objekter. (DETTE
     * KAN DROPPES OM VI FÅR CACHET MODELLENE!)
     */
    public BehandlingModell getModell(BehandlingType behandlingType, FagsakYtelseType fagsakYtelseType) {
        var key = cacheKey(behandlingType, fagsakYtelseType);
        cachedModell.computeIfAbsent(key, kode -> byggModell(behandlingType, fagsakYtelseType));
        return cachedModell.get(key);
    }

    protected Object cacheKey(BehandlingType behandlingType, FagsakYtelseType fagsakYtelseType) {
        // lager en key av flere sammensatte elementer.
        return Arrays.asList(behandlingType, fagsakYtelseType);
    }

    protected BehandlingModell byggModell(BehandlingType behandlingType, FagsakYtelseType ytelseType) {
        return BehandlingTypeRef.Lookup.find(BehandlingModell.class, ytelseType, behandlingType)
                .orElseThrow(() -> new IllegalStateException(
                        "Har ikke BehandlingModell for BehandlingType:" + behandlingType + ", ytelseType:" + ytelseType));
    }

    @Override
    public void close() {
        cachedModell.clear();
    }
}
