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
import no.nav.vedtak.felles.jpa.VLPersistenceUnit;

@ApplicationScoped
public class YtelsesFordelingRepository {

    private EntityManager entityManager;

    protected YtelsesFordelingRepository() {
        // CDI
    }

    @Inject
    public YtelsesFordelingRepository(@VLPersistenceUnit EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager");
        this.entityManager = entityManager;
    }

    public YtelseFordelingAggregat hentAggregat(Long behandlingId) {
        final Optional<YtelseFordelingGrunnlagEntitet> ytelseFordelingGrunnlagEntitet = hentAktivtGrunnlag(behandlingId);
        if (ytelseFordelingGrunnlagEntitet.isPresent()) {
            return mapEntitetTilAggregat(ytelseFordelingGrunnlagEntitet.get());
        }
        throw YtelseFordelingFeil.FACTORY.fantIkkeForventetGrunnlagPåBehandling(behandlingId).toException();
    }

    private Optional<YtelseFordelingGrunnlagEntitet> hentYtelseFordelingPåId(Long grunnlagId) {
        return getVersjonAvYtelsesFordelingPåId(grunnlagId);
    }

    public YtelseFordelingAggregat hentYtelsesFordelingPåId(Long aggregatId) {
        Optional<YtelseFordelingGrunnlagEntitet> optGrunnlag = getVersjonAvYtelsesFordelingPåId(
            aggregatId);
        if (optGrunnlag.isPresent()) {
            return mapEntitetTilAggregat(optGrunnlag.get());
        }
        return new YtelseFordelingAggregat();
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
            .build();
    }

    public Optional<YtelseFordelingAggregat> hentAggregatHvisEksisterer(Long behandlingId) {
        final Optional<YtelseFordelingGrunnlagEntitet> entitet = hentAktivtGrunnlag(behandlingId);
        return entitet.map(this::mapEntitetTilAggregat);
    }

    public void lagre(Long behandlingId, OppgittRettighetEntitet oppgittRettighet) {
        Objects.requireNonNull(behandlingId, "behandlingId"); // NOSONAR $NON-NLS-1$
        Objects.requireNonNull(oppgittRettighet, "oppgittRettighet");
        final YtelseFordelingAggregat.Builder aggregatBuilder = opprettAggregatBuilderFor(behandlingId);
        aggregatBuilder.medOppgittRettighet(oppgittRettighet);

        lagreOgFlush(behandlingId, aggregatBuilder.build());
    }

    public void lagre(Long behandlingId, OppgittFordelingEntitet oppgittPerioder) {
        Objects.requireNonNull(behandlingId, "behandlingId"); // NOSONAR $NON-NLS-1$
        Objects.requireNonNull(oppgittPerioder, "oppgittPerioder");
        final YtelseFordelingAggregat.Builder aggregatBuilder = opprettAggregatBuilderFor(behandlingId);
        aggregatBuilder.medOppgittFordeling(oppgittPerioder);
        lagreOgFlush(behandlingId, aggregatBuilder.build());
    }

    public void lagreJustertFordeling(Long behandlingId, OppgittFordelingEntitet justertFordeling, AvklarteUttakDatoerEntitet avklarteUttakDatoer) {
        Objects.requireNonNull(behandlingId, "behandlingId"); // NOSONAR $NON-NLS-1$
        Objects.requireNonNull(justertFordeling, "justertFordeling");
        Objects.requireNonNull(avklarteUttakDatoer, "avklarteUttakDatoer");
        final YtelseFordelingAggregat.Builder aggregatBuilder = opprettAggregatBuilderFor(behandlingId);
        if (avklarteUttakDatoer.harVerdier()) {
            aggregatBuilder.medAvklarteDatoer(avklarteUttakDatoer);
        }
        aggregatBuilder.medJustertFordeling(justertFordeling);
        lagreOgFlush(behandlingId, aggregatBuilder.build());
    }

