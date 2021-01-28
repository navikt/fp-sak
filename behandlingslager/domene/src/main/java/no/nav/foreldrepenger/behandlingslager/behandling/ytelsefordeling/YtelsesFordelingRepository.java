package no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import no.nav.foreldrepenger.behandlingslager.behandling.RegisterdataDiffsjekker;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.diff.DiffResult;
import no.nav.vedtak.felles.jpa.HibernateVerktøy;

@ApplicationScoped
public class YtelsesFordelingRepository {

    private EntityManager entityManager;

    protected YtelsesFordelingRepository() {
        // CDI
    }

    @Inject
    public YtelsesFordelingRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager");
        this.entityManager = entityManager;
    }

    public YtelseFordelingAggregat hentAggregat(Long behandlingId) {
        return hentAggregatHvisEksisterer(behandlingId).orElseThrow(
            () -> YtelseFordelingFeil.FACTORY.fantIkkeForventetGrunnlagPåBehandling(behandlingId).toException());

    }

    public Optional<YtelseFordelingAggregat> hentAggregatHvisEksisterer(Long behandlingId) {
        return hentAktivtGrunnlag(behandlingId).map(this::mapEntitetTilAggregat);
    }

    public void lagre(Long behandlingId, YtelseFordelingAggregat aggregat) {
        lagreOgFlush(behandlingId, aggregat);
    }

    /**
     * Gir builder for å oppdater ytelse fordeling.
     * Samspiller med bruk av {@link #lagre(Long, YtelseFordelingAggregat)}
     */
    public YtelseFordelingAggregat.Builder opprettBuilder(Long behandlingId) {
        var ytelseFordelingAggregat = hentAggregatHvisEksisterer(behandlingId);
        return YtelseFordelingAggregat.Builder.oppdatere(ytelseFordelingAggregat);
    }

    public YtelseFordelingAggregat hentYtelsesFordelingPåId(Long aggregatId) {
        return getVersjonAvYtelsesFordelingPåId(aggregatId).map(g -> mapEntitetTilAggregat(g)).orElseThrow();
    }

    private YtelseFordelingAggregat mapEntitetTilAggregat(YtelseFordelingGrunnlagEntitet ytelseFordelingGrunnlagEntitet) {
        return YtelseFordelingAggregat.Builder.nytt()
            .medOppgittDekningsgrad(ytelseFordelingGrunnlagEntitet.getOppgittDekningsgrad())
            .medOppgittRettighet(ytelseFordelingGrunnlagEntitet.getOppgittRettighet())
            .medPerioderUtenOmsorg(ytelseFordelingGrunnlagEntitet.getPerioderUtenOmsorg())
            .medPerioderAleneOmsorg(ytelseFordelingGrunnlagEntitet.getPerioderAleneOmsorgEntitet())
            .medOppgittFordeling(ytelseFordelingGrunnlagEntitet.getOppgittFordeling())
            .medJustertFordeling(ytelseFordelingGrunnlagEntitet.getJustertFordeling())
            .medOverstyrtFordeling(ytelseFordelingGrunnlagEntitet.getOverstyrtFordeling())
            .medPerioderUttakDokumentasjon(ytelseFordelingGrunnlagEntitet.getPerioderUttakDokumentasjon())
            .medAvklarteDatoer(ytelseFordelingGrunnlagEntitet.getAvklarteUttakDatoer())
            .medPerioderAnnenforelderHarRett(ytelseFordelingGrunnlagEntitet.getPerioderAnnenforelderHarRettEntitet())
            .medOpprinneligeAktivitetskravPerioder(ytelseFordelingGrunnlagEntitet.getOpprinneligeAktivitetskravPerioder())
            .medSaksbehandledeAktivitetskravPerioder(ytelseFordelingGrunnlagEntitet.getSaksbehandledeAktivitetskravPerioder())
            .build();
    }

    private Optional<YtelseFordelingGrunnlagEntitet> hentYtelseFordelingPåId(Long grunnlagId) {
        return getVersjonAvYtelsesFordelingPåId(grunnlagId);
    }

    private Optional<YtelseFordelingGrunnlagEntitet> hentAktivtGrunnlag(Long behandlingId) {
        Objects.requireNonNull(behandlingId, "behandlingId");
        final TypedQuery<YtelseFordelingGrunnlagEntitet> query = entityManager.createQuery(
            "FROM YtelseFordelingGrunnlag gr " + "WHERE gr.behandlingId = :behandlingId " + "AND gr.aktiv = :aktivt",
            YtelseFordelingGrunnlagEntitet.class);
        query.setParameter("behandlingId", behandlingId);
        query.setParameter("aktivt", true);
        return HibernateVerktøy.hentUniktResultat(query);
    }

    private void lagreOgFlush(Long behandlingId, YtelseFordelingAggregat aggregat) {
        final Optional<YtelseFordelingGrunnlagEntitet> eksisterendeGrunnlag = hentAktivtGrunnlag(behandlingId);
        YtelseFordelingGrunnlagEntitet nyGrunnlagEntitet = mapTilGrunnlagEntitet(behandlingId, aggregat);
        if (eksisterendeGrunnlag.isPresent()) {
            final YtelseFordelingGrunnlagEntitet eksisterendeGrunnlag1 = eksisterendeGrunnlag.get();
            eksisterendeGrunnlag1.setAktiv(false);
            entityManager.persist(eksisterendeGrunnlag1);
            entityManager.flush();
        }
        lagreGrunnlag(nyGrunnlagEntitet);
        entityManager.flush();
    }

    private void lagreGrunnlag(YtelseFordelingGrunnlagEntitet grunnlag) {

        if (grunnlag.getOppgittDekningsgrad() != null) {
            entityManager.persist(grunnlag.getOppgittDekningsgrad());
        }
        if (grunnlag.getOppgittRettighet() != null) {
            entityManager.persist(grunnlag.getOppgittRettighet());
        }
        if (grunnlag.getOppgittFordeling() != null) {
            entityManager.persist(grunnlag.getOppgittFordeling());
            lagrePeriode(grunnlag.getOppgittFordeling().getOppgittePerioder());
        }
        if (grunnlag.getJustertFordeling() != null) {
            entityManager.persist(grunnlag.getJustertFordeling());
            lagrePeriode(grunnlag.getJustertFordeling().getOppgittePerioder());
        }
        if (grunnlag.getOverstyrtFordeling() != null) {
            entityManager.persist(grunnlag.getOverstyrtFordeling());
            lagrePeriode(grunnlag.getOverstyrtFordeling().getOppgittePerioder());
        }
        if (grunnlag.getAvklarteUttakDatoer() != null) {
            entityManager.persist(grunnlag.getAvklarteUttakDatoer());
        }

        lagrePerioderAleneOmsorg(grunnlag);
        lagrePerioderUtenOmsorg(grunnlag);
        lagrePerioderUttakDokumentasjon(grunnlag);
        lagrePerioderAnnenforelderHarRett(grunnlag);
        lagrePerioderAktivitetskrav(grunnlag.getOpprinneligeAktivitetskravPerioder());
        lagrePerioderAktivitetskrav(grunnlag.getSaksbehandledeAktivitetskravPerioder());

        entityManager.persist(grunnlag);
    }

    private YtelseFordelingGrunnlagEntitet mapTilGrunnlagEntitet(Long behandlingId, YtelseFordelingAggregat aggregat) {
        final YtelseFordelingGrunnlagEntitet grunnlag = new YtelseFordelingGrunnlagEntitet();
        grunnlag.setBehandling(behandlingId);
        grunnlag.setOppgittDekningsgrad(aggregat.getOppgittDekningsgrad());
        grunnlag.setOppgittRettighet(aggregat.getOppgittRettighet());
        grunnlag.setOppgittFordeling(aggregat.getOppgittFordeling());
        aggregat.getPerioderUttakDokumentasjon().ifPresent(grunnlag::setPerioderUttakDokumentasjon);
        aggregat.getPerioderUtenOmsorg().ifPresent(grunnlag::setPerioderUtenOmsorg);
        aggregat.getPerioderAleneOmsorg().ifPresent(grunnlag::setPerioderAleneOmsorg);
        aggregat.getOverstyrtFordeling().ifPresent(grunnlag::setOverstyrtFordeling);
        aggregat.getJustertFordeling().ifPresent(grunnlag::setJustertFordeling);
        aggregat.getAvklarteDatoer().ifPresent(grunnlag::setAvklarteUttakDatoerEntitet);
        aggregat.getPerioderAnnenforelderHarRett().ifPresent(grunnlag::setPerioderAnnenforelderHarRettEntitet);
        aggregat.getOpprinneligeAktivitetskravPerioder().ifPresent(grunnlag::setOpprinneligeAktivitetskravPerioder);
        aggregat.getSaksbehandledeAktivitetskravPerioder().ifPresent(grunnlag::setSaksbehandledeAktivitetskravPerioder);
        return grunnlag;
    }

    private void lagrePerioderUttakDokumentasjon(YtelseFordelingGrunnlagEntitet grunnlag) {
        if (grunnlag.getPerioderUttakDokumentasjon() != null) {
            entityManager.persist(grunnlag.getPerioderUttakDokumentasjon());
            for (PeriodeUttakDokumentasjonEntitet periode : grunnlag.getPerioderUttakDokumentasjon().getPerioder()) {
                entityManager.persist(periode);
            }
        }
    }

    private void lagrePerioderUtenOmsorg(YtelseFordelingGrunnlagEntitet grunnlag) {
        if (grunnlag.getPerioderUtenOmsorg() != null) {
            entityManager.persist(grunnlag.getPerioderUtenOmsorg());
            for (PeriodeUtenOmsorgEntitet periode : grunnlag.getPerioderUtenOmsorg().getPerioder()) {
                entityManager.persist(periode);
            }
        }
    }

    private void lagrePerioderAleneOmsorg(YtelseFordelingGrunnlagEntitet grunnlag) {
        if (grunnlag.getPerioderAleneOmsorgEntitet() != null) {
            entityManager.persist(grunnlag.getPerioderAleneOmsorgEntitet());
            for (PeriodeAleneOmsorgEntitet periode : grunnlag.getPerioderAleneOmsorgEntitet().getPerioder()) {
                entityManager.persist(periode);
            }
        }
    }

    private void lagrePerioderAnnenforelderHarRett(YtelseFordelingGrunnlagEntitet grunnlag) {
        if (grunnlag.getPerioderAnnenforelderHarRettEntitet() != null) {
            entityManager.persist(grunnlag.getPerioderAnnenforelderHarRettEntitet());
            for (PeriodeAnnenforelderHarRettEntitet periode : grunnlag.getPerioderAnnenforelderHarRettEntitet()
                .getPerioder()) {
                entityManager.persist(periode);
            }
        }
    }

    private void lagrePerioderAktivitetskrav(AktivitetskravPerioderEntitet perioder) {
        if (perioder != null) {
            entityManager.persist(perioder);
            for (AktivitetskravPeriodeEntitet periode : perioder.getPerioder()) {
                entityManager.persist(periode);
            }
        }
    }

    private void lagrePeriode(List<OppgittPeriodeEntitet> perioder) {
        for (OppgittPeriodeEntitet oppgittPeriode : perioder) {
            entityManager.persist(oppgittPeriode);
        }
    }

    /**
     * Kopierer grunnlag fra en tidligere behandling. Endrer ikke aggregater, en
     * skaper nye referanser til disse.
     */
    public void kopierGrunnlagFraEksisterendeBehandling(Long gammelBehandlingId, Long nyBehandlingId) {
        Optional<YtelseFordelingAggregat> origAggregat = hentAggregatHvisEksisterer(gammelBehandlingId);
        origAggregat.ifPresent(ytelseFordelingAggregat -> {
            lagreOgFlush(nyBehandlingId, ytelseFordelingAggregat);
        });
    }

    public Optional<Long> hentIdPåAktivYtelsesFordeling(Long behandlingId) {
        return hentAktivtGrunnlag(behandlingId).map(YtelseFordelingGrunnlagEntitet::getId);
    }

    /**
     * Tiltenkt ved inngangen til revurdering
     */
    public void tilbakestillFordeling(Long behandlingId) {
        YtelseFordelingAggregat ytelseFordeling = opprettBuilder(behandlingId).medAvklarteDatoer(null)
            .medJustertFordeling(null)
            .medPerioderUttakDokumentasjon(null)
            .medOverstyrtFordeling(null)
            .build();
        lagreOgFlush(behandlingId, ytelseFordeling);
    }

    public DiffResult diffResultat(Long grunnlagId1, Long grunnlagId2, boolean onlyCheckTrackedFields) {
        YtelseFordelingGrunnlagEntitet grunnlag1 = hentYtelseFordelingPåId(grunnlagId1).orElseThrow(
            () -> new IllegalStateException("GrunnlagId1 må være oppgitt"));
        YtelseFordelingGrunnlagEntitet grunnlag2 = hentYtelseFordelingPåId(grunnlagId2).orElseThrow(
            () -> new IllegalStateException("GrunnlagId2 må være oppgitt"));
        return new RegisterdataDiffsjekker(onlyCheckTrackedFields).getDiffEntity().diff(grunnlag1, grunnlag2);
    }

    private Optional<YtelseFordelingGrunnlagEntitet> getVersjonAvYtelsesFordelingPåId(Long aggregatId) {
        Objects.requireNonNull(aggregatId, "aggregatId");
        final TypedQuery<YtelseFordelingGrunnlagEntitet> query = entityManager.createQuery(
            "FROM YtelseFordelingGrunnlag gr " + "WHERE gr.id = :aggregatId ", YtelseFordelingGrunnlagEntitet.class);
        query.setParameter("aggregatId", aggregatId);
        return HibernateVerktøy.hentUniktResultat(query);
    }
}
