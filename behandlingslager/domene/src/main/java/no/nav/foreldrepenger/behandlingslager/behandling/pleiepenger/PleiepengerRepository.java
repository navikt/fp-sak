package no.nav.foreldrepenger.behandlingslager.behandling.pleiepenger;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

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
            .medBehandlingId(behandlingId)
            .medInnleggelsePerioder(builder);

        lagreGrunnlag(aktivtGrunnlag, nyttGrunnlag.build());
    }

    private void lagreGrunnlag(Optional<PleiepengerGrunnlagEntitet> aktivtGrunnlag, PleiepengerGrunnlagEntitet nyttGrunnlag) {
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
                    PleiepengerGrunnlagEntitet.class)
            .setParameter("behandlingId", behandlingId);

        return HibernateVerktøy.hentUniktResultat(query);
    }

    public void reaktiverDerSisteGrunnlagErPassiv() {
        final var query = entityManager.createQuery(
                "FROM PleiepengerGrunnlag p ",
                PleiepengerGrunnlagEntitet.class);

        query.getResultList().stream()
            .collect(Collectors.groupingBy(PleiepengerGrunnlagEntitet::getBehandlingId))
            .forEach((key, value) -> {
                if (value.stream().noneMatch(PleiepengerGrunnlagEntitet::isAktiv)) {
                    var last = value.stream().max(Comparator.comparing(PleiepengerGrunnlagEntitet::getOpprettetTidspunkt));
                    last.ifPresent(gpe -> {
                        gpe.reaktiver();
                        entityManager.persist(gpe);
                    });
            }
        });
    }



    public void kopierGrunnlagFraEksisterendeBehandling(Long orginalBehandlingId, Long nyBehandlingId) {
        var eksisterendeGrunnlag = hentGrunnlag(orginalBehandlingId);
        var innleggelser = eksisterendeGrunnlag.flatMap(PleiepengerGrunnlagEntitet::getPerioderMedInnleggelse)
            .map(PleiepengerPerioderEntitet::getInnleggelser).orElse(List.of());
        if (!innleggelser.isEmpty()) {
            var nyttGrunnlag = PleiepengerGrunnlagEntitet.Builder.oppdatere(eksisterendeGrunnlag)
                .medBehandlingId(nyBehandlingId);
            lagreGrunnlag(Optional.empty(), nyttGrunnlag.build());
        }
    }

}
