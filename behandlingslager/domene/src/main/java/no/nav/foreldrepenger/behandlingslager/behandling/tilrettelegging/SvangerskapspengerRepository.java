package no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.vedtak.felles.jpa.HibernateVerktøy;

@ApplicationScoped
public class SvangerskapspengerRepository {

    private EntityManager entityManager;

    @Inject
    public SvangerskapspengerRepository( EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager");
        this.entityManager = entityManager;
    }

    SvangerskapspengerRepository() {
        // CDI proxy
    }

    public void lagreOgFlush(SvpGrunnlagEntitet svpGrunnlag) {
        var eksisterendeGrunnlag = hentGrunnlag(svpGrunnlag.getBehandlingId());
        if (eksisterendeGrunnlag.isPresent()) {
            var eksisterendeEntitet = eksisterendeGrunnlag.get();
            eksisterendeEntitet.deaktiver();
            entityManager.persist(eksisterendeEntitet);
        }
        entityManager.persist(svpGrunnlag);
        entityManager.flush();
    }

    public Optional<SvpGrunnlagEntitet> hentGrunnlag(Long behandlingId) {
        var query = entityManager.createQuery("FROM SvpGrunnlag s " + "WHERE s.behandlingId = :behandlingId AND s.aktiv = true",
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

    public void tømmeOverstyrtGrunnlag(Long behandlingId) {
        var grunnlagOpt = hentGrunnlag(behandlingId);
        SvpGrunnlagEntitet.Builder nyBuilder;

        if (grunnlagOpt.isPresent()) {
            nyBuilder = new SvpGrunnlagEntitet.Builder(grunnlagOpt.get())
                .medOverstyrteTilrettelegginger(null);
            lagreOgFlush(nyBuilder.build());
        }
    }

    public void kopierSvpGrunnlagFraEksisterendeBehandling(Long orginalBehandlingId, Behandling nyBehandling) {
        var kopiGjeldendeGrunnlag = hentGrunnlag(orginalBehandlingId)
            .map(SvpGrunnlagEntitet::getGjeldendeVersjon)
            .map(SvpTilretteleggingerEntitet::getTilretteleggingListe)
            .orElse(Collections.emptyList()).stream()
            .map(ot -> new SvpTilretteleggingEntitet.Builder(ot)
                .medKopiertFraTidligereBehandling(true)
                .medTilretteleggingFraDatoer(fraDatoerMedKildeTidligereVedtak(ot)).build())
            .toList();

        if(!kopiGjeldendeGrunnlag.isEmpty()) {
            var nyttGrunnlag = new SvpGrunnlagEntitet.Builder().medBehandlingId(nyBehandling.getId())
                .medOpprinneligeTilrettelegginger(kopiGjeldendeGrunnlag)
                .build();
            lagreOgFlush(nyttGrunnlag);
        }
    }

    private List<TilretteleggingFOM> fraDatoerMedKildeTidligereVedtak(SvpTilretteleggingEntitet eksTilrettelegging) {
        return eksTilrettelegging.getTilretteleggingFOMListe().stream()
            .map(tf -> new TilretteleggingFOM.Builder().fraEksisterende(tf).medKilde(SvpTilretteleggingFomKilde.TIDLIGERE_VEDTAK)
                .build())
            .toList();
    }

    public void fjernOverstyrtGrunnlag(Long behandlingId) {
        var overstyrteTilrettelegginger = hentGrunnlag(behandlingId)
            .map(SvpGrunnlagEntitet::getOverstyrteTilrettelegginger);
        overstyrteTilrettelegginger.ifPresent(
            svpTilretteleggingerEntitet -> svpTilretteleggingerEntitet.getTilretteleggingListe().forEach(entityManager::remove));
    }
}
