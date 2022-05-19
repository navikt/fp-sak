package no.nav.foreldrepenger.dbstoette;

import java.util.List;

import javax.persistence.EntityManager;

public final class DBTestUtil {

    public static <T> List<T> hentAlle(EntityManager entityManager, Class<T> klasse) {
        var criteria = entityManager.getCriteriaBuilder().createQuery(klasse);
        criteria.select(criteria.from(klasse));
        return entityManager.createQuery(criteria).getResultList();
    }
}
