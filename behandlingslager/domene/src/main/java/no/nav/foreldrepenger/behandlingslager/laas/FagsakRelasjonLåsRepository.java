package no.nav.foreldrepenger.behandlingslager.laas;

import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonLåsegrunnlag;

@ApplicationScoped
public class FagsakRelasjonLåsRepository {

    private EntityManager entityManager;

    protected FagsakRelasjonLåsRepository() {
    }

    @Inject
    public FagsakRelasjonLåsRepository( EntityManager entityManager) {
        this.entityManager = entityManager;
    }


    public FagsakRelasjonLås taLåsForRelasjon(final FagsakRelasjonLåsegrunnlag låsegrunnlag) {
        Objects.requireNonNull(låsegrunnlag, "låsegrunnlag");
        Objects.requireNonNull(låsegrunnlag.relasjonId(), "relasjonId");
        entityManager.createQuery("select fr.id, fr.versjon from FagsakRelasjon fr where fr.id=:id")
            .setParameter("id", låsegrunnlag.relasjonId())
            .setLockMode(LockModeType.PESSIMISTIC_WRITE)
            .getSingleResult();
        return new FagsakRelasjonLås(låsegrunnlag.relasjonId());
    }

}