    public void lagreOverstyrtFordeling(Long behandlingId, OppgittFordelingEntitet oppgittPerioder, PerioderUttakDokumentasjonEntitet perioderUttakDokumentasjon) {
        Objects.requireNonNull(behandlingId, "behandlingId"); // NOSONAR $NON-NLS-1$
        Objects.requireNonNull(oppgittPerioder, "oppgittPerioder");
        final YtelseFordelingAggregat.Builder aggregatBuilder = opprettAggregatBuilderFor(behandlingId);
        aggregatBuilder.medOverstyrtFordeling(oppgittPerioder);
        aggregatBuilder.medPerioderUttakDokumentasjon(perioderUttakDokumentasjon);
        lagreOgFlush(behandlingId, aggregatBuilder.build());
    }

    public void lagreOverstyrtFordeling(Long behandlingId, OppgittFordelingEntitet oppgittPerioder) {
        lagreOverstyrtFordeling(behandlingId, oppgittPerioder, null);
    }

    public void lagre(Long behandlingId, OppgittDekningsgradEntitet oppgittDekningsgrad) {
        Objects.requireNonNull(behandlingId, "behandlingId"); // NOSONAR $NON-NLS-1$
        Objects.requireNonNull(oppgittDekningsgrad, "oppgittDekningsgrad");
        final YtelseFordelingAggregat.Builder aggregatBuilder = opprettAggregatBuilderFor(behandlingId);
        aggregatBuilder.medOppgittDekningsgrad(oppgittDekningsgrad);
        lagreOgFlush(behandlingId, aggregatBuilder.build());
    }

    public void lagre(Long behandlingId, PerioderUtenOmsorgEntitet perioderUtenOmsorg) {
        Objects.requireNonNull(behandlingId, "behandlingId"); // NOSONAR $NON-NLS-1$
        Objects.requireNonNull(perioderUtenOmsorg, "perioderUtenOmsorg");
        final YtelseFordelingAggregat.Builder aggregatBuilder = opprettAggregatBuilderFor(behandlingId);
        aggregatBuilder.medPerioderUtenOmsorg(perioderUtenOmsorg);
        lagreOgFlush(behandlingId, aggregatBuilder.build());
    }

    public void lagre(Long behandlingId, PerioderAleneOmsorgEntitet perioderAleneOmsorg) {
        Objects.requireNonNull(behandlingId, "behandlingId"); // NOSONAR $NON-NLS-1$
        Objects.requireNonNull(perioderAleneOmsorg, "perioderAleneOmsorg");
        final YtelseFordelingAggregat.Builder aggregatBuilder = opprettAggregatBuilderFor(behandlingId);
        aggregatBuilder.medPerioderAleneOmsorg(perioderAleneOmsorg);
        lagreOgFlush(behandlingId, aggregatBuilder.build());
    }

    public void lagre(Long behandlingId, PerioderAnnenforelderHarRettEntitet perioderAnnenforelderHarRett) {
        Objects.requireNonNull(behandlingId, "behandlingId"); // NOSONAR $NON-NLS-1$
        Objects.requireNonNull(perioderAnnenforelderHarRett, "perioderAnnenforelderHarRett");
        final YtelseFordelingAggregat.Builder aggregatBuilder = opprettAggregatBuilderFor(behandlingId);
        aggregatBuilder.medPerioderAnnenforelderHarRett(perioderAnnenforelderHarRett);
        lagreOgFlush(behandlingId, aggregatBuilder.build());
    }

    public void lagre(Long behandlingId, AvklarteUttakDatoerEntitet avklarteUttakDatoer) {
        Objects.requireNonNull(behandlingId, "behandling"); // NOSONAR $NON-NLS-1$
        Objects.requireNonNull(avklarteUttakDatoer, "avklarteUttakDatoerEntitet");
        if (avklarteUttakDatoer.harVerdier()) {
            final YtelseFordelingAggregat.Builder aggregatBuilder = opprettAggregatBuilderFor(behandlingId);
            aggregatBuilder.medAvklarteDatoer(avklarteUttakDatoer);
            lagreOgFlush(behandlingId, aggregatBuilder.build());
        }
    }

