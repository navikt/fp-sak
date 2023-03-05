package no.nav.foreldrepenger.behandlingslager.behandling.repository;

import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.vedtak.exception.TekniskException;

/**
 * @see BehandlingLås
 */
@ApplicationScoped
public class BehandlingLåsRepository {
    private EntityManager entityManager;

    protected BehandlingLåsRepository() {
        // for CDI proxy
    }

    @Inject
    public BehandlingLåsRepository( EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /** Initialiser lås og ta lock på tilhørende database rader. */
    public BehandlingLås taLås(final Long behandlingId) {
        if (behandlingId != null) {
            låsBehandling(behandlingId);
            var lås = new BehandlingLås(behandlingId);
            return lås;
        }
        return new BehandlingLås(null);

    }

    public BehandlingLås taLås(final UUID eksternBehandlingRef) {
        if (eksternBehandlingRef != null) {
            var behandlingId = låsBehandling(eksternBehandlingRef);
            var lås = new BehandlingLås(behandlingId);
            return lås;
        }
        return new BehandlingLås(null);
    }

    private void låsBehandling(final Long behandlingId) {
        // bruk native query så vi ikke går i beina på hibernate session cache og transiente data
        entityManager
            .createNativeQuery("select 1 from BEHANDLING beh where beh.id=:id FOR UPDATE")
            .setParameter("id", behandlingId)
            .setFlushMode(FlushModeType.COMMIT)
            .getSingleResult();
    }

    private Long låsBehandling(final UUID eksternBehandlingRef) {
        // bruk native query så vi ikke går i beina på hibernate session cache og transiente data
        var result = entityManager
            .createNativeQuery("select beh.id from BEHANDLING beh where beh.uuid=:uuid FOR UPDATE")
            .setParameter("uuid", eksternBehandlingRef)
            .getSingleResult();
        return Long.valueOf(((Number) result).longValue()); // JPA tar ikke scalar output mapping direkte
    }

    /**
     * Verifiser lås ved å sjekke mot underliggende lager.
     * tvinger inkrementerer versjon på relevante parent entiteteter (fagsak, fagsakrelasjon, behandling) slik
     * at andre vil oppdage endringer og få OptimisticLockException
     */
    public void oppdaterLåsVersjon(BehandlingLås lås) {
        if (lås.getBehandlingId() != null) {
            verifisertLås(lås.getBehandlingId());
        } // else NO-OP (for ny behandling uten id)
    }

    private Object verifisertLås(Long id) {
        var lockMode = LockModeType.PESSIMISTIC_FORCE_INCREMENT;
        Object entity = entityManager.find(Behandling.class, id);
        if (entity == null) {
            var msg = String.format("Fant ikke entitet for låsing [%s], id=%s.",
                Behandling.class.getSimpleName(), id);
            throw new TekniskException("FP-131239", msg);
        }
        entityManager.lock(entity, lockMode);
        return entity;
    }

}
