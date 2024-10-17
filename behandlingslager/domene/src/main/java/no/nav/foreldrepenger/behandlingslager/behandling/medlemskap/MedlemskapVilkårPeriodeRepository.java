package no.nav.foreldrepenger.behandlingslager.behandling.medlemskap;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.RegisterdataDiffsjekker;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.vedtak.felles.jpa.HibernateVerktøy;

/**
 * Dette er et Repository for håndtering av alle persistente endringer i en søkers perioder for medlemskapvilkår
 * <p>
 * Merk: "standard" regler adoptert for "grunnlag" (ikke helt standard, ettersom vi her knytter
 * MedlemskapVilkårPeriodeGrunnlag til Vilkårsresultat i stedet for Behandling) - ett Grunnlag eies av ett
 * Vilkårsresultat. Et Aggregat (MedlemskapVilkårPeriodeGrunnlag-graf) har en selvstenig livssyklus og vil kopieres
 * ved hver endring.
 * Ved multiple endringer i et grunnlag for et MedlemskapVilkårPeriodeGrunnlag vil alltid kun et innslag i grunnlag
 * være aktiv for angitt Vilkårsresultat.
 */
@ApplicationScoped
public class MedlemskapVilkårPeriodeRepository {

    private EntityManager entityManager;

    public MedlemskapVilkårPeriodeRepository() {
        // FOR CDI
    }

