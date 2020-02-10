package no.nav.foreldrepenger.behandlingslager.behandling.vedtak;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import no.nav.vedtak.felles.jpa.VLPersistenceUnit;

@ApplicationScoped
public class BehandlingOverlappInfotrygdRepository {

    private EntityManager entityManager;

    public BehandlingOverlappInfotrygdRepository() {
        // for CDI proxy
    }

    @Inject
    public BehandlingOverlappInfotrygdRepository(@VLPersistenceUnit EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Long lagre(BehandlingOverlappInfotrygd behandlingOverlappInfotrygd) {
        entityManager.persist(behandlingOverlappInfotrygd);
        entityManager.flush();
        return behandlingOverlappInfotrygd.getId();
    }
}
