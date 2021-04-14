package no.nav.foreldrepenger.behandlingslager.hendelser;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.hibernate.jpa.QueryHints;

@ApplicationScoped
public class HendelsemottakRepository {

    private EntityManager entityManager;

    HendelsemottakRepository() {
        // CDI
    }

    @Inject
    public HendelsemottakRepository( EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public boolean hendelseErNy(String hendelseUid) {
        var query = entityManager.createQuery("from MottattHendelse where hendelse_uid=:hendelse_uid", MottattHendelse.class);
        query.setParameter("hendelse_uid", hendelseUid);
        query.setHint(QueryHints.HINT_READONLY, "true");
        return query.getResultList().isEmpty();
    }

    public void registrerMottattHendelse(String hendelseUid) {
        var query = entityManager.createNativeQuery("INSERT INTO MOTTATT_HENDELSE (hendelse_uid) VALUES (:hendelse_uid)");
        query.setParameter("hendelse_uid", hendelseUid);
        query.executeUpdate();
    }

}
