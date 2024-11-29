package no.nav.foreldrepenger.behandling;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.dbstoette.JpaExtension;

@ExtendWith(JpaExtension.class)
class RelatertBehandlingTjenesteTest {

    private BehandlingRepositoryProvider repositoryProvider;
    private EntityManager em;

    private RelatertBehandlingTjeneste relatertBehandlingTjeneste;
    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;

    @BeforeEach
    void setUp(EntityManager entityManager) {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        em = entityManager;
        fagsakRelasjonTjeneste = new FagsakRelasjonTjeneste(repositoryProvider);
        relatertBehandlingTjeneste = new RelatertBehandlingTjeneste(repositoryProvider, fagsakRelasjonTjeneste);
    }

    @Test
    void finnesIngenRelatertFagsakReturnererOptionalEmpty() {
        var farsBehandling = ScenarioFarSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);
        var annenPartsGjeldendeVedtattBehandling = relatertBehandlingTjeneste
                .hentAnnenPartsGjeldendeVedtattBehandling(farsBehandling.getSaksnummer());

        assertThat(annenPartsGjeldendeVedtattBehandling).isEmpty();

    }

    @Test
    void finnesIngenRelatertVedtattBehandlingReturnererOptionalEmpty() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var morsBehandling = scenario.lagre(repositoryProvider);

        var farsBehandling = ScenarioFarSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);
        fagsakRelasjonTjeneste.opprettRelasjon(morsBehandling.getFagsak());
        fagsakRelasjonTjeneste.kobleFagsaker(morsBehandling.getFagsak(), farsBehandling.getFagsak());

        var uttakresultat = relatertBehandlingTjeneste
                .hentAnnenPartsGjeldendeVedtattBehandling(farsBehandling.getSaksnummer());

        assertThat(uttakresultat).isEmpty();
    }

    @Test
    void finnesRelatertBehandlingSomErAvslått() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medBehandlingVedtak()
                .medVedtakstidspunkt(LocalDateTime.of(LocalDate.of(2019, 8, 1), LocalTime.MIDNIGHT))
                .medVedtakResultatType(VedtakResultatType.AVSLAG);
        var morsBehandling = scenario.lagre(repositoryProvider);

        var behandlingsresultat = morsBehandling.getBehandlingsresultat();
        Behandlingsresultat.builderEndreEksisterende(behandlingsresultat).medBehandlingResultatType(BehandlingResultatType.AVSLÅTT);
        em.persist(behandlingsresultat);
        morsBehandling.avsluttBehandling();
        em.persist(morsBehandling);

        var farsBehandling = ScenarioFarSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);
        fagsakRelasjonTjeneste.opprettRelasjon(morsBehandling.getFagsak());
        fagsakRelasjonTjeneste.kobleFagsaker(morsBehandling.getFagsak(), farsBehandling.getFagsak());

        var behandling = relatertBehandlingTjeneste
                .hentAnnenPartsGjeldendeVedtattBehandling(farsBehandling.getSaksnummer());

        assertThat(behandling.get().getId()).isEqualTo(morsBehandling.getId());
    }

    @Test
    void finnerRelatertBehandling() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medBehandlingVedtak()
                .medVedtakstidspunkt(LocalDateTime.of(LocalDate.of(2019, 8, 1), LocalTime.MIDNIGHT))
                .medVedtakResultatType(VedtakResultatType.INNVILGET);

        var morsBehandling = scenario.lagre(repositoryProvider);

        morsBehandling.avsluttBehandling();
        repositoryProvider.getBehandlingRepository().lagre(morsBehandling,
                repositoryProvider.getBehandlingLåsRepository().taLås(morsBehandling.getId()));

        var farsBehandling = ScenarioFarSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);
        fagsakRelasjonTjeneste.opprettRelasjon(morsBehandling.getFagsak());
        fagsakRelasjonTjeneste.kobleFagsaker(morsBehandling.getFagsak(), farsBehandling.getFagsak());

        var relatertBehandling = relatertBehandlingTjeneste
                .hentAnnenPartsGjeldendeVedtattBehandling(farsBehandling.getSaksnummer());

        assertThat(relatertBehandling).isPresent();
        assertThat(relatertBehandling.get().getId()).isEqualTo(morsBehandling.getId());
    }

    @Test
    void skal_finne_gjeldende_behandling_fra_annenpart_ved_flere_annenpart_behandlinger_med_vedtak() {
        var morFørstegangScenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        morFørstegangScenario.medBehandlingVedtak()
                .medVedtakstidspunkt(LocalDateTime.of(LocalDate.of(2019, 8, 1), LocalTime.MIDNIGHT))
                .medVedtakResultatType(VedtakResultatType.INNVILGET);

        var morsFørsteBehandling = morFørstegangScenario.lagre(repositoryProvider);
        fagsakRelasjonTjeneste.opprettRelasjon(morsFørsteBehandling.getFagsak());

        morsFørsteBehandling.avsluttBehandling();
        repositoryProvider.getBehandlingRepository().lagre(morsFørsteBehandling,
                repositoryProvider.getBehandlingLåsRepository().taLås(morsFørsteBehandling.getId()));

        var morRevurdering = ScenarioMorSøkerForeldrepenger.forFødsel();
        morRevurdering.medBehandlingType(BehandlingType.REVURDERING);
        morRevurdering.medOriginalBehandling(morsFørsteBehandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);
        morRevurdering.medBehandlingVedtak()
                .medVedtakstidspunkt(LocalDateTime.of(LocalDate.of(2019, 8, 2), LocalTime.MIDNIGHT))
                .medVedtakResultatType(VedtakResultatType.INNVILGET);

        var morsRevurdering = morRevurdering.lagre(repositoryProvider);

        morsRevurdering.avsluttBehandling();
        repositoryProvider.getBehandlingRepository().lagre(morsRevurdering,
                repositoryProvider.getBehandlingLåsRepository().taLås(morsRevurdering.getId()));

        var farsBehandling = ScenarioFarSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);
        farsBehandling = repositoryProvider.getBehandlingRepository().hentBehandling(farsBehandling.getId());
        fagsakRelasjonTjeneste.kobleFagsaker(morsRevurdering.getFagsak(), farsBehandling.getFagsak());

        var annenPartsGjeldendeVedtattBehandling = relatertBehandlingTjeneste
                .hentAnnenPartsGjeldendeVedtattBehandling(farsBehandling.getSaksnummer());

        assertThat(annenPartsGjeldendeVedtattBehandling.get().getId()).isEqualTo(morsRevurdering.getId());
    }

    @Test
    void skal_finne_behandling_for_annenpart_på_søkers_vedtakstidspunkt_ved_flere_annenpart_behandlinger() {
        var morFørstegangScenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        morFørstegangScenario.medBehandlingVedtak()
                .medVedtakstidspunkt(LocalDateTime.of(LocalDate.of(2019, 8, 1), LocalTime.MIDNIGHT))
                .medVedtakResultatType(VedtakResultatType.INNVILGET);

        var morsFørsteBehandling = morFørstegangScenario.lagre(repositoryProvider);
        fagsakRelasjonTjeneste.opprettRelasjon(morsFørsteBehandling.getFagsak());

        morsFørsteBehandling.avsluttBehandling();
        repositoryProvider.getBehandlingRepository().lagre(morsFørsteBehandling,
                repositoryProvider.getBehandlingLåsRepository().taLås(morsFørsteBehandling.getId()));

        var morRevurdering = ScenarioMorSøkerForeldrepenger.forFødsel();
        morRevurdering.medBehandlingType(BehandlingType.REVURDERING);
        morRevurdering.medOriginalBehandling(morsFørsteBehandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);
        morRevurdering.medBehandlingVedtak()
                .medVedtakstidspunkt(LocalDateTime.of(LocalDate.of(2019, 8, 2), LocalTime.MIDNIGHT))
                .medVedtakResultatType(VedtakResultatType.INNVILGET);

        var morsRevurdering = morRevurdering.lagre(repositoryProvider);

        morsRevurdering.avsluttBehandling();
        repositoryProvider.getBehandlingRepository().lagre(morsRevurdering,
                repositoryProvider.getBehandlingLåsRepository().taLås(morsRevurdering.getId()));

        var farsScenario = ScenarioFarSøkerForeldrepenger.forFødsel();
        farsScenario.medBehandlingVedtak()
                .medVedtakstidspunkt(LocalDateTime.of(LocalDate.of(2019, 8, 3), LocalTime.MIDNIGHT))
                .medVedtakResultatType(VedtakResultatType.INNVILGET);
        var farsBehandling = farsScenario.lagre(repositoryProvider);
        farsBehandling = repositoryProvider.getBehandlingRepository().hentBehandling(farsBehandling.getId());
        fagsakRelasjonTjeneste.kobleFagsaker(morsRevurdering.getFagsak(), farsBehandling.getFagsak());

        var annenPartsGjeldendeBehandlingPåVedtakstidspunkt = relatertBehandlingTjeneste
                .hentAnnenPartsGjeldendeBehandlingPåVedtakstidspunkt(farsBehandling);

        assertThat(annenPartsGjeldendeBehandlingPåVedtakstidspunkt.get().getId()).isEqualTo(morsRevurdering.getId());
    }

    @Test
    void skal_finne_gjeldende_behandling_fra_annenpart_ved_flere_annenpart_behandlinger_med_vedtak_på_samme_dato() {
        var morFørstegangScenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        morFørstegangScenario.medBehandlingVedtak()
                .medVedtakstidspunkt(LocalDateTime.of(LocalDate.of(2019, 8, 2), LocalTime.MIDNIGHT))
                .medVedtakResultatType(VedtakResultatType.INNVILGET);

        var morsFørsteBehandling = morFørstegangScenario.lagre(repositoryProvider);
        fagsakRelasjonTjeneste.opprettRelasjon(morsFørsteBehandling.getFagsak());

        morsFørsteBehandling.avsluttBehandling();
        repositoryProvider.getBehandlingRepository().lagre(morsFørsteBehandling,
                repositoryProvider.getBehandlingLåsRepository().taLås(morsFørsteBehandling.getId()));

        var morRevurdering = ScenarioMorSøkerForeldrepenger.forFødsel();
        morRevurdering.medBehandlingType(BehandlingType.REVURDERING);
        morRevurdering.medOriginalBehandling(morsFørsteBehandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);
        morRevurdering.medBehandlingVedtak()
                .medVedtakstidspunkt(LocalDateTime.of(LocalDate.of(2019, 8, 2), LocalTime.MIDNIGHT))
                .medVedtakResultatType(VedtakResultatType.INNVILGET);

        var morsRevurdering = morRevurdering.lagre(repositoryProvider);

        morsRevurdering.avsluttBehandling();
        repositoryProvider.getBehandlingRepository().lagre(morsRevurdering,
                repositoryProvider.getBehandlingLåsRepository().taLås(morsRevurdering.getId()));

        var farsBehandling = ScenarioFarSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);
        farsBehandling = repositoryProvider.getBehandlingRepository().hentBehandling(farsBehandling.getId());
        fagsakRelasjonTjeneste.kobleFagsaker(morsRevurdering.getFagsak(), farsBehandling.getFagsak());

        var annenPartsGjeldendeVedtattBehandling = relatertBehandlingTjeneste
                .hentAnnenPartsGjeldendeVedtattBehandling(farsBehandling.getSaksnummer());

        assertThat(annenPartsGjeldendeVedtattBehandling.get().getId()).isEqualTo(morsRevurdering.getId());
    }

    @Test
    void skal_finne_gjeldende_behandling_fra_annenpart_på_søkers_vedtakstidspunkt_ved_flere_annenpart_behandlinger_med_vedtak_på_samme_dato() {
        var morFørstegangScenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        morFørstegangScenario.medBehandlingVedtak()
                .medVedtakstidspunkt(LocalDateTime.of(LocalDate.of(2019, 8, 1), LocalTime.MIDNIGHT))
                .medVedtakResultatType(VedtakResultatType.INNVILGET);

        var aktivitet = new UttakAktivitetEntitet.Builder()
                .medUttakArbeidType(UttakArbeidType.FRILANS)
                .build();
        var uttakStart = LocalDate.of(2018, 5, 14);
        var uttakMødrekvote = new UttakResultatPeriodeEntitet.Builder(uttakStart, uttakStart.plusWeeks(6).minusDays(1))
                .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
                .build();
        UttakResultatPeriodeAktivitetEntitet.builder(uttakMødrekvote, aktivitet)
                .medTrekkdager(new Trekkdager(30))
                .medTrekkonto(UttakPeriodeType.MØDREKVOTE)
                .medArbeidsprosent(BigDecimal.TEN).build();
        var uttak = new UttakResultatPerioderEntitet();
        uttak.leggTilPeriode(uttakMødrekvote);
        morFørstegangScenario.medUttak(uttak);

        var morsFørsteBehandling = morFørstegangScenario.lagre(repositoryProvider);
        fagsakRelasjonTjeneste.opprettRelasjon(morsFørsteBehandling.getFagsak());

        morsFørsteBehandling.avsluttBehandling();
        repositoryProvider.getBehandlingRepository().lagre(morsFørsteBehandling,
                repositoryProvider.getBehandlingLåsRepository().taLås(morsFørsteBehandling.getId()));

        var morRevurdering = ScenarioMorSøkerForeldrepenger.forFødsel();
        morRevurdering.medBehandlingType(BehandlingType.REVURDERING);
        morRevurdering.medOriginalBehandling(morsFørsteBehandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);
        morRevurdering.medBehandlingVedtak()
                .medVedtakstidspunkt(LocalDateTime.of(LocalDate.of(2019, 8, 1), LocalTime.MIDNIGHT))
                .medVedtakResultatType(VedtakResultatType.INNVILGET);

        var aktivitet2 = new UttakAktivitetEntitet.Builder()
                .medUttakArbeidType(UttakArbeidType.FRILANS)
                .build();
        // Uttak mødrekvote
        var uttakMødrekvote2 = new UttakResultatPeriodeEntitet.Builder(uttakStart, uttakStart.plusWeeks(6).minusDays(1))
                .medResultatType(PeriodeResultatType.AVSLÅTT, PeriodeResultatÅrsak.UKJENT)
                .build();
        UttakResultatPeriodeAktivitetEntitet.builder(uttakMødrekvote2, aktivitet2)
                .medTrekkdager(new Trekkdager(30))
                .medTrekkonto(UttakPeriodeType.MØDREKVOTE)
                .medArbeidsprosent(BigDecimal.ZERO).build();
        var uttak2 = new UttakResultatPerioderEntitet();
        uttak2.leggTilPeriode(uttakMødrekvote2);
        morRevurdering.medUttak(uttak2);

        var morsRevurdering = morRevurdering.lagre(repositoryProvider);

        morsRevurdering.avsluttBehandling();
        repositoryProvider.getBehandlingRepository().lagre(morsRevurdering,
                repositoryProvider.getBehandlingLåsRepository().taLås(morsRevurdering.getId()));

        var farsScenario = ScenarioFarSøkerForeldrepenger.forFødsel();
        farsScenario.medBehandlingVedtak()
                .medVedtakstidspunkt(LocalDateTime.of(LocalDate.of(2019, 8, 2), LocalTime.MIDNIGHT))
                .medVedtakResultatType(VedtakResultatType.INNVILGET);
        var farsBehandling = farsScenario.lagre(repositoryProvider);
        farsBehandling = repositoryProvider.getBehandlingRepository().hentBehandling(farsBehandling.getId());
        fagsakRelasjonTjeneste.kobleFagsaker(morsRevurdering.getFagsak(), farsBehandling.getFagsak());

        var annenPartsGjeldendeVedtattUttaksplan = relatertBehandlingTjeneste
                .hentAnnenPartsGjeldendeBehandlingPåVedtakstidspunkt(farsBehandling);

        assertThat(annenPartsGjeldendeVedtattUttaksplan.get().getId()).isEqualTo(morsRevurdering.getId());
    }
}