    @Inject
    public MedlemskapVilkårPeriodeRepository( EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    private EntityManager getEntityManager() {
        Objects.requireNonNull(this.entityManager, "entityManager ikke satt");
        return this.entityManager;
    }

    public Optional<MedlemskapVilkårPeriodeGrunnlagEntitet> hentAggregatHvisEksisterer(Behandling behandling) {
        return hentAktivtGrunnlag(behandling);
    }

    public void kopierGrunnlagFraEksisterendeBehandling(Behandling eksisterendeBehandling, Behandling nyBehandling) {
        var eksisterendeGrunnlag = hentAktivtGrunnlag(eksisterendeBehandling);
        if (eksisterendeGrunnlag.isEmpty()) {
            return; // Intet å kopiere
        }
        var nyttGrunnlag = MedlemskapVilkårPeriodeGrunnlagEntitet.fra(eksisterendeGrunnlag, nyBehandling);
        var em = getEntityManager();
        em.persist(nyttGrunnlag);
        em.flush();
    }

    public MedlemskapVilkårPeriodeGrunnlagEntitet.Builder hentBuilderFor(Behandling behandling) {
        return opprettGrunnlagBuilderFor(behandling);
    }

    public void lagreMedlemskapsvilkår(Behandling behandling, MedlemskapVilkårPeriodeGrunnlagEntitet.Builder builder) {
        lagreOgFlush(behandling, builder.build());
    }

    public Optional<LocalDate> hentOpphørsdatoHvisEksisterer(Behandling behandling) {
        var periodeEntitet = hentAktivtGrunnlag(behandling)
                .map(MedlemskapVilkårPeriodeGrunnlagEntitet::getMedlemskapsvilkårPeriode);

        if (periodeEntitet.isPresent()) {
            var entitet = periodeEntitet.get();
            var overstyringOpt = entitet.getOverstyring();
            if (overstyringOpt.getOverstyringsdato().isPresent()) {
                if (overstyringOpt.getVilkårUtfall().equals(VilkårUtfallType.IKKE_OPPFYLT)) {
                    return overstyringOpt.getOverstyringsdato();
                }
                if (overstyringOpt.getVilkårUtfall().equals(VilkårUtfallType.OPPFYLT)) {
                    return Optional.empty();
                }
            }
        }
        return periodeEntitet
                .map(MedlemskapsvilkårPeriodeEntitet::getPerioder)
                .flatMap(perioder -> perioder.stream().filter(p -> VilkårUtfallType.IKKE_OPPFYLT.equals(p.getVilkårUtfall()))
                    .map(MedlemskapsvilkårPerioderEntitet::getVurderingsdato).findFirst());
    }

    private MedlemskapVilkårPeriodeGrunnlagEntitet.Builder opprettGrunnlagBuilderFor(Behandling behandling) {
        var aggregat = hentAktivtGrunnlag(behandling);
        if (aggregat.isPresent()) {
            return MedlemskapVilkårPeriodeGrunnlagEntitet.Builder.oppdatere(Optional.of(new MedlemskapVilkårPeriodeGrunnlagEntitet(aggregat.get())));
        }
        return MedlemskapVilkårPeriodeGrunnlagEntitet.Builder.oppdatere(Optional.empty());
    }

    private Optional<MedlemskapVilkårPeriodeGrunnlagEntitet> hentAktivtGrunnlag(Behandling behandling) {
        var vilkårResultat = Optional.ofNullable(getBehandlingsresultat(behandling))
                .map(Behandlingsresultat::getVilkårResultat)
                .orElse(null);
        if (vilkårResultat == null) {
            return Optional.empty();
        }
        var query = entityManager.createQuery("FROM MedlemskapVilkårPeriodeGrunnlag gr " +
                "WHERE gr.vilkårResultat.id = :vilkar_res_id " +
                "AND gr.aktiv = :aktivt", MedlemskapVilkårPeriodeGrunnlagEntitet.class);
        query.setParameter("vilkar_res_id", vilkårResultat.getId());
        query.setParameter("aktivt", true);
        return HibernateVerktøy.hentUniktResultat(query);
    }

    private Behandlingsresultat getBehandlingsresultat(Behandling behandling) {
        return behandling.getBehandlingsresultat();
    }

    private void lagreOgFlush(Behandling behandling, MedlemskapVilkårPeriodeGrunnlagEntitet nyttGrunnlag) {
        var eksisterendeGrunnlag = hentAktivtGrunnlag(behandling);
        var nyGrunnlagEntitet = tilGrunnlagEntitet(getBehandlingsresultat(behandling).getVilkårResultat(), nyttGrunnlag);
        if (eksisterendeGrunnlag.isPresent()) {
            if (!erEndret(eksisterendeGrunnlag.get(), nyGrunnlagEntitet, true)) {
                return;
            }
            var eksisterendeGrunnlagEntitet = eksisterendeGrunnlag.get();
            eksisterendeGrunnlagEntitet.setAktiv(false);
            entityManager.persist(eksisterendeGrunnlagEntitet);
            entityManager.flush();
        }
        lagreGrunnlag(getBehandlingsresultat(behandling).getVilkårResultat(), nyttGrunnlag);
        entityManager.flush();
    }

    public boolean erEndret(MedlemskapVilkårPeriodeGrunnlagEntitet grunnlag1, MedlemskapVilkårPeriodeGrunnlagEntitet grunnlag2,
                            boolean onlyCheckTrackedFields) {
        var diff = new RegisterdataDiffsjekker(onlyCheckTrackedFields).getDiffEntity().diff(grunnlag1, grunnlag2);
        return !diff.isEmpty();
    }

    private void lagreGrunnlag(VilkårResultat vilkårResultat, MedlemskapVilkårPeriodeGrunnlagEntitet aggregat) {
        var grunnlag = tilGrunnlagEntitet(vilkårResultat, aggregat);

        entityManager.persist(grunnlag.getMedlemskapsvilkårPeriode());
        grunnlag.getMedlemskapsvilkårPeriode().getPerioder().forEach(periode -> {
            periode.setRot(grunnlag.getMedlemskapsvilkårPeriode());
            entityManager.persist(periode);
        });
        entityManager.persist(grunnlag);
    }

    private MedlemskapVilkårPeriodeGrunnlagEntitet tilGrunnlagEntitet(VilkårResultat vilkårResultat, MedlemskapVilkårPeriodeGrunnlagEntitet grunnlag) {
        var grunnlagEntitet = new MedlemskapVilkårPeriodeGrunnlagEntitet();
        grunnlagEntitet.setVilkårResultat(vilkårResultat);
        grunnlagEntitet.setMedlemskapsvilkårPeriode(grunnlag.getMedlemskapsvilkårPeriode());
        return grunnlagEntitet;
    }
}
