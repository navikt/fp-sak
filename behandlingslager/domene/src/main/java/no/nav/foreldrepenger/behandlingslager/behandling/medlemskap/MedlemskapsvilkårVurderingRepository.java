package no.nav.foreldrepenger.behandlingslager.behandling.medlemskap;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.vedtak.felles.jpa.HibernateVerktøy;

@ApplicationScoped
public class MedlemskapsvilkårVurderingRepository {

    private EntityManager entityManager;
    private BehandlingsresultatRepository behandlingsresultatRepository;

    @Inject
    public MedlemskapsvilkårVurderingRepository(EntityManager entityManager, BehandlingsresultatRepository behandlingsresultatRepository) {
        this.entityManager = entityManager;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
    }

    MedlemskapsvilkårVurderingRepository() {
        //CDI
    }

    public void lagre(MedlemskapsvilkårVurderingEntitet medlemskapsvilkårVurderingEntitet) {
        slettFor(medlemskapsvilkårVurderingEntitet.getVilkårResultat());
        entityManager.persist(medlemskapsvilkårVurderingEntitet);
        entityManager.flush();
    }

    public void slettFor(VilkårResultat vilkårResultat) {
        hentHvisEksisterer(vilkårResultat).ifPresent(eksisterende -> {
            eksisterende.setAktiv(false);
            entityManager.persist(eksisterende);
            entityManager.flush();
        });
    }

    public MedlemskapsvilkårVurderingEntitet hent(long behandlingId) {
        return hentHvisEksisterer(behandlingId).orElseThrow();
    }

    public Optional<MedlemskapsvilkårVurderingEntitet> hentHvisEksisterer(long behandlingId) {
        return behandlingsresultatRepository.hentHvisEksisterer(behandlingId).flatMap(br -> hentHvisEksisterer(br.getVilkårResultat()));
    }

    public void kopierGrunnlagFraEksisterendeBehandling(Behandling origBehandling, Behandling nyBehandling) {
        var eksisterendeGrunnlag = hentHvisEksisterer(origBehandling.getId());
        if (eksisterendeGrunnlag.isEmpty()) {
            return;
        }
        var nyttVilkårResultat = behandlingsresultatRepository.hent(nyBehandling.getId()).getVilkårResultat();
        var nyttGrunnlag = new MedlemskapsvilkårVurderingEntitet(nyttVilkårResultat,
            eksisterendeGrunnlag.get().getOpphør().orElse(null),
            eksisterendeGrunnlag.get().getMedlemFom().orElse(null));
        entityManager.persist(nyttGrunnlag);
        entityManager.flush();
    }

    private Optional<MedlemskapsvilkårVurderingEntitet> hentHvisEksisterer(VilkårResultat vilkårResultat) {
        var query = entityManager.createQuery("""
            FROM MedlemskapsvilkårVurdering v
                        WHERE v.vilkårResultat.id = :vilkar_res_id
                        AND v.aktiv = :aktiv
            """, MedlemskapsvilkårVurderingEntitet.class);
        query.setParameter("vilkar_res_id", vilkårResultat.getId());
        query.setParameter("aktiv", true);
        return HibernateVerktøy.hentUniktResultat(query);
    }
}
