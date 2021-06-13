package no.nav.foreldrepenger.behandlingslager.behandling.pleiepenger;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import no.nav.vedtak.felles.jpa.HibernateVerktøy;

@ApplicationScoped
public class PleiepengerRepository {

    private EntityManager entityManager;

    @Inject
    public PleiepengerRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
    }

    PleiepengerRepository() {
        // CDI proxy
    }

    public void lagrePerioder(Long behandlingId, PleiepengerPerioderEntitet.Builder builder) {

        var aktivtGrunnlag = hentGrunnlag(behandlingId);
        var nyttGrunnlag = PleiepengerGrunnlagEntitet.Builder.oppdatere(aktivtGrunnlag)
            .medInnleggelsePerioder(builder);

        lagreGrunnlag(behandlingId, aktivtGrunnlag, nyttGrunnlag);
    }

    private void lagreGrunnlag(Long behandlingId, Optional<PleiepengerGrunnlagEntitet> aktivtGrunnlag, PleiepengerGrunnlagEntitet.Builder builder) {
        var nyttGrunnlag = builder.build();
        nyttGrunnlag.setBehandlingId(behandlingId);

        if (!Objects.equals(aktivtGrunnlag.orElse(null), nyttGrunnlag)) {
            aktivtGrunnlag.ifPresent(eksisterendeGrunnlag -> {
                eksisterendeGrunnlag.deaktiver();
                entityManager.persist(eksisterendeGrunnlag);
            });
            nyttGrunnlag.getPerioderMedInnleggelse().ifPresent(perioder -> {
                entityManager.persist(perioder);
                for (var entitet : perioder.getInnleggelser()) {
                    entityManager.persist(entitet);
                }
            });
            entityManager.persist(nyttGrunnlag);
            entityManager.flush();
        }
    }

    public Optional<PleiepengerGrunnlagEntitet> hentGrunnlag(Long behandlingId) {
        final var query = entityManager.createQuery(
            "FROM PleiepengerGrunnlag p WHERE p.behandlingId = :behandlingId AND p.aktiv = true",
                    PleiepengerGrunnlagEntitet.class);

        query.setParameter("behandlingId", behandlingId);

        return HibernateVerktøy.hentUniktResultat(query);
    }

    public void kopierGrunnlagFraEksisterendeBehandling(Long orginalBehandlingId, Long nyBehandlingId) {
        var eksisterendeGrunnlag = hentGrunnlag(orginalBehandlingId);
        var innleggelser = eksisterendeGrunnlag.flatMap(PleiepengerGrunnlagEntitet::getPerioderMedInnleggelse)
            .map(PleiepengerPerioderEntitet::getInnleggelser).orElse(List.of());
        if (!innleggelser.isEmpty()) {
            var nyttGrunnlag = PleiepengerGrunnlagEntitet.Builder.oppdatere(eksisterendeGrunnlag);
            lagreGrunnlag(nyBehandlingId, eksisterendeGrunnlag, nyttGrunnlag);
        }
    }

}
