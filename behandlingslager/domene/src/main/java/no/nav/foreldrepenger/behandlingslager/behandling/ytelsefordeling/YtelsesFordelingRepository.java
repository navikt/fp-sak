package no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.jpa.HibernateVerktøy;

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

    private YtelseFordelingAggregat mapEntitetTilAggregat(YtelseFordelingGrunnlagEntitet ytelseFordelingGrunnlagEntitet) {
        return YtelseFordelingAggregat.Builder.nytt()
            .medOppgittDekningsgrad(ytelseFordelingGrunnlagEntitet.getOppgittDekningsgrad() == null ? null : Dekningsgrad.grad(
                ytelseFordelingGrunnlagEntitet.getOppgittDekningsgrad()))
            .medSakskompleksDekningsgrad(ytelseFordelingGrunnlagEntitet.getSakskompleksDekningsgrad() == null ? null : Dekningsgrad.grad(
                ytelseFordelingGrunnlagEntitet.getSakskompleksDekningsgrad()))
            .medOppgittRettighet(ytelseFordelingGrunnlagEntitet.getOppgittRettighet())
            .medOverstyrtRettighet(ytelseFordelingGrunnlagEntitet.getOverstyrtRettighet())
            .medOppgittFordeling(ytelseFordelingGrunnlagEntitet.getOppgittFordeling())
            .medJustertFordeling(ytelseFordelingGrunnlagEntitet.getJustertFordeling())
            .medOverstyrtFordeling(ytelseFordelingGrunnlagEntitet.getOverstyrtFordeling())
            .medAvklarteDatoer(ytelseFordelingGrunnlagEntitet.getAvklarteUttakDatoer())
            .medOverstyrtOmsorg(ytelseFordelingGrunnlagEntitet.getOverstyrtOmsorg())
            .build();
    }

    private Optional<YtelseFordelingGrunnlagEntitet> hentAktivtGrunnlag(Long behandlingId) {
        Objects.requireNonNull(behandlingId, "behandlingId");
        var query = entityManager.createQuery(
            "FROM YtelseFordelingGrunnlag gr " + "WHERE gr.behandlingId = :behandlingId " + "AND gr.aktiv = :aktivt",
            YtelseFordelingGrunnlagEntitet.class);
        query.setParameter("behandlingId", behandlingId);
        query.setParameter("aktivt", true);
        return HibernateVerktøy.hentUniktResultat(query);
    }

    private void lagreOgFlush(Long behandlingId, YtelseFordelingAggregat aggregat) {
        var eksisterendeGrunnlag = hentAktivtGrunnlag(behandlingId);
        var nyGrunnlagEntitet = mapTilGrunnlagEntitet(behandlingId, aggregat);
        if (eksisterendeGrunnlag.isPresent()) {
            var eksisterendeGrunnlag1 = eksisterendeGrunnlag.get();
            eksisterendeGrunnlag1.setAktiv(false);
            entityManager.persist(eksisterendeGrunnlag1);
            entityManager.flush();
        }
        lagreGrunnlag(nyGrunnlagEntitet);
        entityManager.flush();
    }

    private void lagreGrunnlag(YtelseFordelingGrunnlagEntitet grunnlag) {
        if (grunnlag.getOppgittRettighet() != null) {
            entityManager.persist(grunnlag.getOppgittRettighet());
        }
        if (grunnlag.getOverstyrtRettighet() != null) {
            entityManager.persist(grunnlag.getOverstyrtRettighet());
        }
        if (grunnlag.getOppgittFordeling() != null) {
            entityManager.persist(grunnlag.getOppgittFordeling());
            lagrePeriode(grunnlag.getOppgittFordeling().getPerioder());
        }
        if (grunnlag.getJustertFordeling() != null) {
            entityManager.persist(grunnlag.getJustertFordeling());
            lagrePeriode(grunnlag.getJustertFordeling().getPerioder());
        }
        if (grunnlag.getOverstyrtFordeling() != null) {
            entityManager.persist(grunnlag.getOverstyrtFordeling());
            lagrePeriode(grunnlag.getOverstyrtFordeling().getPerioder());
        }
        if (grunnlag.getAvklarteUttakDatoer() != null) {
            entityManager.persist(grunnlag.getAvklarteUttakDatoer());
        }

        entityManager.persist(grunnlag);
    }

    private YtelseFordelingGrunnlagEntitet mapTilGrunnlagEntitet(Long behandlingId, YtelseFordelingAggregat aggregat) {
        var grunnlag = new YtelseFordelingGrunnlagEntitet();
        grunnlag.setBehandling(behandlingId);
        grunnlag.setOppgittDekningsgrad(aggregat.getOppgittDekningsgrad() == null ? null : aggregat.getOppgittDekningsgrad().getVerdi());
        grunnlag.setSakskompleksDekningsgrad(aggregat.getSakskompleksDekningsgrad() == null ? null : aggregat.getSakskompleksDekningsgrad().getVerdi());
        grunnlag.setOppgittRettighet(aggregat.getOppgittRettighet());
        aggregat.getOverstyrtRettighet().ifPresent(grunnlag::setOverstyrtRettighet);
        grunnlag.setOppgittFordeling(aggregat.getOppgittFordeling());
        aggregat.getOverstyrtFordeling().ifPresent(grunnlag::setOverstyrtFordeling);
        aggregat.getJustertFordeling().ifPresent(grunnlag::setJustertFordeling);
        aggregat.getAvklarteDatoer().ifPresent(grunnlag::setAvklarteUttakDatoerEntitet);
        Optional.ofNullable(aggregat.getOverstyrtOmsorg()).ifPresent(grunnlag::setOverstyrtOmsorg);
        return grunnlag;
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
