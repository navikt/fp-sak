package no.nav.foreldrepenger.mottak.vedtak.overlapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadAnnenPartType;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.tid.VirkedagUtil;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.mottak.dokumentmottak.impl.Kompletthetskontroller;
import no.nav.foreldrepenger.mottak.sakskompleks.KøKontroller;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.task.OpprettOppgaveVurderKonsekvensTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.felles.prosesstask.api.TaskType;

@ExtendWith(MockitoExtension.class)
class HåndterOpphørAvYtelserTest extends EntityManagerAwareTest {

    private static final OrganisasjonsEnhet ENHET = new OrganisasjonsEnhet("4833", "NFP");

    private static final LocalDate FØDSELS_DATO_1 = VirkedagUtil.fomVirkedag(LocalDate.now().minusMonths(2));
    private static final LocalDate SISTE_DAG_MOR = FØDSELS_DATO_1.plusWeeks(6);

    private static final LocalDate SKJÆRINGSTIDSPUNKT_OVERLAPPER_BEH_1 = VirkedagUtil.fomVirkedag(
        LocalDate.now().minusMonths(1));
    private static final LocalDate SISTE_DAG_PER_OVERLAPP = SKJÆRINGSTIDSPUNKT_OVERLAPPER_BEH_1.plusWeeks(6);

    private static final int DAGSATS = 100;
    private static final AktørId AKTØR_ID_MOR = AktørId.dummy();
    private static final String BESKRIVELSE = "beskrivelse";

    private HåndterOpphørAvYtelser håndterOpphørAvYtelser;

    private BehandlingRepositoryProvider repositoryProvider;
    private FagsakRepository fagsakRepository;
    private BeregningsresultatRepository beregningsresultatRepository;
    @Mock
    private ProsessTaskTjeneste taskTjeneste;
    private final BehandlendeEnhetTjeneste behandlendeEnhetTjeneste = Mockito.mock(BehandlendeEnhetTjeneste.class);

    @Mock
    private RevurderingTjeneste revurderingTjenesteMockFP;
    @Mock
    private RevurderingTjeneste revurderingTjenesteMockSVP;
    @Mock
    private BehandlingProsesseringTjeneste behandlingProsesseringTjenesteMock;

    @BeforeEach
    public void setUp() {
        var entityManager = getEntityManager();
        lenient().when(behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(any())).thenReturn(ENHET);
        lenient().when(behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(any(), any(String.class))).thenReturn(ENHET);
        lenient().when(behandlendeEnhetTjeneste.finnBehandlendeEnhetFra(any())).thenReturn(ENHET);
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        beregningsresultatRepository = new BeregningsresultatRepository(entityManager);
        fagsakRepository = new FagsakRepository(entityManager);
        håndterOpphørAvYtelser = new HåndterOpphørAvYtelser(repositoryProvider, revurderingTjenesteMockFP,
            revurderingTjenesteMockSVP, taskTjeneste, behandlendeEnhetTjeneste, behandlingProsesseringTjenesteMock,
            mock(KøKontroller.class), mock(Kompletthetskontroller.class));
    }

    @Test
    void skal_opprette_revurdering_når_fagsak_ikke_har_åpen_behandling() {
        // Arrange
        var avsluttetBehMor = lagBehandlingMor(FØDSELS_DATO_1, AKTØR_ID_MOR, null);
        var berResMorBeh1 = lagBeregningsresultat(FØDSELS_DATO_1, SISTE_DAG_MOR, Inntektskategori.ARBEIDSTAKER);
        beregningsresultatRepository.lagre(avsluttetBehMor, berResMorBeh1);
        var fsavsluttetBehMor = avsluttetBehMor.getFagsak();

        var nyAvsBehandlingMor = lagBehandlingMor(SKJÆRINGSTIDSPUNKT_OVERLAPPER_BEH_1, AKTØR_ID_MOR, null);
        var berResMorOverlapp = lagBeregningsresultat(SKJÆRINGSTIDSPUNKT_OVERLAPPER_BEH_1, SISTE_DAG_PER_OVERLAPP,
            Inntektskategori.ARBEIDSTAKER);
        beregningsresultatRepository.lagre(nyAvsBehandlingMor, berResMorOverlapp);

        // Act
        håndterOpphørAvYtelser.oppdaterEllerOpprettRevurdering(fsavsluttetBehMor, BESKRIVELSE,
            BehandlingÅrsakType.OPPHØR_YTELSE_NYTT_BARN);

        // Assert
        verify(revurderingTjenesteMockFP, times(1)).opprettAutomatiskRevurdering(eq(fsavsluttetBehMor),
            eq(BehandlingÅrsakType.OPPHØR_YTELSE_NYTT_BARN), any());

        var captor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(taskTjeneste).lagre(captor.capture());
        var vurderKonsekvens = captor.getValue();
        assertThat(vurderKonsekvens.taskType()).isEqualTo(
            TaskType.forProsessTask(OpprettOppgaveVurderKonsekvensTask.class));
        assertThat(vurderKonsekvens.getFagsakId()).isEqualTo(fsavsluttetBehMor.getId());
        assertThat(vurderKonsekvens.getPropertyValue(OpprettOppgaveVurderKonsekvensTask.KEY_BESKRIVELSE)).isEqualTo(
            BESKRIVELSE);
    }

