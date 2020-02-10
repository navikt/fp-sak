package no.nav.foreldrepenger.behandlingslager.hendelser;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.hibernate.jpa.QueryHints;

import no.nav.vedtak.felles.jpa.VLPersistenceUnit;

@ApplicationScoped
public class HendelsemottakRepository {

    private EntityManager entityManager;

    HendelsemottakRepository() {
        // CDI
    }

    @Inject
    public HendelsemottakRepository(@VLPersistenceUnit EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public boolean hendelseErNy(String hendelseUid) {
        TypedQuery<MottattHendelse> query = entityManager.createQuery("from MottattHendelse where hendelse_uid=:hendelse_uid", MottattHendelse.class);
        query.setParameter("hendelse_uid", hendelseUid);
        query.setHint(QueryHints.HINT_READONLY, "true");
        return query.getResultList().isEmpty();
    }

    public void registrerMottattHendelse(String hendelseUid) {
        Query query = entityManager.createNativeQuery("INSERT INTO MOTTATT_HENDELSE (hendelse_uid) VALUES (:hendelse_uid)");
        query.setParameter("hendelse_uid", hendelseUid);
        query.executeUpdate();
    }

}
