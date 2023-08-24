package no.nav.foreldrepenger.domene.uttak.testutilities.behandling;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

class BehandlingsresultatRepositoryStub extends BehandlingsresultatRepository {

    private final Map<Long, Behandlingsresultat> behandlingsresultatMap = new ConcurrentHashMap<>();

    @Override
    public Optional<Behandlingsresultat> hentHvisEksisterer(Long behandlingId) {
        var behandlingsresultat = behandlingsresultatMap.get(behandlingId);
        if (behandlingsresultat == null) {
            return Optional.empty();
        }
        return Optional.of(behandlingsresultat);
    }

    @Override
    public Behandlingsresultat hent(Long behandlingId) {
        return hentHvisEksisterer(behandlingId).orElseThrow();
    }

    @Override
    public void lagre(Long behandlingId, Behandlingsresultat resultat) {
        behandlingsresultatMap.put(behandlingId, resultat);
    }
}
