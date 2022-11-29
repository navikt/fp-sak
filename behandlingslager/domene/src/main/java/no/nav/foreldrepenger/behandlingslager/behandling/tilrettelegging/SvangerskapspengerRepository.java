package no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.vedtak.felles.jpa.HibernateVerktøy;

@ApplicationScoped
public class SvangerskapspengerRepository {

    private EntityManager entityManager;

    @Inject
    public SvangerskapspengerRepository( EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
    }

    SvangerskapspengerRepository() {
        // CDI proxy
    }

    public void lagreOgFlush(SvpGrunnlagEntitet svpGrunnlag) {
        final var eksisterendeGrunnlag = hentGrunnlag(svpGrunnlag.getBehandlingId());
        if (eksisterendeGrunnlag.isPresent()) {
            var eksisterendeEntitet = eksisterendeGrunnlag.get();
            eksisterendeEntitet.deaktiver();
            entityManager.persist(eksisterendeEntitet);
        }
        entityManager.persist(svpGrunnlag);
        entityManager.flush();
    }

    public Optional<SvpGrunnlagEntitet> hentGrunnlag(Long behandlingId) {
        final var query = entityManager.createQuery(
            "FROM SvpGrunnlag s " +
                    "WHERE s.behandlingId = :behandlingId AND s.aktiv = true",
                    SvpGrunnlagEntitet.class);

        query.setParameter("behandlingId", behandlingId);

        return HibernateVerktøy.hentUniktResultat(query);
    }

    public void lagreOverstyrtGrunnlag(Behandling behandling, List<SvpTilretteleggingEntitet> overstyrtTilrettelegging) {
        var grunnlagOpt = hentGrunnlag(behandling.getId());
        SvpGrunnlagEntitet.Builder nyBuilder;

        if (grunnlagOpt.isPresent()) {
            nyBuilder = new SvpGrunnlagEntitet.Builder(grunnlagOpt.get())
                    .medOverstyrteTilrettelegginger(overstyrtTilrettelegging);

        } else {
            nyBuilder = new SvpGrunnlagEntitet.Builder()
                    .medBehandlingId(behandling.getId())
                    .medOverstyrteTilrettelegginger(overstyrtTilrettelegging);
        }

        lagreOgFlush(nyBuilder.build());
    }

    public void kopierSvpGrunnlagFraEksisterendeBehandling(Long orginalBehandlingId, Behandling nyBehandling) {
        var kopiGjeldendeGrunnlag = hentGrunnlag(orginalBehandlingId)
            .map(SvpGrunnlagEntitet::getGjeldendeVersjon)
            .map(SvpTilretteleggingerEntitet::getTilretteleggingListe)
            .orElse(Collections.emptyList()).stream()
            .map(ot -> new SvpTilretteleggingEntitet.Builder(ot).medKopiertFraTidligereBehandling(true).build())
            .toList();

            if(!kopiGjeldendeGrunnlag.isEmpty()) {
                var nyttGrunnlag = new SvpGrunnlagEntitet.Builder().medBehandlingId(nyBehandling.getId())
                    .medOpprinneligeTilrettelegginger(kopiGjeldendeGrunnlag)
                    .build();
                lagreOgFlush(nyttGrunnlag);
            }
        }

}
