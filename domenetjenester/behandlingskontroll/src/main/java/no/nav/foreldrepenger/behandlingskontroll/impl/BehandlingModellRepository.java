package no.nav.foreldrepenger.behandlingskontroll.impl;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import jakarta.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

@ApplicationScoped
public class BehandlingModellRepository {

    private static final ConcurrentMap<Object, BehandlingModell> MODELL_CACHE = new ConcurrentHashMap<>();

    private BehandlingModellRepository() {
    }


    /**
     * Finn modell for angitt behandling type.
     */
    public static synchronized BehandlingModell getModell(BehandlingType behandlingType, FagsakYtelseType fagsakYtelseType) {
        var key = List.of(behandlingType, fagsakYtelseType);
        MODELL_CACHE.computeIfAbsent(key, kode -> byggModell(behandlingType, fagsakYtelseType));
        return MODELL_CACHE.get(key);
    }

    private static BehandlingModell byggModell(BehandlingType behandlingType, FagsakYtelseType ytelseType) {
        return BehandlingTypeRef.Lookup.find(BehandlingModell.class, ytelseType, behandlingType)
                .orElseThrow(() -> new IllegalStateException(
                        "Har ikke BehandlingModell for BehandlingType:" + behandlingType + ", ytelseType:" + ytelseType));
    }
}
