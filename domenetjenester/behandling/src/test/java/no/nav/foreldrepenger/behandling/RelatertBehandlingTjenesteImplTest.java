package no.nav.foreldrepenger.behandling;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.vedtak.felles.testutilities.db.Repository;
import no.nav.vedtak.felles.testutilities.db.RepositoryRule;

public class RelatertBehandlingTjenesteImplTest {
    @Rule
    public RepositoryRule repoRule = new UnittestRepositoryRule();
    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());
    private Repository repository = repoRule.getRepository();

    private RelatertBehandlingTjeneste relatertBehandlingTjeneste = new RelatertBehandlingTjeneste(repositoryProvider);

    @Test
    public void finnesIngenRelatertFagsakReturnererOptionalEmpty() {
        // Arrange
        Behandling farsBehandling = ScenarioFarSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);

        // Act
        Optional<UttakResultatEntitet> uttakresultat = relatertBehandlingTjeneste
            .hentAnnenPartsGjeldendeVedtattUttaksplan(farsBehandling.getFagsak().getSaksnummer());

        // Assert
        assertThat(uttakresultat).isEmpty();

    }

    @Test
    public void finnesIngenRelatertVedtattBehandlingReturnererOptionalEmpty() {
        // Arrange
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        Behandling morsBehandling = scenario.lagre(repositoryProvider);

        Behandling farsBehandling = ScenarioFarSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);
        repositoryProvider.getFagsakRelasjonRepository().opprettRelasjon(morsBehandling.getFagsak(), Dekningsgrad._100);
        repositoryProvider.getFagsakRelasjonRepository().kobleFagsaker(morsBehandling.getFagsak(), farsBehandling.getFagsak(), morsBehandling);

        // Act
        Optional<UttakResultatEntitet> uttakresultat = relatertBehandlingTjeneste
            .hentAnnenPartsGjeldendeVedtattUttaksplan(farsBehandling.getFagsak().getSaksnummer());

        // Assert
        assertThat(uttakresultat).isEmpty();
    }

    @Test
    public void finnesIngenUttaksplanPåRelatertBehandlingSomErAvslått() {
        // Arrange
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        Behandling morsBehandling = scenario.lagre(repositoryProvider);

        Behandlingsresultat behandlingsresultat = morsBehandling.getBehandlingsresultat();
        Behandlingsresultat.builderEndreEksisterende(behandlingsresultat).medBehandlingResultatType(BehandlingResultatType.AVSLÅTT);
        repository.lagre(behandlingsresultat);
        morsBehandling.avsluttBehandling();
        repository.lagre(morsBehandling);

        Behandling farsBehandling = ScenarioFarSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);
        repositoryProvider.getFagsakRelasjonRepository().opprettRelasjon(morsBehandling.getFagsak(), Dekningsgrad._100);
        repositoryProvider.getFagsakRelasjonRepository().kobleFagsaker(morsBehandling.getFagsak(), farsBehandling.getFagsak(), morsBehandling);

        // Act
        Optional<UttakResultatEntitet> uttakresultat = relatertBehandlingTjeneste
            .hentAnnenPartsGjeldendeVedtattUttaksplan(farsBehandling.getFagsak().getSaksnummer());

        // Assert
        assertThat(uttakresultat).isEmpty();
    }

    @Test
    public void finnerUttaksplanTilRelatertBehandling() {
        // Arrange
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medBehandlingVedtak()
            .medVedtakstidspunkt(LocalDateTime.of(LocalDate.of(2019, 8, 1), LocalTime.MIDNIGHT))
            .medVedtakResultatType(VedtakResultatType.INNVILGET);

        String orgnr = "1111";
        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet(orgnr);
        var arbeidsforholdId = InternArbeidsforholdRef.nyRef();
        UttakAktivitetEntitet aktivitet = new UttakAktivitetEntitet.Builder()
            .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID).medArbeidsforhold(arbeidsgiver, arbeidsforholdId)
            .build();
        LocalDate uttakStart = LocalDate.of(2018, 5, 14);
        // Uttak mødrekvote
        UttakResultatPeriodeEntitet uttakMødrekvote = new UttakResultatPeriodeEntitet.Builder(uttakStart, uttakStart.plusWeeks(6).minusDays(1))
            .medPeriodeResultat(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
            .build();
        UttakResultatPeriodeAktivitetEntitet.builder(uttakMødrekvote, aktivitet)
            .medTrekkdager(new Trekkdager(30))
            .medTrekkonto(StønadskontoType.MØDREKVOTE)
            .medArbeidsprosent(BigDecimal.ZERO).build();
        UttakResultatPerioderEntitet uttak = new UttakResultatPerioderEntitet();
        uttak.leggTilPeriode(uttakMødrekvote);
        // Uttak fellesperiode
        UttakResultatPeriodeEntitet uttakFellesperiode = new UttakResultatPeriodeEntitet.Builder(uttakStart.plusWeeks(6), uttakStart.plusWeeks(10).minusDays(1))
            .medPeriodeResultat(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
            .build();
        UttakResultatPeriodeAktivitetEntitet.builder(uttakFellesperiode, aktivitet)
            .medTrekkdager(new Trekkdager(20))
            .medTrekkonto(StønadskontoType.FELLESPERIODE)
            .medArbeidsprosent(BigDecimal.ZERO).build();
        uttak.leggTilPeriode(uttakFellesperiode);

        scenario.medUttak(uttak);

        Behandling morsBehandling = scenario.lagre(repositoryProvider);

        morsBehandling.avsluttBehandling();
        repositoryProvider.getBehandlingRepository().lagre(morsBehandling, repositoryProvider.getBehandlingLåsRepository().taLås(morsBehandling.getId()));

        Behandling farsBehandling = ScenarioFarSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);
        repositoryProvider.getFagsakRelasjonRepository().opprettRelasjon(morsBehandling.getFagsak(), Dekningsgrad._100);
        repositoryProvider.getFagsakRelasjonRepository().kobleFagsaker(morsBehandling.getFagsak(), farsBehandling.getFagsak(), morsBehandling);

        // Act
        Optional<UttakResultatEntitet> uttakresultat = relatertBehandlingTjeneste
            .hentAnnenPartsGjeldendeVedtattUttaksplan(farsBehandling.getFagsak().getSaksnummer());

        // Assert
        assertThat(uttakresultat).isPresent();
        assertThat(uttakresultat.get().getGjeldendePerioder().getPerioder()).hasSize(2);
    }

    @Test
    public void skal_finne_gjeldende_uttak_fra_annenpart_ved_flere_annenpart_behandlinger_med_vedtak() {
        // Arrange
        ScenarioMorSøkerForeldrepenger morFørstegangScenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        morFørstegangScenario.medBehandlingVedtak()
            .medVedtakstidspunkt(LocalDateTime.of(LocalDate.of(2019, 8, 1), LocalTime.MIDNIGHT))
            .medVedtakResultatType(VedtakResultatType.INNVILGET);

        UttakAktivitetEntitet aktivitet = new UttakAktivitetEntitet.Builder()
            .medUttakArbeidType(UttakArbeidType.FRILANS)
            .build();
        LocalDate uttakStart = LocalDate.of(2018, 5, 14);
        // Uttak mødrekvote
        UttakResultatPeriodeEntitet uttakMødrekvote = new UttakResultatPeriodeEntitet.Builder(uttakStart, uttakStart.plusWeeks(6).minusDays(1))
            .medPeriodeResultat(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
            .build();
        UttakResultatPeriodeAktivitetEntitet.builder(uttakMødrekvote, aktivitet)
            .medTrekkdager(new Trekkdager(30))
            .medTrekkonto(StønadskontoType.MØDREKVOTE)
            .medArbeidsprosent(BigDecimal.TEN).build();
        UttakResultatPerioderEntitet uttak = new UttakResultatPerioderEntitet();
        uttak.leggTilPeriode(uttakMødrekvote);
        morFørstegangScenario.medUttak(uttak);

        Behandling morsFørsteBehandling = morFørstegangScenario.lagre(repositoryProvider);
        repositoryProvider.getFagsakRelasjonRepository().opprettRelasjon(morsFørsteBehandling.getFagsak(), Dekningsgrad._100);

        morsFørsteBehandling.avsluttBehandling();
        repositoryProvider.getBehandlingRepository().lagre(morsFørsteBehandling,
            repositoryProvider.getBehandlingLåsRepository().taLås(morsFørsteBehandling.getId()));

        // Arrange
        ScenarioMorSøkerForeldrepenger morRevurdering = ScenarioMorSøkerForeldrepenger.forFødsel();
        morRevurdering.medBehandlingType(BehandlingType.REVURDERING);
        morRevurdering.medOriginalBehandling(morsFørsteBehandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);
        morRevurdering.medBehandlingVedtak()
            .medVedtakstidspunkt(LocalDateTime.of(LocalDate.of(2019, 8, 2), LocalTime.MIDNIGHT))
            .medVedtakResultatType(VedtakResultatType.INNVILGET);

        UttakAktivitetEntitet aktivitet2 = new UttakAktivitetEntitet.Builder()
            .medUttakArbeidType(UttakArbeidType.FRILANS)
            .build();
        // Uttak mødrekvote
        UttakResultatPeriodeEntitet uttakMødrekvote2 = new UttakResultatPeriodeEntitet.Builder(uttakStart, uttakStart.plusWeeks(6).minusDays(1))
            .medPeriodeResultat(PeriodeResultatType.AVSLÅTT, PeriodeResultatÅrsak.UKJENT)
            .build();
        UttakResultatPeriodeAktivitetEntitet.builder(uttakMødrekvote2, aktivitet2)
            .medTrekkdager(new Trekkdager(30))
            .medTrekkonto(StønadskontoType.MØDREKVOTE)
            .medArbeidsprosent(BigDecimal.ZERO).build();
        UttakResultatPerioderEntitet uttak2 = new UttakResultatPerioderEntitet();
        uttak2.leggTilPeriode(uttakMødrekvote2);
        morRevurdering.medUttak(uttak2);

        Behandling morsRevurdering = morRevurdering.lagre(repositoryProvider);

        morsRevurdering.avsluttBehandling();
        repositoryProvider.getBehandlingRepository().lagre(morsRevurdering, repositoryProvider.getBehandlingLåsRepository().taLås(morsRevurdering.getId()));

        Behandling farsBehandling = ScenarioFarSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);
        farsBehandling = repositoryProvider.getBehandlingRepository().hentBehandling(farsBehandling.getId());
        repositoryProvider.getFagsakRelasjonRepository().kobleFagsaker(morsRevurdering.getFagsak(), farsBehandling.getFagsak(), morsRevurdering);

        // Act
        Optional<UttakResultatEntitet> annenPartsGjeldendeVedtattUttaksplan = relatertBehandlingTjeneste
            .hentAnnenPartsGjeldendeVedtattUttaksplan(farsBehandling.getFagsak().getSaksnummer());

        // Assert
        assertThat(annenPartsGjeldendeVedtattUttaksplan.get().getGjeldendePerioder().getPerioder().get(0).isInnvilget()).isFalse();
    }

    @Test
    public void skal_finne_gjeldende_uttak_fra_annenpart_på_søkers_vedtakstidspunkt_ved_flere_annenpart_behandlinger() {
        // Arrange
        ScenarioMorSøkerForeldrepenger morFørstegangScenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        morFørstegangScenario.medBehandlingVedtak()
            .medVedtakstidspunkt(LocalDateTime.of(LocalDate.of(2019, 8, 1), LocalTime.MIDNIGHT))
            .medVedtakResultatType(VedtakResultatType.INNVILGET);

        UttakAktivitetEntitet aktivitet = new UttakAktivitetEntitet.Builder()
            .medUttakArbeidType(UttakArbeidType.FRILANS)
            .build();
        LocalDate uttakStart = LocalDate.of(2018, 5, 14);
        // Uttak mødrekvote
        UttakResultatPeriodeEntitet uttakMødrekvote = new UttakResultatPeriodeEntitet.Builder(uttakStart, uttakStart.plusWeeks(6).minusDays(1))
            .medPeriodeResultat(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
            .build();
        UttakResultatPeriodeAktivitetEntitet.builder(uttakMødrekvote, aktivitet)
            .medTrekkdager(new Trekkdager(30))
            .medTrekkonto(StønadskontoType.MØDREKVOTE)
            .medArbeidsprosent(BigDecimal.TEN).build();
        UttakResultatPerioderEntitet uttak = new UttakResultatPerioderEntitet();
        uttak.leggTilPeriode(uttakMødrekvote);
        morFørstegangScenario.medUttak(uttak);

        Behandling morsFørsteBehandling = morFørstegangScenario.lagre(repositoryProvider);
        repositoryProvider.getFagsakRelasjonRepository().opprettRelasjon(morsFørsteBehandling.getFagsak(), Dekningsgrad._100);

        morsFørsteBehandling.avsluttBehandling();
        repositoryProvider.getBehandlingRepository().lagre(morsFørsteBehandling,
            repositoryProvider.getBehandlingLåsRepository().taLås(morsFørsteBehandling.getId()));

        // Arrange
        ScenarioMorSøkerForeldrepenger morRevurdering = ScenarioMorSøkerForeldrepenger.forFødsel();
        morRevurdering.medBehandlingType(BehandlingType.REVURDERING);
        morRevurdering.medOriginalBehandling(morsFørsteBehandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);
        morRevurdering.medBehandlingVedtak()
            .medVedtakstidspunkt(LocalDateTime.of(LocalDate.of(2019, 8, 2), LocalTime.MIDNIGHT))
            .medVedtakResultatType(VedtakResultatType.INNVILGET);

        UttakAktivitetEntitet aktivitet2 = new UttakAktivitetEntitet.Builder()
            .medUttakArbeidType(UttakArbeidType.FRILANS)
            .build();
        // Uttak mødrekvote
        UttakResultatPeriodeEntitet uttakMødrekvote2 = new UttakResultatPeriodeEntitet.Builder(uttakStart, uttakStart.plusWeeks(6).minusDays(1))
            .medPeriodeResultat(PeriodeResultatType.AVSLÅTT, PeriodeResultatÅrsak.UKJENT)
            .build();
        UttakResultatPeriodeAktivitetEntitet.builder(uttakMødrekvote2, aktivitet2)
            .medTrekkdager(new Trekkdager(30))
            .medTrekkonto(StønadskontoType.MØDREKVOTE)
            .medArbeidsprosent(BigDecimal.ZERO).build();
        UttakResultatPerioderEntitet uttak2 = new UttakResultatPerioderEntitet();
        uttak2.leggTilPeriode(uttakMødrekvote2);
        morRevurdering.medUttak(uttak2);

        Behandling morsRevurdering = morRevurdering.lagre(repositoryProvider);

        morsRevurdering.avsluttBehandling();
        repositoryProvider.getBehandlingRepository().lagre(morsRevurdering, repositoryProvider.getBehandlingLåsRepository().taLås(morsRevurdering.getId()));

        ScenarioFarSøkerForeldrepenger farsScenario = ScenarioFarSøkerForeldrepenger.forFødsel();
        farsScenario.medBehandlingVedtak()
            .medVedtakstidspunkt(LocalDateTime.of(LocalDate.of(2019, 8, 3), LocalTime.MIDNIGHT))
            .medVedtakResultatType(VedtakResultatType.INNVILGET);
        Behandling farsBehandling = farsScenario.lagre(repositoryProvider);
        farsBehandling = repositoryProvider.getBehandlingRepository().hentBehandling(farsBehandling.getId());
        repositoryProvider.getFagsakRelasjonRepository().kobleFagsaker(morsRevurdering.getFagsak(), farsBehandling.getFagsak(), morsRevurdering);

        // Act
        Optional<UttakResultatEntitet> annenPartsGjeldendeVedtattUttaksplan = relatertBehandlingTjeneste
            .hentAnnenPartsGjeldendeUttaksplanPåVedtakstidspunkt(farsBehandling);

        // Assert
        assertThat(annenPartsGjeldendeVedtattUttaksplan.get().getGjeldendePerioder().getPerioder().get(0).isInnvilget()).isFalse();
    }

    @Test
    public void skal_finne_gjeldende_uttak_fra_annenpart_ved_flere_annenpart_behandlinger_med_vedtak_på_samme_dato() {
        // Arrange
        ScenarioMorSøkerForeldrepenger morFørstegangScenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        morFørstegangScenario.medBehandlingVedtak()
            .medVedtakstidspunkt(LocalDateTime.of(LocalDate.of(2019, 8, 2), LocalTime.MIDNIGHT))
            .medVedtakResultatType(VedtakResultatType.INNVILGET);

        UttakAktivitetEntitet aktivitet = new UttakAktivitetEntitet.Builder()
            .medUttakArbeidType(UttakArbeidType.FRILANS)
            .build();
        LocalDate uttakStart = LocalDate.of(2018, 5, 14);
        // Uttak mødrekvote
        UttakResultatPeriodeEntitet uttakMødrekvote = new UttakResultatPeriodeEntitet.Builder(uttakStart, uttakStart.plusWeeks(6).minusDays(1))
            .medPeriodeResultat(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
            .build();
        UttakResultatPeriodeAktivitetEntitet.builder(uttakMødrekvote, aktivitet)
            .medTrekkdager(new Trekkdager(30))
            .medTrekkonto(StønadskontoType.MØDREKVOTE)
            .medArbeidsprosent(BigDecimal.TEN).build();
        UttakResultatPerioderEntitet uttak = new UttakResultatPerioderEntitet();
        uttak.leggTilPeriode(uttakMødrekvote);
        morFørstegangScenario.medUttak(uttak);

        Behandling morsFørsteBehandling = morFørstegangScenario.lagre(repositoryProvider);
        repositoryProvider.getFagsakRelasjonRepository().opprettRelasjon(morsFørsteBehandling.getFagsak(), Dekningsgrad._100);

        morsFørsteBehandling.avsluttBehandling();
        repositoryProvider.getBehandlingRepository().lagre(morsFørsteBehandling,
            repositoryProvider.getBehandlingLåsRepository().taLås(morsFørsteBehandling.getId()));

        // Arrange
        ScenarioMorSøkerForeldrepenger morRevurdering = ScenarioMorSøkerForeldrepenger.forFødsel();
        morRevurdering.medBehandlingType(BehandlingType.REVURDERING);
        morRevurdering.medOriginalBehandling(morsFørsteBehandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);
        morRevurdering.medBehandlingVedtak()
            .medVedtakstidspunkt(LocalDateTime.of(LocalDate.of(2019, 8, 2), LocalTime.MIDNIGHT))
            .medVedtakResultatType(VedtakResultatType.INNVILGET);

        UttakAktivitetEntitet aktivitet2 = new UttakAktivitetEntitet.Builder()
            .medUttakArbeidType(UttakArbeidType.FRILANS)
            .build();
        // Uttak mødrekvote
        UttakResultatPeriodeEntitet uttakMødrekvote2 = new UttakResultatPeriodeEntitet.Builder(uttakStart, uttakStart.plusWeeks(6).minusDays(1))
            .medPeriodeResultat(PeriodeResultatType.AVSLÅTT, PeriodeResultatÅrsak.UKJENT)
            .build();
        UttakResultatPeriodeAktivitetEntitet.builder(uttakMødrekvote2, aktivitet2)
            .medTrekkdager(new Trekkdager(30))
            .medTrekkonto(StønadskontoType.MØDREKVOTE)
            .medArbeidsprosent(BigDecimal.ZERO).build();
        UttakResultatPerioderEntitet uttak2 = new UttakResultatPerioderEntitet();
        uttak2.leggTilPeriode(uttakMødrekvote2);
        morRevurdering.medUttak(uttak2);

        Behandling morsRevurdering = morRevurdering.lagre(repositoryProvider);

        morsRevurdering.avsluttBehandling();
        repositoryProvider.getBehandlingRepository().lagre(morsRevurdering, repositoryProvider.getBehandlingLåsRepository().taLås(morsRevurdering.getId()));

        Behandling farsBehandling = ScenarioFarSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);
        farsBehandling = repositoryProvider.getBehandlingRepository().hentBehandling(farsBehandling.getId());
        repositoryProvider.getFagsakRelasjonRepository().kobleFagsaker(morsRevurdering.getFagsak(), farsBehandling.getFagsak(), morsRevurdering);

        // Act
        Optional<UttakResultatEntitet> annenPartsGjeldendeVedtattUttaksplan = relatertBehandlingTjeneste
            .hentAnnenPartsGjeldendeVedtattUttaksplan(farsBehandling.getFagsak().getSaksnummer());

        // Assert
        assertThat(annenPartsGjeldendeVedtattUttaksplan.get().getGjeldendePerioder().getPerioder().get(0).isInnvilget()).isFalse();
    }

    @Test
    public void skal_finne_gjeldende_uttak_fra_annenpart_på_søkers_vedtakstidspunkt_ved_flere_annenpart_behandlinger_med_vedtak_på_samme_dato() {
        // Arrange
        ScenarioMorSøkerForeldrepenger morFørstegangScenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        morFørstegangScenario.medBehandlingVedtak()
            .medVedtakstidspunkt(LocalDateTime.of(LocalDate.of(2019, 8, 1), LocalTime.MIDNIGHT))
            .medVedtakResultatType(VedtakResultatType.INNVILGET);

        UttakAktivitetEntitet aktivitet = new UttakAktivitetEntitet.Builder()
            .medUttakArbeidType(UttakArbeidType.FRILANS)
            .build();
        LocalDate uttakStart = LocalDate.of(2018, 5, 14);
        // Uttak mødrekvote
        UttakResultatPeriodeEntitet uttakMødrekvote = new UttakResultatPeriodeEntitet.Builder(uttakStart, uttakStart.plusWeeks(6).minusDays(1))
            .medPeriodeResultat(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
            .build();
        UttakResultatPeriodeAktivitetEntitet.builder(uttakMødrekvote, aktivitet)
            .medTrekkdager(new Trekkdager(30))
            .medTrekkonto(StønadskontoType.MØDREKVOTE)
            .medArbeidsprosent(BigDecimal.TEN).build();
        UttakResultatPerioderEntitet uttak = new UttakResultatPerioderEntitet();
        uttak.leggTilPeriode(uttakMødrekvote);
        morFørstegangScenario.medUttak(uttak);

        Behandling morsFørsteBehandling = morFørstegangScenario.lagre(repositoryProvider);
        repositoryProvider.getFagsakRelasjonRepository().opprettRelasjon(morsFørsteBehandling.getFagsak(), Dekningsgrad._100);

        morsFørsteBehandling.avsluttBehandling();
        repositoryProvider.getBehandlingRepository().lagre(morsFørsteBehandling,
            repositoryProvider.getBehandlingLåsRepository().taLås(morsFørsteBehandling.getId()));

        // Arrange
        ScenarioMorSøkerForeldrepenger morRevurdering = ScenarioMorSøkerForeldrepenger.forFødsel();
        morRevurdering.medBehandlingType(BehandlingType.REVURDERING);
        morRevurdering.medOriginalBehandling(morsFørsteBehandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);
        morRevurdering.medBehandlingVedtak()
            .medVedtakstidspunkt(LocalDateTime.of(LocalDate.of(2019, 8, 1), LocalTime.MIDNIGHT))
            .medVedtakResultatType(VedtakResultatType.INNVILGET);

        UttakAktivitetEntitet aktivitet2 = new UttakAktivitetEntitet.Builder()
            .medUttakArbeidType(UttakArbeidType.FRILANS)
            .build();
        // Uttak mødrekvote
        UttakResultatPeriodeEntitet uttakMødrekvote2 = new UttakResultatPeriodeEntitet.Builder(uttakStart, uttakStart.plusWeeks(6).minusDays(1))
            .medPeriodeResultat(PeriodeResultatType.AVSLÅTT, PeriodeResultatÅrsak.UKJENT)
            .build();
        UttakResultatPeriodeAktivitetEntitet.builder(uttakMødrekvote2, aktivitet2)
            .medTrekkdager(new Trekkdager(30))
            .medTrekkonto(StønadskontoType.MØDREKVOTE)
            .medArbeidsprosent(BigDecimal.ZERO).build();
        UttakResultatPerioderEntitet uttak2 = new UttakResultatPerioderEntitet();
        uttak2.leggTilPeriode(uttakMødrekvote2);
        morRevurdering.medUttak(uttak2);

        Behandling morsRevurdering = morRevurdering.lagre(repositoryProvider);

        morsRevurdering.avsluttBehandling();
        repositoryProvider.getBehandlingRepository().lagre(morsRevurdering, repositoryProvider.getBehandlingLåsRepository().taLås(morsRevurdering.getId()));

        ScenarioFarSøkerForeldrepenger farsScenario = ScenarioFarSøkerForeldrepenger.forFødsel();
        farsScenario.medBehandlingVedtak()
            .medVedtakstidspunkt(LocalDateTime.of(LocalDate.of(2019, 8, 2), LocalTime.MIDNIGHT))
            .medVedtakResultatType(VedtakResultatType.INNVILGET);
        Behandling farsBehandling = farsScenario.lagre(repositoryProvider);
        farsBehandling = repositoryProvider.getBehandlingRepository().hentBehandling(farsBehandling.getId());
        repositoryProvider.getFagsakRelasjonRepository().kobleFagsaker(morsRevurdering.getFagsak(), farsBehandling.getFagsak(), morsRevurdering);

        // Act
        Optional<UttakResultatEntitet> annenPartsGjeldendeVedtattUttaksplan = relatertBehandlingTjeneste
            .hentAnnenPartsGjeldendeUttaksplanPåVedtakstidspunkt(farsBehandling);

        // Assert
        assertThat(annenPartsGjeldendeVedtattUttaksplan.get().getGjeldendePerioder().getPerioder().get(0).isInnvilget()).isFalse();
    }
}
