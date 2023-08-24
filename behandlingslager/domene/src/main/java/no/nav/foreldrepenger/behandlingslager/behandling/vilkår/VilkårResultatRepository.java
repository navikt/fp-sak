package no.nav.foreldrepenger.behandlingslager.behandling.vilkår;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;

import java.util.Optional;

@ApplicationScoped
public class VilkårResultatRepository {
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private BehandlingRepository behandlingRepository;
    private EntityManager entityManager;

    VilkårResultatRepository() {
        // for CDI proxy
    }

    @Inject
    public VilkårResultatRepository( EntityManager entityManager) {
        this.entityManager = entityManager;
        this.behandlingsresultatRepository = new BehandlingsresultatRepository(entityManager);
        this.behandlingRepository = new BehandlingRepository(entityManager);
    }

    public Optional<VilkårResultat> hentHvisEksisterer(Long behandlingId) {
        return behandlingsresultatRepository.hentHvisEksisterer(behandlingId).map(Behandlingsresultat::getVilkårResultat);
    }

    public VilkårResultat hent(Long behandlingId) {
        return behandlingsresultatRepository.hent(behandlingId).getVilkårResultat();
    }

    public void lagre(Long behandlingId, VilkårResultat resultat) {
        var behandlingsresultat = behandlingsresultatRepository.hent(behandlingId);
        settOriginalVedBehov(behandlingId, resultat);
        behandlingsresultat.medOppdatertVilkårResultat(resultat);
        entityManager.persist(resultat);
        behandlingsresultatRepository.lagre(behandlingId, behandlingsresultat);
    }

    private void settOriginalVedBehov(Long behandlingId, VilkårResultat resultat) {
        var originalBehandlingId = resultat.getOriginalBehandlingId();
        if (originalBehandlingId == null || behandlingsresultatRepository.hentHvisEksisterer(originalBehandlingId).isEmpty()) {
            resultat.setOriginalBehandling(behandlingRepository.hentBehandling(behandlingId));
        }
    }

}
