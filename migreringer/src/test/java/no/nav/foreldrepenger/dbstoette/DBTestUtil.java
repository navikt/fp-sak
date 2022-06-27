package no.nav.foreldrepenger.dbstoette;

import java.util.List;

import javax.persistence.EntityManager;

import no.nav.foreldrepenger.konfig.Environment;

public final class DBTestUtil {

    public static boolean kjøresAvMaven() {
        return Environment.current().getProperty("maven.cmd.line.args") != null;
    }

    public static <T> List<T> hentAlle(EntityManager entityManager, Class<T> klasse) {
        var criteria = entityManager.getCriteriaBuilder().createQuery(klasse);
        criteria.select(criteria.from(klasse));
        return entityManager.createQuery(criteria).getResultList();
    }
}
