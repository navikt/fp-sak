package no.nav.foreldrepenger.domene.uttak.testutilities.behandling;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import no.nav.foreldrepenger.behandlingslager.uttak.Uttaksperiodegrense;
import no.nav.foreldrepenger.behandlingslager.uttak.UttaksperiodegrenseRepository;

class UttaksperiodegrenseRepositoryStub extends UttaksperiodegrenseRepository {

    private final Map<Long, Uttaksperiodegrense> uttaksperiodegrenseMap = new ConcurrentHashMap<>();

    @Override
    public void lagre(Long behandlingId, Uttaksperiodegrense uttaksperiodegrense) {
        uttaksperiodegrenseMap.put(behandlingId, uttaksperiodegrense);
    }

    @Override
    public Uttaksperiodegrense hent(Long behandlingId) {
        return hentHvisEksisterer(behandlingId).orElseThrow();
    }

    @Override
    public Optional<Uttaksperiodegrense> hentHvisEksisterer(Long behandlingId) {
        var uttaksperiodegrense = uttaksperiodegrenseMap.get(behandlingId);
        if (uttaksperiodegrense == null) {
            return Optional.empty();
        }
        return Optional.of(uttaksperiodegrense);
    }
}
