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
}
