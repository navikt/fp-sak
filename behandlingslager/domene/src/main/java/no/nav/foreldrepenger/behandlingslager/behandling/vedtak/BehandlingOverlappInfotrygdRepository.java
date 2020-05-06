package no.nav.foreldrepenger.behandlingslager.behandling.vedtak;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

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

    public List<BehandlingOverlappInfotrygd> hentForBehandling(Long behandlingId) {
        TypedQuery<BehandlingOverlappInfotrygd> query = entityManager
            .createQuery("from BehandlingOverlappInfotrygd where behandlingId=:behandlingId",
                BehandlingOverlappInfotrygd.class);
        query.setParameter("behandlingId", behandlingId); // NOSONAR
        return query.getResultList();
    }

    public Long lagre(BehandlingOverlappInfotrygd behandlingOverlappInfotrygd) {
        entityManager.persist(behandlingOverlappInfotrygd);
        entityManager.flush();
        return behandlingOverlappInfotrygd.getId();
    }
}
