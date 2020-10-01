package no.nav.foreldrepenger.behandlingslager.behandling.opptjening.utlanddok;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import no.nav.vedtak.felles.jpa.HibernateVerktøy;

@ApplicationScoped
public class OpptjeningIUtlandDokStatusRepository {

    private EntityManager entityManager;

    @Inject
    public OpptjeningIUtlandDokStatusRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    OpptjeningIUtlandDokStatusRepository() {
        //CDI
    }

    public void lagre(OpptjeningIUtlandDokStatusEntitet entitet) {
        deaktiverStatus(entitet.getBehandlingId());
        entityManager.persist(entitet);
        entityManager.flush();
    }

    public Optional<OpptjeningIUtlandDokStatusEntitet> hent(long behandlingId) {
        final TypedQuery<OpptjeningIUtlandDokStatusEntitet> query = entityManager.createQuery("FROM OpptjeningIUtlandDokStatusEntitet status " +
            "WHERE status.behandlingId = :behandlingId " +
            "AND status.aktiv = :aktivt", OpptjeningIUtlandDokStatusEntitet.class);
        query.setParameter("behandlingId", behandlingId);
        query.setParameter("aktivt", true);
        return HibernateVerktøy.hentUniktResultat(query);
    }

    public void kopierGrunnlagFraEksisterendeBehandling(long originalBehandling, long nyBehandling) {
        var originalGrunnlag = hent(originalBehandling);
        if (originalGrunnlag.isPresent()) {
            lagre(new OpptjeningIUtlandDokStatusEntitet(nyBehandling, originalGrunnlag.get().getDokStatus()));
        }
    }

    public void deaktiverStatus(Long behandlingId) {
        var eksisterende = hent(behandlingId);
        if (eksisterende.isPresent()) {
            eksisterende.get().setAktiv(false);
            entityManager.persist(eksisterende.get());
            entityManager.flush();
        }
    }
}
