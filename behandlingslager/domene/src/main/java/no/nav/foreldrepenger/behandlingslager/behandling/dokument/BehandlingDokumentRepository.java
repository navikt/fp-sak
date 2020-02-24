package no.nav.foreldrepenger.behandlingslager.behandling.dokument;

import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

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

    public Optional<BehandlingDokumentEntitet> hentHvisEksisterer(Long behandlingId) {
        TypedQuery<BehandlingDokumentEntitet> query = entityManager.createQuery("from BehandlingDokument where behandlingId = :behandlingId", BehandlingDokumentEntitet.class);
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
