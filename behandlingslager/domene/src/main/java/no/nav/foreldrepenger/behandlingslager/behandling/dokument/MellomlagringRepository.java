package no.nav.foreldrepenger.behandlingslager.behandling.dokument;

import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import no.nav.vedtak.felles.jpa.HibernateVerktøy;

@ApplicationScoped
public class MellomlagringRepository {

    private EntityManager entityManager;

    protected MellomlagringRepository() {
        // CDI
    }

    @Inject
    public MellomlagringRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Optional<MellomlagringEntitet> hentMellomlagring(Long behandlingId, MellomlagringType type) {
        var query = entityManager.createQuery(
            "from BehandlingMellomlagring m where m.behandlingId = :behandlingId and m.type = :type",
            MellomlagringEntitet.class);
        query.setParameter("behandlingId", behandlingId);
        query.setParameter("type", type);
        return HibernateVerktøy.hentUniktResultat(query);
    }

    public void lagreOgFlush(MellomlagringEntitet mellomlagring) {
        Objects.requireNonNull(mellomlagring, "mellomlagring");
        if (mellomlagring.getId() == null) {
            entityManager.persist(mellomlagring);
        } else {
            entityManager.merge(mellomlagring);
        }
        entityManager.flush();
    }

    public void lagreEllerOppdater(Long behandlingId, MellomlagringType type, String innhold) {
        var eksisterende = hentMellomlagring(behandlingId, type);
        if (eksisterende.isPresent()) {
            eksisterende.get().setInnhold(innhold);
            lagreOgFlush(eksisterende.get());
        } else {
            var mellomlagring = MellomlagringEntitet.Builder.ny()
                .medBehandlingId(behandlingId)
                .medType(type)
                .medInnhold(innhold)
                .build();
            lagreOgFlush(mellomlagring);
        }
    }

    public void fjernMellomlagring(Long behandlingId, MellomlagringType type) {
        entityManager.createQuery(
                "delete from BehandlingMellomlagring m where m.behandlingId = :behandlingId and m.type = :type")
            .setParameter("behandlingId", behandlingId)
            .setParameter("type", type)
            .executeUpdate();
        entityManager.flush();
    }

    public void fjernAlleMellomlagringer(Long behandlingId) {
        entityManager.createQuery(
                "delete from BehandlingMellomlagring m where m.behandlingId = :behandlingId")
            .setParameter("behandlingId", behandlingId)
            .executeUpdate();
        entityManager.flush();
    }
}
