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

    public void deaktiverAktivtGrunnlagHvisFinnes(Long behandlingId) {
        var aktivtGrunnlagOpt = hentGrunnlag(behandlingId);
        if (aktivtGrunnlagOpt.isEmpty()) {
            return;
        }
        var aktivtGrunnlag = aktivtGrunnlagOpt.get();
        aktivtGrunnlag.deaktiver();
        entityManager.persist(aktivtGrunnlag);
        entityManager.flush();
    }

    public void lagreEøsUttak(Long behandlingId, EøsUttaksperioderEntitet eøsUttak) {
        var aktivtGrunnlag = hentGrunnlag(behandlingId);
        var nyttGrunnlag = EøsUttakGrunnlagEntitet.Builder.ny()
            .medBehandlingId(behandlingId)
            .medEøsUttaksperioder(eøsUttak);
        lagreGrunnlag(aktivtGrunnlag, nyttGrunnlag.build());
    }

    public void kopierGrunnlagFraEksisterendeBehandling(Long originalBehandlingId, Long nyBehandlingId) {
        var eksisterendeGrunnlagOpt = hentGrunnlag(originalBehandlingId);
        if (eksisterendeGrunnlagOpt.isEmpty()) {
            return;
        }
        var eksisterendeGrunnlag = eksisterendeGrunnlagOpt.get();
        var nyttGrunnlag = EøsUttakGrunnlagEntitet.Builder.ny()
            .medEøsUttaksperioder(eksisterendeGrunnlag.getSaksbehandlerPerioder())
            .medBehandlingId(nyBehandlingId)
            .build();
        lagreNyttGrunnlag(nyttGrunnlag);
    }

    private void lagreGrunnlag(Optional<EøsUttakGrunnlagEntitet> aktivtGrunnlag, EøsUttakGrunnlagEntitet nyttGrunnlag) {
        // Deaktiver eksisterende grunnlag hvis det finnes
        aktivtGrunnlag.ifPresent(eksisterendeGrunnlag -> {
            eksisterendeGrunnlag.deaktiver();
            entityManager.persist(eksisterendeGrunnlag);
        });
        lagreNyttGrunnlag(nyttGrunnlag);
        entityManager.flush();
    }

    private void lagreNyttGrunnlag(EøsUttakGrunnlagEntitet nyttGrunnlag) {
        var perioder = nyttGrunnlag.getSaksbehandlerPerioder();
        entityManager.persist(perioder);
        for (var entitet : perioder.getPerioder()) {
            entityManager.persist(entitet);
        }

        entityManager.persist(nyttGrunnlag);
    }
}
