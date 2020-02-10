package no.nav.foreldrepenger.behandlingslager.behandling.vilkår;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.vedtak.felles.jpa.VLPersistenceUnit;

@ApplicationScoped
public class VilkårResultatRepository {
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private EntityManager entityManager;

    VilkårResultatRepository() {
        // for CDI proxy
    }

    @Inject
    public VilkårResultatRepository(@VLPersistenceUnit EntityManager entityManager) {
        this.entityManager = entityManager;
        this.behandlingsresultatRepository = new BehandlingsresultatRepository(entityManager);
    }

    public Optional<VilkårResultat> hentHvisEksisterer(Long behandlingId) {
        return behandlingsresultatRepository.hentHvisEksisterer(behandlingId).map(Behandlingsresultat::getVilkårResultat);
    }

    public VilkårResultat hent(Long behandlingId) {
        return behandlingsresultatRepository.hent(behandlingId).getVilkårResultat();
    }
    
    public void lagre(Long behandlingId, VilkårResultat resultat) {
        var behandlingsresultat = behandlingsresultatRepository.hent(behandlingId);
        if(resultat.getOriginalBehandlingsresultat()==null) {
            resultat.setOriginalBehandlingsresultat(behandlingsresultat);
        }
        behandlingsresultat.medOppdatertVilkårResultat(resultat);
        entityManager.persist(resultat);
        behandlingsresultatRepository.lagre(behandlingId, behandlingsresultat);
    }
    
}