    @Test
    void skal_ikke_opprette_revurdering_når_fagsak_allerede_har_åpen_behandling() {
        // Arrange
        var avsluttetBehMor = lagBehandlingMor(FØDSELS_DATO_1, AKTØR_ID_MOR, null);
        var berResMorBeh1 = lagBeregningsresultat(FØDSELS_DATO_1, SISTE_DAG_MOR, Inntektskategori.ARBEIDSTAKER);
        beregningsresultatRepository.lagre(avsluttetBehMor, berResMorBeh1);
        var fsavsluttetBehMor = avsluttetBehMor.getFagsak();
        var revurdering = Behandling.nyBehandlingFor(fsavsluttetBehMor, BehandlingType.REVURDERING).build();
        repositoryProvider.getBehandlingRepository()
            .lagre(revurdering, repositoryProvider.getBehandlingRepository().taSkriveLås(revurdering));

        var nyAvsBehandlingMor = lagBehandlingMor(SKJÆRINGSTIDSPUNKT_OVERLAPPER_BEH_1, AKTØR_ID_MOR, null);
        var berResMorOverlapp = lagBeregningsresultat(SKJÆRINGSTIDSPUNKT_OVERLAPPER_BEH_1, SISTE_DAG_PER_OVERLAPP,
            Inntektskategori.ARBEIDSTAKER);
        beregningsresultatRepository.lagre(nyAvsBehandlingMor, berResMorOverlapp);

        // Act
        håndterOpphørAvYtelser.oppdaterEllerOpprettRevurdering(fsavsluttetBehMor, BESKRIVELSE,
            BehandlingÅrsakType.OPPHØR_YTELSE_NYTT_BARN);

        // Assert
        verify(revurderingTjenesteMockFP, times(0)).opprettAutomatiskRevurdering(any(), any(), any());

        var captor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(taskTjeneste).lagre(captor.capture());
        var vurderKonsekvens = captor.getValue();
        assertThat(vurderKonsekvens.taskType()).isEqualTo(
            TaskType.forProsessTask(OpprettOppgaveVurderKonsekvensTask.class));
        assertThat(vurderKonsekvens.getFagsakId()).isEqualTo(fsavsluttetBehMor.getId());
        assertThat(vurderKonsekvens.getPropertyValue(OpprettOppgaveVurderKonsekvensTask.KEY_BESKRIVELSE)).isEqualTo(
            BESKRIVELSE);

        assertThat(revurdering.harBehandlingÅrsak(BehandlingÅrsakType.OPPHØR_YTELSE_NYTT_BARN)).isTrue();
    }

    private Behandling lagBehandlingMor(LocalDate fødselsDato, AktørId aktørId, AktørId medfAktørId) {
        var scenarioAvsluttetBehMor = ScenarioMorSøkerForeldrepenger.forFødselMedGittAktørId(aktørId);
        scenarioAvsluttetBehMor.medSøknadHendelse().medFødselsDato(fødselsDato);
        if (medfAktørId != null) {
            scenarioAvsluttetBehMor.medSøknadAnnenPart()
                .medAktørId(medfAktørId)
                .medNavn("Seig Pinne")
                .medType(SøknadAnnenPartType.FAR);
        }
        scenarioAvsluttetBehMor.medBehandlingsresultat(
            Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET));
        scenarioAvsluttetBehMor.medBehandlingVedtak()
            .medVedtakstidspunkt(LocalDateTime.now().minusMonths(2))
            .medVedtakResultatType(VedtakResultatType.INNVILGET);
        var behandling = scenarioAvsluttetBehMor.lagre(repositoryProvider);
        avsluttBehandlingOgFagsak(behandling);
        return behandling;
    }

    private BeregningsresultatEntitet lagBeregningsresultat(LocalDate periodeFom,
                                                            LocalDate periodeTom,
                                                            Inntektskategori inntektskategori) {
        var beregningsresultat = BeregningsresultatEntitet.builder()
            .medRegelInput("input")
            .medRegelSporing("sporing")
            .build();
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
        return beregningsresultat;
    }

    private void avsluttBehandlingOgFagsak(Behandling behandling) {
        behandling.avsluttBehandling();
        repositoryProvider.getBehandlingRepository()
            .lagre(behandling, repositoryProvider.getBehandlingRepository().taSkriveLås(behandling));
        fagsakRepository.oppdaterFagsakStatus(behandling.getFagsakId(), FagsakStatus.LØPENDE);
    }
}
