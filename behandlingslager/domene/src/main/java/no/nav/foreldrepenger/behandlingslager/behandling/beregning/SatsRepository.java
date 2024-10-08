package no.nav.foreldrepenger.behandlingslager.behandling.beregning;

import static no.nav.vedtak.felles.jpa.HibernateVerktøy.hentEksaktResultat;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.hibernate.jpa.HibernateHints;

@ApplicationScoped
public class SatsRepository {

    private EntityManager entityManager;

    SatsRepository() {
        // for CDI proxy
    }

    @Inject
    public SatsRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager");
        this.entityManager = entityManager;
    }

    public BeregningSats finnEksaktSats(BeregningSatsType satsType, LocalDate dato) {
        var query = entityManager.createQuery("from BeregningSats where satsType=:satsType" +
                " and periode.fomDato<=:dato" +
                " and periode.tomDato>=:dato", BeregningSats.class);

        query.setParameter("satsType", satsType);
        query.setParameter("dato", dato);
        query.setHint(HibernateHints.HINT_READ_ONLY, "true");
        query.getResultList();
        return hentEksaktResultat(query);
    }

    public BeregningSats finnGjeldendeSats(BeregningSatsType satsType) {
        var query = entityManager.createQuery("from BeregningSats where satsType=:satsType", BeregningSats.class);

        query.setParameter("satsType", satsType);
        return query.getResultList().stream()
            .max(Comparator.comparing(s -> s.getPeriode().getFomDato())).orElseThrow(() -> new IllegalStateException("Fant ikke nyeste sats"));
    }

    public void lagreSats(BeregningSats sats) {
        entityManager.persist(sats);
        entityManager.flush();
    }

}
