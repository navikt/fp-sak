package no.nav.foreldrepenger.behandlingslager.hendelser;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.hibernate.jpa.HibernateHints;

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
        var query = entityManager.createQuery("from MottattHendelse where hendelseUid=:hendelse_uid", MottattHendelse.class)
            .setParameter("hendelse_uid", hendelseUid)
            .setHint(HibernateHints.HINT_READ_ONLY, "true");
        return query.getResultList().isEmpty();
    }

    public void registrerMottattHendelse(String hendelseUid) {
        entityManager.createNativeQuery("INSERT INTO MOTTATT_HENDELSE (hendelse_uid) VALUES (:hendelse_uid)")
            .setParameter("hendelse_uid", hendelseUid)
            .executeUpdate();
        entityManager.flush();
    }

    public void registrerMottattVedtak(MottattVedtak mottattVedtak) {
        entityManager.persist(mottattVedtak);
    }

}
