package no.nav.foreldrepenger.ytelse.beregning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.DekningsgradTjeneste;
import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BehandlingBeregningsresultatBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatFeriepenger;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.JpaExtension;
import no.nav.foreldrepenger.ytelse.beregning.adapter.MapInputFraVLTilRegelGrunnlag;
import no.nav.foreldrepenger.ytelse.beregning.fp.BeregnFeriepenger;

@ExtendWith(JpaExtension.class)
@ExtendWith(MockitoExtension.class)
class BeregnFeriepengerTjenesteTest {

    @Mock
    private MapInputFraVLTilRegelGrunnlag inputTjeneste;

    private static final LocalDate SKJÆRINGSTIDSPUNKT_MOR = LocalDate.of(2018, 12, 1);
    private static final LocalDate SKJÆRINGSTIDSPUNKT_FAR = SKJÆRINGSTIDSPUNKT_MOR.plusWeeks(6);
    private static final LocalDate SISTE_DAG_FAR = SKJÆRINGSTIDSPUNKT_FAR.plusWeeks(4);
    private static final int DAGSATS = 123;

    private EntityManager entityManager;
    private BehandlingRepositoryProvider repositoryProvider;
    private BeregningsresultatRepository beregningsresultatRepository;

    private BeregnFeriepengerTjeneste tjeneste;
    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;

    @BeforeEach
    void setUp(EntityManager entityManager) {
        this.entityManager = entityManager;
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        beregningsresultatRepository = repositoryProvider.getBeregningsresultatRepository();
        Mockito.when(inputTjeneste.arbeidstakerVedSkjæringstidspunkt(any())).thenReturn(true);
        fagsakRelasjonTjeneste = new FagsakRelasjonTjeneste(repositoryProvider);
        var dekningsgradTjeneste = new DekningsgradTjeneste(repositoryProvider.getYtelsesFordelingRepository());
        tjeneste = new BeregnFeriepenger(repositoryProvider, inputTjeneste, fagsakRelasjonTjeneste, dekningsgradTjeneste, 60);
    }

    @Test
    void skalBeregneFeriepenger() {
        var farsBehandling = lagBehandlingFar();
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medDefaultOppgittDekningsgrad();
        var morsBehandling = scenario.lagre(repositoryProvider);
        fagsakRelasjonTjeneste.opprettRelasjon(morsBehandling.getFagsak());
        fagsakRelasjonTjeneste.kobleFagsaker(morsBehandling.getFagsak(), farsBehandling.getFagsak());
        var morsBeregningsresultatFP = lagBeregningsresultatFP(SKJÆRINGSTIDSPUNKT_MOR, SKJÆRINGSTIDSPUNKT_FAR,
                Inntektskategori.ARBEIDSTAKER);

        // Act
        var ref = BehandlingReferanse.fra(morsBehandling);
        var feriepenger = tjeneste.beregnFeriepenger(ref, morsBeregningsresultatFP);

        // Assert
        assertBeregningsresultatFeriepenger(feriepenger);
    }

    @Test
    void skalBeregneFeriepenger_flere_perioder_samme_resultat() {
        var farsBehandling = lagBehandlingFar();
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medDefaultOppgittDekningsgrad();
        var morsBehandling = scenario.lagre(repositoryProvider);
        fagsakRelasjonTjeneste.opprettRelasjon(morsBehandling.getFagsak());
        fagsakRelasjonTjeneste.kobleFagsaker(morsBehandling.getFagsak(), farsBehandling.getFagsak());
        var morsBeregningsresultatFP = BeregningsresultatEntitet.builder().medRegelInput("input").medRegelSporing("sporing").build();
        lagBeregningsresultaPeriode(morsBeregningsresultatFP, SKJÆRINGSTIDSPUNKT_MOR, SKJÆRINGSTIDSPUNKT_MOR.plusWeeks(1).minusDays(1), Inntektskategori.ARBEIDSTAKER);
        lagBeregningsresultaPeriode(morsBeregningsresultatFP, SKJÆRINGSTIDSPUNKT_MOR.plusWeeks(1), SKJÆRINGSTIDSPUNKT_FAR.minusWeeks(1).minusDays(1), Inntektskategori.ARBEIDSTAKER);
        lagBeregningsresultaPeriode(morsBeregningsresultatFP, SKJÆRINGSTIDSPUNKT_FAR.minusWeeks(1), SKJÆRINGSTIDSPUNKT_FAR.minusDays(1), Inntektskategori.ARBEIDSTAKER);

        // Act
        var ref = BehandlingReferanse.fra(morsBehandling);
        var feriepenger = tjeneste.beregnFeriepenger(ref, morsBeregningsresultatFP);

        // Assert
        assertBeregningsresultatFeriepenger(feriepenger);
    }

