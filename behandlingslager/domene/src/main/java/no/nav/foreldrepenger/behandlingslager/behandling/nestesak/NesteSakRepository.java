package no.nav.foreldrepenger.behandlingslager.behandling.nestesak;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.vedtak.felles.jpa.HibernateVerktøy;

@ApplicationScoped
public class NesteSakRepository {

    private EntityManager entityManager;

    @Inject
    public NesteSakRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
    }

    NesteSakRepository() {
        // CDI proxy
    }

    public void lagreNesteSak(Long behandlingId, Saksnummer saksnummer, LocalDate startdato, LocalDate hendelsedato) {
        var aktivtGrunnlag = hentGrunnlag(behandlingId);
        var nyttGrunnlag = NesteSakGrunnlagEntitet.Builder.oppdatere(aktivtGrunnlag)
            .medBehandlingId(behandlingId)
            .medSaksnummer(saksnummer)
            .medStartdato(startdato)
            .medHendelsedato(hendelsedato);
        lagreGrunnlag(aktivtGrunnlag, nyttGrunnlag);
    }

    public void fjernEventuellNesteSak(Long behandlingId) {
        hentGrunnlag(behandlingId).ifPresent(eksisterendeGrunnlag -> {
            eksisterendeGrunnlag.deaktiver();
            entityManager.persist(eksisterendeGrunnlag);
        });
    }

    private void lagreGrunnlag(Optional<NesteSakGrunnlagEntitet> aktivtGrunnlag, NesteSakGrunnlagEntitet.Builder builder) {
        var nyttGrunnlag = builder.build();

        if (!Objects.equals(aktivtGrunnlag.orElse(null), nyttGrunnlag)) {
            aktivtGrunnlag.ifPresent(eksisterendeGrunnlag -> {
                eksisterendeGrunnlag.deaktiver();
                entityManager.persist(eksisterendeGrunnlag);
            });
            entityManager.persist(nyttGrunnlag);
            entityManager.flush();
        }
    }

    public Optional<NesteSakGrunnlagEntitet> hentGrunnlag(Long behandlingId) {
        final var query = entityManager.createQuery(
            "FROM NestesakGrunnlag n WHERE n.behandlingId = :behandlingId AND n.aktiv = true",
                NesteSakGrunnlagEntitet.class)
            .setParameter("behandlingId", behandlingId);

        return HibernateVerktøy.hentUniktResultat(query);
    }

    public NesteSakGrunnlagEntitet hentGrunnlagPåId(Long grunnlagId) {
        final var query = entityManager
            .createQuery("FROM NestesakGrunnlag n WHERE n.id = :grunnlagId", NesteSakGrunnlagEntitet.class)
            .setParameter("grunnlagId", grunnlagId);

        return query.getResultStream().findFirst().orElse(null);
    }

    public void kopierGrunnlagFraEksisterendeBehandling(Long orginalBehandlingId, Long nyBehandlingId) {
        var eksisterendeGrunnlag = hentGrunnlag(orginalBehandlingId);
        eksisterendeGrunnlag.ifPresent(g -> {
            var nyttgrunnlag = NesteSakGrunnlagEntitet.Builder.oppdatere(eksisterendeGrunnlag)
                .medBehandlingId(nyBehandlingId);
            lagreGrunnlag(Optional.empty(), nyttgrunnlag);
        });
    }

}
