package no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.vedtak.felles.jpa.HibernateVerktøy;
import no.nav.vedtak.felles.jpa.VLPersistenceUnit;

@ApplicationScoped
public class SvangerskapspengerRepository {

    private EntityManager entityManager;

    @Inject
    public SvangerskapspengerRepository(@VLPersistenceUnit EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
    }

    SvangerskapspengerRepository() {
        // CDI proxy
    }

    public void lagreOgFlush(SvpGrunnlagEntitet svpGrunnlag) {
        final Optional<SvpGrunnlagEntitet> eksisterendeGrunnlag = hentGrunnlag(svpGrunnlag.getBehandlingId());
        if (eksisterendeGrunnlag.isPresent()) {
            SvpGrunnlagEntitet eksisterendeEntitet = eksisterendeGrunnlag.get();
            eksisterendeEntitet.deaktiver();
            entityManager.persist(eksisterendeEntitet);
        }
        entityManager.persist(svpGrunnlag);
        entityManager.flush();
    }

    public Optional<SvpGrunnlagEntitet> hentGrunnlag(Long behandlingId) {
        final TypedQuery<SvpGrunnlagEntitet> query = entityManager.createQuery(
            "FROM SvpGrunnlag s " +
                    "WHERE s.behandlingId = :behandlingId AND s.aktiv = true",
                    SvpGrunnlagEntitet.class);

        query.setParameter("behandlingId", behandlingId);

        return HibernateVerktøy.hentUniktResultat(query);
    }

    public void lagreOverstyrtGrunnlag(Behandling behandling, List<SvpTilretteleggingEntitet> overstyrtTilrettelegging) {
        Optional<SvpGrunnlagEntitet> grunnlagOpt = hentGrunnlag(behandling.getId());
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
        Optional<SvpGrunnlagEntitet> eksisterendeGrunnlag = hentGrunnlag(orginalBehandlingId);
        if (eksisterendeGrunnlag.isPresent()) {
            SvpGrunnlagEntitet eksisterendeEntitet = eksisterendeGrunnlag.get();
            List<SvpTilretteleggingEntitet> opprTilrettelegginger = opprettKopierAvTilrettelegginger(eksisterendeEntitet.getOpprinneligeTilrettelegginger());
            List<SvpTilretteleggingEntitet> ovstTilrettelegginger = opprettKopierAvTilrettelegginger(eksisterendeEntitet.getOverstyrteTilrettelegginger());
            SvpGrunnlagEntitet nyttGrunnlag = new SvpGrunnlagEntitet.Builder()
                    .medBehandlingId(nyBehandling.getId())
                    .medOpprinneligeTilrettelegginger(opprTilrettelegginger)
                    .medOverstyrteTilrettelegginger(ovstTilrettelegginger)
                    .build();
            lagreOgFlush(nyttGrunnlag);
        }
    }

    private List<SvpTilretteleggingEntitet> opprettKopierAvTilrettelegginger(SvpTilretteleggingerEntitet tilrettelegginger) {
        if (tilrettelegginger == null) {
            return Collections.emptyList();
        }
        return tilrettelegginger.getTilretteleggingListe().stream()
                .map(ot -> new SvpTilretteleggingEntitet.Builder(ot).medKopiertFraTidligereBehandling(true).build())
                .collect(Collectors.toList());
    }
}