    @Test
    void skalSjekkeFeriepengerMedAvvik() {
        var farsBehandling = lagBehandlingFar();
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medDefaultOppgittDekningsgrad();
        var morsBehandling = scenario.lagre(repositoryProvider);
        fagsakRelasjonTjeneste.opprettRelasjon(morsBehandling.getFagsak());
        fagsakRelasjonTjeneste.kobleFagsaker(morsBehandling.getFagsak(), farsBehandling.getFagsak());
        var morsBeregningsresultatFP = lagBeregningsresultatFP(SKJÆRINGSTIDSPUNKT_MOR, SKJÆRINGSTIDSPUNKT_FAR,
            Inntektskategori.ARBEIDSTAKER);
        var morBrR = BehandlingBeregningsresultatBuilder.ny().medBgBeregningsresultatFP(morsBeregningsresultatFP).build(1L);
        // Act
        var ref = BehandlingReferanse.fra(morsBehandling);
        var avvik = tjeneste.avvikBeregnetFeriepengerBeregningsresultat(ref, morBrR);

        // Assert
        assertThat(avvik).isTrue();
    }

    @Test
    void skalSjekkeFeriepengerUtenAvvik() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medDefaultOppgittDekningsgrad();
        var morsBehandling = scenario.lagre(repositoryProvider);
        fagsakRelasjonTjeneste.opprettRelasjon(morsBehandling.getFagsak());
        var morsBeregningsresultatFP = lagBeregningsresultatFP(SKJÆRINGSTIDSPUNKT_MOR, SKJÆRINGSTIDSPUNKT_MOR.plusMonths(6),
            Inntektskategori.ARBEIDSTAKER_UTEN_FERIEPENGER);
        var morBrR = BehandlingBeregningsresultatBuilder.ny().medBgBeregningsresultatFP(morsBeregningsresultatFP).build(1L);

        // Act
        var ref = BehandlingReferanse.fra(morsBehandling);
        var avvik = tjeneste.avvikBeregnetFeriepengerBeregningsresultat(ref, morBrR);

