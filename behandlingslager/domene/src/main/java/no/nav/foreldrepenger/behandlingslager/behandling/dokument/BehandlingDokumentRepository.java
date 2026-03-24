package no.nav.foreldrepenger.behandlingslager.behandling.dokument;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import no.nav.vedtak.felles.jpa.HibernateVerktøy;

@ApplicationScoped
public class BehandlingDokumentRepository {

    private EntityManager entityManager;

    protected BehandlingDokumentRepository() {
        // CDI
    }

    @Inject
    public BehandlingDokumentRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Optional<BehandlingDokumentBestiltEntitet> hentHvisEksisterer(UUID bestillingUuid) {
        var query = entityManager.createQuery("from BehandlingDokumentBestilt where bestillingUuid = :bestillingUuid", BehandlingDokumentBestiltEntitet.class);
        query.setParameter("bestillingUuid", bestillingUuid);
        return HibernateVerktøy.hentUniktResultat(query);
    }

    public void lagreOgFlush(BehandlingDokumentBestiltEntitet behandlingDokument) {
        Objects.requireNonNull(behandlingDokument, "behandlingDokument");
        if (behandlingDokument.getId() == null) {
            entityManager.persist(behandlingDokument);
        } else {
            entityManager.merge(behandlingDokument);
        }
        entityManager.flush();
    }

    public Optional<BehandlingDokumentEntitet> hentHvisEksisterer(Long behandlingId) {
        var query = entityManager.createQuery("from BehandlingDokument where behandlingId = :behandlingId", BehandlingDokumentEntitet.class);
        query.setParameter("behandlingId", behandlingId);
        return HibernateVerktøy.hentUniktResultat(query);
    }

    public void lagreOgFlush(BehandlingDokumentEntitet behandlingDokument) {
        Objects.requireNonNull(behandlingDokument, "behandlingDokument");
        if (behandlingDokument.getId() == null) {
            entityManager.persist(behandlingDokument);
        } else {
            entityManager.merge(behandlingDokument);
        }
        entityManager.flush();
    }

    public Optional<BehandlingBrevMellomlagringEntitet> hentMellomlagretBrev(Long behandlingId, DokumentMalType dokumentMalType) {
        var query = entityManager.createQuery(
            "from BehandlingBrevMellomlagring m where m.behandlingDokument.behandlingId = :behandlingId and m.dokumentMalType = :dokumentMalType",
            BehandlingBrevMellomlagringEntitet.class);
        query.setParameter("behandlingId", behandlingId);
        query.setParameter("dokumentMalType", dokumentMalType);
        return HibernateVerktøy.hentUniktResultat(query);
    }

    public void lagreOgFlush(BehandlingBrevMellomlagringEntitet mellomlagring) {
        Objects.requireNonNull(mellomlagring, "mellomlagring");
        if (mellomlagring.getId() == null) {
            entityManager.persist(mellomlagring);
        } else {
            entityManager.merge(mellomlagring);
        }
        entityManager.flush();
    }

    public void fjernMellomlagretBrev(Long behandlingId, DokumentMalType dokumentMalType) {
        entityManager.createQuery(
                "delete from BehandlingBrevMellomlagring m where m.behandlingDokument.behandlingId = :behandlingId and m.dokumentMalType = :dokumentMalType")
            .setParameter("behandlingId", behandlingId)
            .setParameter("dokumentMalType", dokumentMalType)
            .executeUpdate();
        entityManager.flush();
    }

    public void fjernAlleMellomlagredeBrev(Long behandlingId) {
        entityManager.createQuery(
                "delete from BehandlingBrevMellomlagring m where m.behandlingDokument.behandlingId = :behandlingId")
            .setParameter("behandlingId", behandlingId)
            .executeUpdate();
        entityManager.flush();
    }
}
