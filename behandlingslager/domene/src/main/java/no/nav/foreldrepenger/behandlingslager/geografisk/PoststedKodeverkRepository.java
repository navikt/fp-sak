package no.nav.foreldrepenger.behandlingslager.geografisk;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.hibernate.jpa.QueryHints;

import no.nav.vedtak.felles.jpa.HibernateVerktøy;

@ApplicationScoped
public class PoststedKodeverkRepository {

    private EntityManager entityManager;

    PoststedKodeverkRepository() {
        // for CDI proxy
    }

    @Inject
    public PoststedKodeverkRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager");
        this.entityManager = entityManager;
    }

    public List<Poststed> hentAllePostnummer() {
        var query = entityManager.createQuery("from Poststed p", Poststed.class);
        return query.getResultList();
    }

    public Optional<Poststed> finnPoststedReadOnly(String postnummer) {
        var query = entityManager.createQuery("from Poststed p where poststednummer = :postnr", Poststed.class)
            .setParameter("postnr", postnummer)
            .setHint(QueryHints.HINT_READONLY, "true");
        return HibernateVerktøy.hentUniktResultat(query);
    }

    public void lagrePostnummer(Poststed postnummer) {
        entityManager.persist(postnummer);
    }

    public void oppdaterPostnummer(Poststed postnummer, String poststed, LocalDate gyldigFom, LocalDate gyldigTom) {
        entityManager.createNativeQuery(
                "UPDATE POSTSTED SET gyldigfom = :fom , gyldigtom = :tom , poststednavn = :sted WHERE poststednummer = :postnr ")
            .setParameter("postnr", postnummer.getPoststednummer())
            .setParameter("fom", gyldigFom)
            .setParameter("tom", gyldigTom)
            .setParameter("sted", poststed)
            .executeUpdate();
    }
}