        // Assert
        assertThat(avvik).isFalse();
    }

    @Test
    void skalIkkeBeregneFeriepenger() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medDefaultOppgittDekningsgrad();
        var morsBehandling = scenario.lagre(repositoryProvider);
        fagsakRelasjonTjeneste.opprettRelasjon(morsBehandling.getFagsak());
        var morsBeregningsresultatFP = lagBeregningsresultatFP(SKJÆRINGSTIDSPUNKT_MOR, SKJÆRINGSTIDSPUNKT_MOR.plusMonths(6),
                Inntektskategori.ARBEIDSTAKER_UTEN_FERIEPENGER);

        // Act
        var ref = BehandlingReferanse.fra(morsBehandling);
        var feriepenger = tjeneste.beregnFeriepenger(ref, morsBeregningsresultatFP);

        // Assert
        assertThat(feriepenger.getBeregningsresultatFeriepengerPrÅrListe()).isEmpty();
        assertThat(feriepenger.getFeriepengerPeriodeFom()).isNull();
        assertThat(feriepenger.getFeriepengerPeriodeTom()).isNull();
        assertThat(feriepenger.getFeriepengerRegelInput()).isNotNull();
        assertThat(feriepenger.getFeriepengerRegelSporing()).isNotNull();
    }

    private void assertBeregningsresultatFeriepenger(BeregningsresultatFeriepenger feriepenger) {
        assertThat(feriepenger.getFeriepengerPeriodeFom()).as("FeriepengerPeriodeFom").isEqualTo(SKJÆRINGSTIDSPUNKT_MOR);
        assertThat(feriepenger.getFeriepengerPeriodeTom()).as("FeriepengerPeriodeTom").isEqualTo(SISTE_DAG_FAR);
        var beregningsresultatFeriepengerPrÅrListe = feriepenger.getBeregningsresultatFeriepengerPrÅrListe();
        assertThat(beregningsresultatFeriepengerPrÅrListe).as("beregningsresultatFeriepengerPrÅrListe").hasSize(2);
        var prÅr1 = beregningsresultatFeriepengerPrÅrListe.stream().filter(f -> f.getOpptjeningsår().getYear() == 2018).findFirst().orElseThrow();
        assertThat(prÅr1.getOpptjeningsår()).as("prÅr1.opptjeningsår").isEqualTo(LocalDate.of(2018, 12, 31));
        assertThat(prÅr1.getÅrsbeløp().getVerdi()).as("prÅr1.årsbeløp").isEqualTo(BigDecimal.valueOf(263)); // DAGSATS * 21 * 0.102
        assertThat(prÅr1.getAktivitetStatus()).isEqualTo(AktivitetStatus.ARBEIDSTAKER);
        assertThat(prÅr1.erBrukerMottaker()).isTrue();
        var prÅr2 = beregningsresultatFeriepengerPrÅrListe.stream().filter(f -> f.getOpptjeningsår().getYear() == 2019).findFirst().orElseThrow();
        assertThat(prÅr2.getOpptjeningsår()).as("prÅr2.opptjeningsår").isEqualTo(LocalDate.of(2019, 12, 31));
        assertThat(prÅr2.getÅrsbeløp().getVerdi()).as("prÅr2.årsbeløp").isEqualTo(BigDecimal.valueOf(113)); // DAGSATS * 9 * 0.102
        assertThat(prÅr1.getAktivitetStatus()).isEqualTo(AktivitetStatus.ARBEIDSTAKER);
        assertThat(prÅr1.erBrukerMottaker()).isTrue();
    }

    private Behandling lagBehandlingFar() {
        var scenarioAnnenPart = ScenarioFarSøkerForeldrepenger.forFødsel();
        scenarioAnnenPart.medBehandlingVedtak()
                .medVedtakstidspunkt(LocalDateTime.now())
                .medVedtakResultatType(VedtakResultatType.INNVILGET);
        var farsBehandling = scenarioAnnenPart.lagre(repositoryProvider);
        var behandlingsresultat = repositoryProvider.getBehandlingsresultatRepository().hent(farsBehandling.getId());
        Behandlingsresultat.builderEndreEksisterende(behandlingsresultat).medBehandlingResultatType(BehandlingResultatType.INNVILGET);
        entityManager.persist(behandlingsresultat);
        farsBehandling.avsluttBehandling();
        entityManager.persist(farsBehandling);

        var farsBeregningsresultatFP = lagBeregningsresultatFP(SKJÆRINGSTIDSPUNKT_FAR, SISTE_DAG_FAR,
                Inntektskategori.ARBEIDSTAKER);

        beregningsresultatRepository.lagre(farsBehandling, farsBeregningsresultatFP, null);
        return farsBehandling;
    }

    private BeregningsresultatEntitet lagBeregningsresultatFP(LocalDate periodeFom, LocalDate periodeTom, Inntektskategori inntektskategori) {
        var beregningsresultat = BeregningsresultatEntitet.builder().medRegelInput("input").medRegelSporing("sporing").build();
        lagBeregningsresultaPeriode(beregningsresultat, periodeFom, periodeTom, inntektskategori);
        return beregningsresultat;
    }

    private BeregningsresultatPeriode lagBeregningsresultaPeriode(BeregningsresultatEntitet beregningsresultat, LocalDate periodeFom, LocalDate periodeTom, Inntektskategori inntektskategori) {
        var beregningsresultatPeriode = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(periodeFom, periodeTom)
            .build(beregningsresultat);
        BeregningsresultatAndel.builder()
            .medInntektskategori(inntektskategori)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medDagsats(DAGSATS)
            .medDagsatsFraBg(DAGSATS)
            .medBrukerErMottaker(true)
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medStillingsprosent(BigDecimal.valueOf(100))
            .build(beregningsresultatPeriode);
        return beregningsresultatPeriode;
    }
}