    private Optional<YtelseFordelingGrunnlagEntitet> hentAktivtGrunnlag(Long behandlingId) {
        Objects.requireNonNull(behandlingId, "behandlingId"); // NOSONAR $NON-NLS-1$
        final TypedQuery<YtelseFordelingGrunnlagEntitet> query = entityManager.createQuery("FROM YtelseFordelingGrunnlag gr " +
            "WHERE gr.behandlingId = :behandlingId " +
            "AND gr.aktiv = :aktivt", YtelseFordelingGrunnlagEntitet.class);
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
            for (PeriodeAnnenforelderHarRettEntitet periode : grunnlag.getPerioderAnnenforelderHarRettEntitet().getPerioder()) {
                entityManager.persist(periode);
            }
        }
    }

    private void lagrePeriode(List<OppgittPeriodeEntitet> perioder) {
        for (OppgittPeriodeEntitet oppgittPeriode : perioder) {
            entityManager.persist(oppgittPeriode);
        }
    }

    private YtelseFordelingAggregat.Builder opprettAggregatBuilderFor(Long behandlingId) {
        Optional<YtelseFordelingAggregat> ytelseFordelingAggregat = hentAggregatHvisEksisterer(behandlingId);
        return YtelseFordelingAggregat.Builder.oppdatere(ytelseFordelingAggregat);
    }

    /**
     * Kopierer grunnlag fra en tidligere behandling.  Endrer ikke aggregater, en skaper nye referanser til disse.
     */
    public void kopierGrunnlagFraEksisterendeBehandling(Long gammelBehandlingId, Long nyBehandlingId) {
        Optional<YtelseFordelingAggregat> origAggregat = hentAggregatHvisEksisterer(gammelBehandlingId);
        origAggregat.ifPresent(ytelseFordelingAggregat -> lagreOgFlush(nyBehandlingId, ytelseFordelingAggregat));
    }

    public Optional<Long> hentIdPåAktivYtelsesFordeling(Long behandlingId) {
        return hentAktivtGrunnlag(behandlingId)
            .map(YtelseFordelingGrunnlagEntitet::getId);
    }

    /**
     * Tiltenkt ved inngangen til revurdering
     */
    public void tilbakestillFordeling(Long behandlingId) {
        YtelseFordelingAggregat ytelseFordeling = opprettAggregatBuilderFor(behandlingId)
            .medAvklarteDatoer(null)
            .medJustertFordeling(null)
            .medPerioderUttakDokumentasjon(null)
            .medOverstyrtFordeling(null)
            .build();
        lagreOgFlush(behandlingId, ytelseFordeling);
    }

    public void lagre(Long behandlingId, YtelseFordelingAggregat aggregat) {
        lagreOgFlush(behandlingId, aggregat);
    }

    public DiffResult diffResultat(Long grunnlagId1, Long grunnlagId2, boolean onlyCheckTrackedFields) {
        YtelseFordelingGrunnlagEntitet grunnlag1 = hentYtelseFordelingPåId(grunnlagId1)
            .orElseThrow(() -> new IllegalStateException("GrunnlagId1 må være oppgitt"));
        YtelseFordelingGrunnlagEntitet grunnlag2 = hentYtelseFordelingPåId(grunnlagId2)
            .orElseThrow(() -> new IllegalStateException("GrunnlagId2 må være oppgitt"));
        return new RegisterdataDiffsjekker(onlyCheckTrackedFields).getDiffEntity().diff(grunnlag1, grunnlag2);
    }

    private Optional<YtelseFordelingGrunnlagEntitet> getVersjonAvYtelsesFordelingPåId(
                                                                                      Long aggregatId) {
        Objects.requireNonNull(aggregatId, "aggregatId"); // NOSONAR $NON-NLS-1$
        final TypedQuery<YtelseFordelingGrunnlagEntitet> query = entityManager.createQuery("FROM YtelseFordelingGrunnlag gr " +
            "WHERE gr.id = :aggregatId ", YtelseFordelingGrunnlagEntitet.class);
        query.setParameter("aggregatId", aggregatId);
        return HibernateVerktøy.hentUniktResultat(query);
    }
}
