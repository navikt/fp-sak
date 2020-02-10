package no.nav.foreldrepenger.behandlingslager.laas;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;

import no.nav.vedtak.felles.jpa.VLPersistenceUnit;

@ApplicationScoped
public class FagsakRelasjonLåsRepository {

    private EntityManager entityManager;

    protected FagsakRelasjonLåsRepository() {
    }

    @Inject
    public FagsakRelasjonLåsRepository(@VLPersistenceUnit EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Tar lås på underliggende rader
     *
     * @param fagsakIdIn id for fagsaken
     * @return låsen
     */
    public FagsakRelasjonLås taLås(final Long fagsakIdIn) {
        final LockModeType lockModeType = LockModeType.PESSIMISTIC_WRITE;

        FagsakRelasjonLås lås = new FagsakRelasjonLås();

        // sjekker om fagsakId != null slik at det fungerer som no-op for ferske, unpersisted entiteter
        if (fagsakIdIn != null) {
            // bruker enkle queries slik at unngår laste eager associations og entiteter her
            Long relasjonId = låsFagsakRelasjon(fagsakIdIn, lockModeType);
            lås.setFagsakRelasjonId(relasjonId);
        }

        return lås;
    }

    private Long låsFagsakRelasjon(final Long fagsakId, LockModeType lockModeType) {
        Object[] resultFs = (Object[]) entityManager
            .createQuery("select fr.id, fr.versjon from FagsakRelasjon fr where (fr.fagsakNrEn.id=:id or fr.fagsakNrTo.id=:id) and fr.aktiv = true") //$NON-NLS-1$
            .setParameter("id", fagsakId) //$NON-NLS-1$
            .setLockMode(lockModeType)
            .getSingleResult();
        return (Long) resultFs[0];
    }
}
