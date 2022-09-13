package no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.SpesialBehandling;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.jpa.HibernateVerktøy;

import static no.nav.foreldrepenger.behandlingslager.behandling.SpesialBehandling.erBerørtBehandling;

@ApplicationScoped
public class YtelsesFordelingRepository {

    private EntityManager entityManager;

    @Inject
    public YtelsesFordelingRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager");
        this.entityManager = entityManager;
    }

    protected YtelsesFordelingRepository() {
        // CDI
    }

    public YtelseFordelingAggregat hentAggregat(Long behandlingId) {
        return hentAggregatHvisEksisterer(behandlingId).orElseThrow(() -> new TekniskException("FP-634781",
                    "Fant ikke forventet YtelseFordeling grunnlag for behandling med id " + behandlingId));

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
        return hentGrunnlagPåId(aggregatId).map(g -> mapEntitetTilAggregat(g)).orElseThrow();
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
            .medPerioderAnnenForelderRettEØS(ytelseFordelingGrunnlagEntitet.getPerioderAnnenForelderRettEØSEntitet())
            .medOpprinneligeAktivitetskravPerioder(
                ytelseFordelingGrunnlagEntitet.getOpprinneligeAktivitetskravPerioder())
            .medSaksbehandledeAktivitetskravPerioder(
                ytelseFordelingGrunnlagEntitet.getSaksbehandledeAktivitetskravPerioder())
            .build();
    }

    private Optional<YtelseFordelingGrunnlagEntitet> hentAktivtGrunnlag(Long behandlingId) {
        Objects.requireNonNull(behandlingId, "behandlingId");
        final var query = entityManager.createQuery(
            "FROM YtelseFordelingGrunnlag gr " + "WHERE gr.behandlingId = :behandlingId " + "AND gr.aktiv = :aktivt",
            YtelseFordelingGrunnlagEntitet.class);
        query.setParameter("behandlingId", behandlingId);
        query.setParameter("aktivt", true);
        return HibernateVerktøy.hentUniktResultat(query);
    }

    private void lagreOgFlush(Long behandlingId, YtelseFordelingAggregat aggregat) {
        final var eksisterendeGrunnlag = hentAktivtGrunnlag(behandlingId);
        var nyGrunnlagEntitet = mapTilGrunnlagEntitet(behandlingId, aggregat);
        if (eksisterendeGrunnlag.isPresent()) {
            final var eksisterendeGrunnlag1 = eksisterendeGrunnlag.get();
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
        lagrePerioderAnnenforelderRettEØS(grunnlag);
        lagrePerioderAktivitetskrav(grunnlag.getOpprinneligeAktivitetskravPerioder());
        lagrePerioderAktivitetskrav(grunnlag.getSaksbehandledeAktivitetskravPerioder());

        entityManager.persist(grunnlag);
    }

    private YtelseFordelingGrunnlagEntitet mapTilGrunnlagEntitet(Long behandlingId, YtelseFordelingAggregat aggregat) {
        final var grunnlag = new YtelseFordelingGrunnlagEntitet();
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
        aggregat.getPerioderAnnenForelderRettEØS().ifPresent(grunnlag::setPerioderAnnenForelderRettEØSEntitet);
        aggregat.getOpprinneligeAktivitetskravPerioder().ifPresent(grunnlag::setOpprinneligeAktivitetskravPerioder);
        aggregat.getSaksbehandledeAktivitetskravPerioder().ifPresent(grunnlag::setSaksbehandledeAktivitetskravPerioder);
        return grunnlag;
    }

    private void lagrePerioderUttakDokumentasjon(YtelseFordelingGrunnlagEntitet grunnlag) {
        if (grunnlag.getPerioderUttakDokumentasjon() != null) {
            entityManager.persist(grunnlag.getPerioderUttakDokumentasjon());
            for (var periode : grunnlag.getPerioderUttakDokumentasjon().getPerioder()) {
                entityManager.persist(periode);
            }
        }
    }

    private void lagrePerioderUtenOmsorg(YtelseFordelingGrunnlagEntitet grunnlag) {
        if (grunnlag.getPerioderUtenOmsorg() != null) {
            entityManager.persist(grunnlag.getPerioderUtenOmsorg());
            for (var periode : grunnlag.getPerioderUtenOmsorg().getPerioder()) {
                entityManager.persist(periode);
            }
        }
    }

    private void lagrePerioderAleneOmsorg(YtelseFordelingGrunnlagEntitet grunnlag) {
        if (grunnlag.getPerioderAleneOmsorgEntitet() != null) {
            entityManager.persist(grunnlag.getPerioderAleneOmsorgEntitet());
            for (var periode : grunnlag.getPerioderAleneOmsorgEntitet().getPerioder()) {
                entityManager.persist(periode);
            }
        }
    }

    private void lagrePerioderAnnenforelderHarRett(YtelseFordelingGrunnlagEntitet grunnlag) {
        if (grunnlag.getPerioderAnnenforelderHarRettEntitet() != null) {
            entityManager.persist(grunnlag.getPerioderAnnenforelderHarRettEntitet());
            for (var periode : grunnlag.getPerioderAnnenforelderHarRettEntitet().getPerioder()) {
                entityManager.persist(periode);
            }
        }
    }

    private void lagrePerioderAnnenforelderRettEØS(YtelseFordelingGrunnlagEntitet grunnlag) {
        if (grunnlag.getPerioderAnnenForelderRettEØSEntitet() != null) {
            entityManager.persist(grunnlag.getPerioderAnnenForelderRettEØSEntitet());
            for (var periode : grunnlag.getPerioderAnnenForelderRettEØSEntitet().getPerioder()) {
                entityManager.persist(periode);
            }
        }
    }

    private void lagrePerioderAktivitetskrav(AktivitetskravPerioderEntitet perioder) {
        if (perioder != null) {
            entityManager.persist(perioder);
            for (var periode : perioder.getPerioder()) {
                entityManager.persist(periode);
            }
        }
    }

    private void lagrePeriode(List<OppgittPeriodeEntitet> perioder) {
        for (var oppgittPeriode : perioder) {
            entityManager.persist(oppgittPeriode);
        }
    }

    /**
     * Kopierer grunnlag fra en tidligere behandling. Nullstiller avklarte datoer.
     * Brukes ifm oppretting av revurdering
     */
    public void kopierGrunnlagFraEksisterendeBehandling(Long gammelBehandlingId, Behandling nyBehandling) {
        var origAggregat = hentAggregatHvisEksisterer(gammelBehandlingId);
        origAggregat.ifPresent(ytelseFordelingAggregat -> {
            var yfBuilder = YtelseFordelingAggregat.Builder.oppdatere(Optional.of(ytelseFordelingAggregat))
                .medAvklarteDatoer(null)
                .medJustertFordeling(null)
                .medOverstyrtFordeling(null);
            if (!erBerørtBehandling(nyBehandling)) {
                yfBuilder.medPerioderUttakDokumentasjon(null);
            }
            ytelseFordelingAggregat.getGjeldendeAktivitetskravPerioder()
                .ifPresent(akp -> yfBuilder.medOpprinneligeAktivitetskravPerioder(akp).medSaksbehandledeAktivitetskravPerioder(null));
            lagreOgFlush(nyBehandling.getId(), yfBuilder.build());
        });
    }

    /**
     * Kopierer grunnlag fra en tidligere behandling. Nullstiller avklarte datoer.
     * Brukes ifm overhopp av uttak
     */
    public void kopierGrunnlagFraEksisterendeBehandlingForOverhoppUttak(Long gammelBehandlingId, Long nyBehandlingId) {
        var origAggregat = hentAggregatHvisEksisterer(gammelBehandlingId);
        origAggregat.ifPresent(ytelseFordelingAggregat -> {
            var avklarteDatoer = ytelseFordelingAggregat.getAvklarteDatoer();

            var avklarteUttakDatoerEntitet = new AvklarteUttakDatoerEntitet.Builder(avklarteDatoer)
                .medFørsteUttaksdato(null)
                .build();
            var yfBuilder = YtelseFordelingAggregat.Builder.oppdatere(Optional.of(ytelseFordelingAggregat))
                .medAvklarteDatoer(avklarteUttakDatoerEntitet);
            lagreOgFlush(nyBehandlingId, yfBuilder.build());
        });
    }


    public Optional<Long> hentIdPåAktivYtelsesFordeling(Long behandlingId) {
        return hentAktivtGrunnlag(behandlingId).map(YtelseFordelingGrunnlagEntitet::getId);
    }

    public Optional<YtelseFordelingGrunnlagEntitet> hentGrunnlagPåId(Long grunnlagId) {
        Objects.requireNonNull(grunnlagId, "grunnlagId");
        var query = entityManager.createQuery("FROM YtelseFordelingGrunnlag gr " + "WHERE gr.id = :grunnlagId ",
            YtelseFordelingGrunnlagEntitet.class);
        query.setParameter("grunnlagId", grunnlagId);
        return HibernateVerktøy.hentUniktResultat(query);
    }
}
