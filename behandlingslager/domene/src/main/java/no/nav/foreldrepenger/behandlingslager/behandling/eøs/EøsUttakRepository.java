package no.nav.foreldrepenger.behandlingslager.behandling.eøs;

import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import no.nav.vedtak.felles.jpa.HibernateVerktøy;

@ApplicationScoped
public class EøsUttakRepository {

    private EntityManager entityManager;

    @Inject
    public EøsUttakRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager");
        this.entityManager = entityManager;
    }

    EøsUttakRepository() {
        // CDI proxy
    }

    public Optional<EøsUttakGrunnlagEntitet> hentGrunnlag(Long behandlingId) {
        var query = entityManager.createQuery(
            "FROM EøsUttak p WHERE p.behandlingId = :behandlingId AND p.aktiv = true",
            EøsUttakGrunnlagEntitet.class).setParameter("behandlingId", behandlingId);

        return HibernateVerktøy.hentUniktResultat(query);
    }

    public void lagreEøsUttak(Long behandlingId, EøsUttaksperioderEntitet eøsUttak) {
        var aktivtGrunnlag = hentGrunnlag(behandlingId);
        var nyttGrunnlag = EøsUttakGrunnlagEntitet.Builder.ny()
            .medBehandlingId(behandlingId)
            .medEøsUttaksperioder(eøsUttak);
        lagreGrunnlag(aktivtGrunnlag, nyttGrunnlag.build());
    }

    private void lagreGrunnlag(Optional<EøsUttakGrunnlagEntitet> aktivtGrunnlag, EøsUttakGrunnlagEntitet nyttGrunnlag) {
        // Deaktiver eksisterende grunnlag hvis det finnes
        aktivtGrunnlag.ifPresent(eksisterendeGrunnlag -> {
            eksisterendeGrunnlag.deaktiver();
            entityManager.persist(eksisterendeGrunnlag);
        });

        // Lagre nytt grunnlag
        var perioder = nyttGrunnlag.getSaksbehandlerPerioder();
        entityManager.persist(perioder);
        for (var entitet : perioder.getPerioder()) {
            entityManager.persist(entitet);
        }

        entityManager.persist(nyttGrunnlag);
        entityManager.flush();
    }





}
