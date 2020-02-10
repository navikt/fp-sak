package no.nav.foreldrepenger.behandlingslager.abakus.logg;

import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import no.nav.vedtak.felles.jpa.HibernateVerktøy;
import no.nav.vedtak.felles.jpa.VLPersistenceUnit;


@ApplicationScoped
public class AbakusInnhentingGrunnlagLoggRepository {
    private EntityManager entityManager;

    AbakusInnhentingGrunnlagLoggRepository() {
        // CDI
    }

    @Inject
    public AbakusInnhentingGrunnlagLoggRepository(@VLPersistenceUnit EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager");
        this.entityManager = entityManager;
    }

    public void lagre(AbakusInnhentingGrunnlagLogg entitet) {
        final var eksisterendeElement = hentGjeldende(entitet.getBehandlingId());
        if (eksisterendeElement.isPresent()) {
            final var logg = eksisterendeElement.get();
            logg.setAktiv(false);
            entityManager.persist(logg);
            entityManager.flush();
        }
        entityManager.persist(entitet);
        entityManager.flush();
    }

    private Optional<AbakusInnhentingGrunnlagLogg> hentGjeldende(Long behandlingId) {
        final var query = entityManager.createQuery("SELECT log " +
            "FROM AbakusInnhentingGrunnlagLogg log " +
            "WHERE log.behandlingId = :behandlingId AND log.aktiv = true", AbakusInnhentingGrunnlagLogg.class);
        query.setParameter("behandlingId", behandlingId);
        return HibernateVerktøy.hentUniktResultat(query);
    }
}
