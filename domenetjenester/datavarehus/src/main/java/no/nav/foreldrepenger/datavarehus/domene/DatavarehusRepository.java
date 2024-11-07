package no.nav.foreldrepenger.datavarehus.domene;

import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@ApplicationScoped
public class DatavarehusRepository {

    private EntityManager entityManager;

    DatavarehusRepository() {
        // for CDI proxy
    }

    @Inject
    public DatavarehusRepository( EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager");
        this.entityManager = entityManager;
    }

    public long lagre(BehandlingDvh behandlingDvh) {
        entityManager.persist(behandlingDvh);
        return behandlingDvh.getId();
    }

}
