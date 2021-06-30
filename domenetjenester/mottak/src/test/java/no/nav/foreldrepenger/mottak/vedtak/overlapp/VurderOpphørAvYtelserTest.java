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
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
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
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerSvangerskapspenger;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.tid.VirkedagUtil;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.mottak.sakskompleks.KøKontroller;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.task.OpprettOppgaveVurderKonsekvensTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskStatus;
import no.nav.vedtak.felles.prosesstask.impl.ProsessTaskEventPubliserer;
import no.nav.vedtak.felles.prosesstask.impl.ProsessTaskRepositoryImpl;

@ExtendWith(MockitoExtension.class)
public class VurderOpphørAvYtelserTest extends EntityManagerAwareTest {

    private static final LocalDate FØDSELS_DATO_1 = VirkedagUtil.fomVirkedag(LocalDate.now().minusMonths(2));
    private static final LocalDate SISTE_DAG_MOR = FØDSELS_DATO_1.plusWeeks(6);

    private static final LocalDate SKJÆRINGSTIDSPUNKT_OVERLAPPER_BEH_1 = VirkedagUtil.fomVirkedag(LocalDate.now().minusMonths(1));
    private static final LocalDate SISTE_DAG_PER_OVERLAPP = SKJÆRINGSTIDSPUNKT_OVERLAPPER_BEH_1.plusWeeks(6);

    private static final LocalDate SKJÆRINGSTIDSPUNKT_OVERLAPPER_IKKE_BEH_1 = VirkedagUtil.fomVirkedag(LocalDate.now().plusMonths(1));
    private static final LocalDate SISTE_DAG_IKKE_OVERLAPP = SKJÆRINGSTIDSPUNKT_OVERLAPPER_IKKE_BEH_1.plusWeeks(6);

    private static final int DAGSATS = 100;
    private static final int DAGSATS_GRADERING = 50;
    private static final AktørId AKTØR_ID_MOR = AktørId.dummy();
    private static final AktørId MEDF_AKTØR_ID = AktørId.dummy();

    private VurderOpphørAvYtelser vurderOpphørAvYtelser;
    private final OverlappFPInfotrygdTjeneste sjekkInfotrygdTjeneste = mock(OverlappFPInfotrygdTjeneste.class);

    private BehandlingRepositoryProvider repositoryProvider;
    private FagsakRepository fagsakRepository;
    private BeregningsresultatRepository beregningsresultatRepository;
    private final ProsessTaskEventPubliserer eventPubliserer = Mockito.mock(ProsessTaskEventPubliserer.class);
    private ProsessTaskRepository prosessTaskRepository;
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
        lenient().when(behandlendeEnhetTjeneste.gyldigEnhetNfpNk(any())).thenReturn(true);
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        beregningsresultatRepository = new BeregningsresultatRepository(entityManager);
        fagsakRepository = new FagsakRepository(entityManager);
        prosessTaskRepository = new ProsessTaskRepositoryImpl(entityManager, null, eventPubliserer);
        vurderOpphørAvYtelser = new VurderOpphørAvYtelser(repositoryProvider, revurderingTjenesteMockFP, revurderingTjenesteMockSVP,
            prosessTaskRepository, behandlendeEnhetTjeneste, behandlingProsesseringTjenesteMock, sjekkInfotrygdTjeneste, mock(KøKontroller.class), entityManager);
    }

    @Test
    public void opphørLøpendeSakNårNySakOverlapperPåMor() {
        var avsluttetBehMor = lagBehandlingMor(FØDSELS_DATO_1, AKTØR_ID_MOR, null);
        var berResMorBeh1 = lagBeregningsresultat(FØDSELS_DATO_1, SISTE_DAG_MOR, Inntektskategori.ARBEIDSTAKER);
        beregningsresultatRepository.lagre(avsluttetBehMor, berResMorBeh1);
        var fsavsluttetBehMor = avsluttetBehMor.getFagsak();

        var nyAvsBehandlingMor = lagBehandlingMor(SKJÆRINGSTIDSPUNKT_OVERLAPPER_BEH_1, AKTØR_ID_MOR, null);
        var berResMorOverlapp = lagBeregningsresultat(SKJÆRINGSTIDSPUNKT_OVERLAPPER_BEH_1, SISTE_DAG_PER_OVERLAPP, Inntektskategori.ARBEIDSTAKER);
        beregningsresultatRepository.lagre(nyAvsBehandlingMor, berResMorOverlapp);
        var fagsakNy = nyAvsBehandlingMor.getFagsak();

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(fagsakNy.getId(), nyAvsBehandlingMor.getId());

        verify(revurderingTjenesteMockFP, times(1)).opprettAutomatiskRevurdering(eq(fsavsluttetBehMor), eq(BehandlingÅrsakType.OPPHØR_YTELSE_NYTT_BARN), any());
        verify(sjekkInfotrygdTjeneste, times(0)).harForeldrepengerInfotrygdSomOverlapper(any(), any());
    }

    @Test
    public void opphørSakPåMorOgMedforelderNårNySakOverlapper() {
        var avsluttetBehMor = lagBehandlingMor(FØDSELS_DATO_1, AKTØR_ID_MOR, MEDF_AKTØR_ID);
        var berResMorBeh1 = lagBeregningsresultat(FØDSELS_DATO_1, SISTE_DAG_MOR, Inntektskategori.ARBEIDSTAKER);
        beregningsresultatRepository.lagre(avsluttetBehMor, berResMorBeh1);

        var nyAvsBehandlingMor = lagBehandlingMor(SKJÆRINGSTIDSPUNKT_OVERLAPPER_BEH_1, AKTØR_ID_MOR, MEDF_AKTØR_ID);
        var berResMorOverlapp = lagBeregningsresultat(SKJÆRINGSTIDSPUNKT_OVERLAPPER_BEH_1, SISTE_DAG_PER_OVERLAPP, Inntektskategori.ARBEIDSTAKER);
        beregningsresultatRepository.lagre(nyAvsBehandlingMor, berResMorOverlapp);
        var fagsakNy = nyAvsBehandlingMor.getFagsak();

        var avslBehFarMedOverlappMor = lagBehandlingFar(FØDSELS_DATO_1, MEDF_AKTØR_ID, AKTØR_ID_MOR);
        var berResFar = lagBeregningsresultat(SKJÆRINGSTIDSPUNKT_OVERLAPPER_BEH_1, SISTE_DAG_PER_OVERLAPP, Inntektskategori.ARBEIDSTAKER);
        beregningsresultatRepository.lagre(avslBehFarMedOverlappMor, berResFar);
        var fsavsluttetBehFar = avslBehFarMedOverlappMor.getFagsak();

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(fagsakNy.getId(),nyAvsBehandlingMor.getId());

        verify(revurderingTjenesteMockFP, times(1)).opprettAutomatiskRevurdering(eq(fsavsluttetBehFar), eq(BehandlingÅrsakType.OPPHØR_YTELSE_NYTT_BARN), any());
        verify(sjekkInfotrygdTjeneste, times(1)).harForeldrepengerInfotrygdSomOverlapper(fsavsluttetBehFar.getAktørId(),SKJÆRINGSTIDSPUNKT_OVERLAPPER_BEH_1 );
    }

    @Test
    public void adopsjonFarFørstSkalIkkeOpphøresAvMor() {

        var avsluttetBehFar = lagBehandlingFar(FØDSELS_DATO_1, MEDF_AKTØR_ID, AKTØR_ID_MOR);
        var berResFarBeh1 = lagBeregningsresultat(FØDSELS_DATO_1, SISTE_DAG_MOR, Inntektskategori.ARBEIDSTAKER);
        beregningsresultatRepository.lagre(avsluttetBehFar, berResFarBeh1);
        var fsavsluttetBehFar = avsluttetBehFar.getFagsak();

        var avsluttetBehMor = lagBehandlingMor(FØDSELS_DATO_1, AKTØR_ID_MOR, MEDF_AKTØR_ID);
        var berResMorBeh1 = lagBeregningsresultat(SKJÆRINGSTIDSPUNKT_OVERLAPPER_BEH_1, SISTE_DAG_PER_OVERLAPP, Inntektskategori.ARBEIDSTAKER);
        beregningsresultatRepository.lagre(avsluttetBehMor, berResMorBeh1);
        var fsavsluttetBehMor = avsluttetBehMor.getFagsak();

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(fsavsluttetBehMor.getId(),avsluttetBehMor.getId());

        // OBS: Dette vil vi helst ikke. Trenger sjekk på om sak gjelder samme barn
        verify(revurderingTjenesteMockFP, times(1)).opprettAutomatiskRevurdering(eq(fsavsluttetBehFar), eq(BehandlingÅrsakType.OPPHØR_YTELSE_NYTT_BARN), any());
    }

    @Test
    public void ikkeOpphørSakNårNySakIkkeOverlapper() {
        var avsluttetBehMor = lagBehandlingMor(FØDSELS_DATO_1, AKTØR_ID_MOR,null);
        var berResMorBeh1 = lagBeregningsresultat(FØDSELS_DATO_1, SISTE_DAG_MOR, Inntektskategori.ARBEIDSTAKER);
        leggTilBeregningsresPeriode(berResMorBeh1, SISTE_DAG_MOR, SISTE_DAG_MOR.plusWeeks(2), false);
        beregningsresultatRepository.lagre(avsluttetBehMor, berResMorBeh1);

        var nyBehMorSomIkkeOverlapper = lagBehandlingMor(SKJÆRINGSTIDSPUNKT_OVERLAPPER_IKKE_BEH_1, AKTØR_ID_MOR, null);
        var berResMorBeh2 = lagBeregningsresultat(SKJÆRINGSTIDSPUNKT_OVERLAPPER_IKKE_BEH_1, SKJÆRINGSTIDSPUNKT_OVERLAPPER_IKKE_BEH_1, Inntektskategori.ARBEIDSTAKER);
        leggTilBeregningsresPeriode(berResMorBeh2, SISTE_DAG_IKKE_OVERLAPP, SISTE_DAG_IKKE_OVERLAPP.plusWeeks(2), false);
        beregningsresultatRepository.lagre(nyBehMorSomIkkeOverlapper, berResMorBeh2);
        var fagsakNy = nyBehMorSomIkkeOverlapper.getFagsak();

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(fagsakNy.getId(), nyBehMorSomIkkeOverlapper.getId());
        verify(revurderingTjenesteMockFP, times(0)).opprettAutomatiskRevurdering(any(), any(), any());
        verify(sjekkInfotrygdTjeneste, times(0)).harForeldrepengerInfotrygdSomOverlapper(any(),any() );
    }

    @Test
    public void opphørSakPåMedforelderMenIkkeMor() {
        var avsluttetBehMor = lagBehandlingMor(FØDSELS_DATO_1, AKTØR_ID_MOR,MEDF_AKTØR_ID);
        var berResMorBeh1 = lagBeregningsresultat(FØDSELS_DATO_1, SISTE_DAG_MOR, Inntektskategori.ARBEIDSTAKER);
        leggTilBeregningsresPeriode(berResMorBeh1, SISTE_DAG_MOR, SISTE_DAG_MOR.plusWeeks(2), false);
        beregningsresultatRepository.lagre(avsluttetBehMor, berResMorBeh1);

        var nyBehMorSomIkkeOverlapper = lagBehandlingMor(SKJÆRINGSTIDSPUNKT_OVERLAPPER_IKKE_BEH_1, AKTØR_ID_MOR, MEDF_AKTØR_ID);
        var berResMorBeh2 = lagBeregningsresultat(SKJÆRINGSTIDSPUNKT_OVERLAPPER_IKKE_BEH_1, SKJÆRINGSTIDSPUNKT_OVERLAPPER_IKKE_BEH_1, Inntektskategori.ARBEIDSTAKER);
        leggTilBeregningsresPeriode(berResMorBeh2, SISTE_DAG_IKKE_OVERLAPP, SISTE_DAG_IKKE_OVERLAPP.plusWeeks(2), false);
        beregningsresultatRepository.lagre(nyBehMorSomIkkeOverlapper, berResMorBeh2);
        var fagsakNy = nyBehMorSomIkkeOverlapper.getFagsak();

        var avslBehFarMedOverlappMor = lagBehandlingFar(SKJÆRINGSTIDSPUNKT_OVERLAPPER_IKKE_BEH_1, MEDF_AKTØR_ID, AKTØR_ID_MOR);
        var berResFar = lagBeregningsresultat(SKJÆRINGSTIDSPUNKT_OVERLAPPER_BEH_1.plusMonths(2), SISTE_DAG_PER_OVERLAPP.plusMonths(2), Inntektskategori.ARBEIDSTAKER);
        beregningsresultatRepository.lagre(avslBehFarMedOverlappMor, berResFar);
        var fsavsluttetBehFar = avslBehFarMedOverlappMor.getFagsak();

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(fagsakNy.getId(), nyBehMorSomIkkeOverlapper.getId());
        verify(revurderingTjenesteMockFP, times(1)).opprettAutomatiskRevurdering(eq(fsavsluttetBehFar), eq(BehandlingÅrsakType.OPPHØR_YTELSE_NYTT_BARN), any());
        verify(sjekkInfotrygdTjeneste, times(1)).harForeldrepengerInfotrygdSomOverlapper(fsavsluttetBehFar.getAktørId(),SKJÆRINGSTIDSPUNKT_OVERLAPPER_IKKE_BEH_1 );

    }

    @Test
    public void opphørSakPåFarNårNySakPåFarOverlapper() {
        var avslBehFar = lagBehandlingFar(FØDSELS_DATO_1, MEDF_AKTØR_ID, AKTØR_ID_MOR);
        var berResFarBeh1 = lagBeregningsresultat(FØDSELS_DATO_1, SISTE_DAG_MOR, Inntektskategori.ARBEIDSTAKER);
        leggTilBeregningsresPeriode(berResFarBeh1, SISTE_DAG_MOR, SISTE_DAG_MOR.plusWeeks(2), false);
        beregningsresultatRepository.lagre(avslBehFar, berResFarBeh1);

        var fsavsluttetBehFar = avslBehFar.getFagsak();

        var nyBehFar = lagBehandlingFar(SKJÆRINGSTIDSPUNKT_OVERLAPPER_BEH_1, MEDF_AKTØR_ID, AKTØR_ID_MOR);
        var berResFar = lagBeregningsresultat(SKJÆRINGSTIDSPUNKT_OVERLAPPER_BEH_1, SISTE_DAG_PER_OVERLAPP, Inntektskategori.ARBEIDSTAKER);
        beregningsresultatRepository.lagre(nyBehFar, berResFar);
        var fagsakNy = nyBehFar.getFagsak();

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(fagsakNy.getId(), nyBehFar.getId());
        verify(revurderingTjenesteMockFP, times(1)).opprettAutomatiskRevurdering(eq(fsavsluttetBehFar), eq(BehandlingÅrsakType.OPPHØR_YTELSE_NYTT_BARN), any());
        verify(sjekkInfotrygdTjeneste, times(1)).harForeldrepengerInfotrygdSomOverlapper(fagsakNy.getAktørId(),SKJÆRINGSTIDSPUNKT_OVERLAPPER_BEH_1 );
    }

    @Test
    public void opphørSakPåMorNårSisteUttakLikStartPåNyttUttak() {
        var avsluttetBehMor = lagBehandlingMor(FØDSELS_DATO_1, AKTØR_ID_MOR, null);
        var berResMorBeh1 = lagBeregningsresultat(FØDSELS_DATO_1, SISTE_DAG_MOR, Inntektskategori.ARBEIDSTAKER);
        beregningsresultatRepository.lagre(avsluttetBehMor, berResMorBeh1);
        var fsavsluttetBehMor = avsluttetBehMor.getFagsak();

        var nyAvsBehandlingMor = lagBehandlingMor(SISTE_DAG_MOR, AKTØR_ID_MOR, null);
        var berResMorOverlapp = lagBeregningsresultat(SISTE_DAG_MOR, SISTE_DAG_MOR.plusWeeks(3), Inntektskategori.ARBEIDSTAKER);
        beregningsresultatRepository.lagre(nyAvsBehandlingMor, berResMorOverlapp);
        var fagsakNy = nyAvsBehandlingMor.getFagsak();

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(fagsakNy.getId(), nyAvsBehandlingMor.getId());
        verify(revurderingTjenesteMockFP, times(1)).opprettAutomatiskRevurdering(eq(fsavsluttetBehMor), eq(BehandlingÅrsakType.OPPHØR_YTELSE_NYTT_BARN), any());
        verify(sjekkInfotrygdTjeneste, times(0)).harForeldrepengerInfotrygdSomOverlapper(any(),any() );
    }

    @Test
    public void ikkeOpphørSakPåMorNårOpphørErOpprettetAllerede() {
        var avsluttetBehMor = lagBehandlingMor(FØDSELS_DATO_1, AKTØR_ID_MOR, null);
        var berResMorBeh1 = lagBeregningsresultat(FØDSELS_DATO_1, SISTE_DAG_MOR, Inntektskategori.ARBEIDSTAKER);
        beregningsresultatRepository.lagre(avsluttetBehMor, berResMorBeh1);

        var nyAvsBehandlingMor = lagBehandlingMor(SISTE_DAG_MOR, AKTØR_ID_MOR, null);
        var berResMorOverlapp = lagBeregningsresultat(SISTE_DAG_MOR, SISTE_DAG_MOR.plusWeeks(3), Inntektskategori.ARBEIDSTAKER);
        beregningsresultatRepository.lagre(nyAvsBehandlingMor, berResMorOverlapp);
        var fagsakNy = nyAvsBehandlingMor.getFagsak();

        lagRevurdering(avsluttetBehMor);

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(fagsakNy.getId(), nyAvsBehandlingMor.getId());
        verify(revurderingTjenesteMockFP, times(0)).opprettAutomatiskRevurdering(any(), any(), any());
        verify(sjekkInfotrygdTjeneste, times(0)).harForeldrepengerInfotrygdSomOverlapper(any(),any() );
    }

    @Test
    public void oppretteTaskVurderKonsekvensIngenGjeldendeAktørId() {
        var KEY_GJELDENDE_AKTØR_ID="aktuellAktoerId";

        var nyBehMorSomIkkeOverlapper = lagBehandlingMor(SKJÆRINGSTIDSPUNKT_OVERLAPPER_IKKE_BEH_1, AKTØR_ID_MOR, null);
        var fagsakNy = nyBehMorSomIkkeOverlapper.getFagsak();

        vurderOpphørAvYtelser.opprettTaskForÅVurdereKonsekvens(fagsakNy.getId(),"Test", "Test", Optional.empty() );
        var vurderKonsekvens = prosessTaskRepository.finnIkkeStartet().stream().findFirst().orElse(null);
        var gjeldendeAktørId = Optional.ofNullable(vurderKonsekvens.getPropertyValue(KEY_GJELDENDE_AKTØR_ID));

        assertThat(gjeldendeAktørId.isPresent()).isFalse();
    }

    @Test
    public void oppretteTaskVurderKonsekvensMedAktørId() {
        var nyBehMorSomIkkeOverlapper = lagBehandlingMor(SKJÆRINGSTIDSPUNKT_OVERLAPPER_IKKE_BEH_1, AKTØR_ID_MOR, null);
        var fagsakNy = nyBehMorSomIkkeOverlapper.getFagsak();

        vurderOpphørAvYtelser.opprettTaskForÅVurdereKonsekvens(fagsakNy.getId(), "Test2",
            "Test2", Optional.of(nyBehMorSomIkkeOverlapper.getAktørId().getId()));
        var tasks = prosessTaskRepository.finnAlle(ProsessTaskStatus.KLAR);
        var vurderKonsekvensTaskForFagsak = tasks.stream()
            .filter(p -> p.getTaskType().equals(OpprettOppgaveVurderKonsekvensTask.TASKTYPE))
            .filter(p -> Objects.equals(p.getFagsakId(), nyBehMorSomIkkeOverlapper.getFagsakId()))
            .collect(Collectors.toList());
        assertThat(vurderKonsekvensTaskForFagsak).hasSize(1);
    }

    @Test
    public void opprettRevNårOverlappMedFPNårInnvSVPPåSammeBarn() {
        var avsluttetFPBehMor = lagBehandlingMor(FØDSELS_DATO_1, AKTØR_ID_MOR, null);
        var berResMorBeh1 = lagBeregningsresultat(FØDSELS_DATO_1, SISTE_DAG_MOR, Inntektskategori.ARBEIDSTAKER);
        beregningsresultatRepository.lagre(avsluttetFPBehMor, berResMorBeh1);

        var nyBehSVPOverlapper = lagBehandlingSVP(AKTØR_ID_MOR);
        var berResMedOverlapp = lagBeregningsresultat(FØDSELS_DATO_1.minusWeeks(3), FØDSELS_DATO_1.plusDays(4), Inntektskategori.ARBEIDSTAKER);
        beregningsresultatRepository.lagre(nyBehSVPOverlapper, berResMedOverlapp);
        var fagsakNy = nyBehSVPOverlapper.getFagsak();

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(fagsakNy.getId(), nyBehSVPOverlapper.getId());

        verify(revurderingTjenesteMockSVP, times(1)).opprettAutomatiskRevurdering(eq(fagsakNy), eq(BehandlingÅrsakType.OPPHØR_YTELSE_NYTT_BARN), any());
    }

    @Test
    public void opprettRevNårOverlappMedFPNårInnvilgerSVPPåNyttBarn() {
        var avsluttetFPBehMor = lagBehandlingMor(FØDSELS_DATO_1, AKTØR_ID_MOR, null);
        var berResMorBeh1 = lagBeregningsresultat(FØDSELS_DATO_1, SISTE_DAG_MOR, Inntektskategori.ARBEIDSTAKER);
        beregningsresultatRepository.lagre(avsluttetFPBehMor, berResMorBeh1);
        var avsluttetFPSakMor = avsluttetFPBehMor.getFagsak();

        var nyBehSVPOverlapper = lagBehandlingSVP(AKTØR_ID_MOR);
        var berResMedOverlapp = lagBeregningsresultat(SKJÆRINGSTIDSPUNKT_OVERLAPPER_BEH_1, SISTE_DAG_PER_OVERLAPP, Inntektskategori.ARBEIDSTAKER);
        beregningsresultatRepository.lagre(nyBehSVPOverlapper, berResMedOverlapp);
        var fagsakNy = nyBehSVPOverlapper.getFagsak();

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(fagsakNy.getId(), nyBehSVPOverlapper.getId());

        verify(revurderingTjenesteMockFP, times(1)).opprettAutomatiskRevurdering(eq(avsluttetFPSakMor), eq(BehandlingÅrsakType.OPPHØR_YTELSE_NYTT_BARN), any());
    }

    @Test
    public void loggOverlappFPMedGraderingNårInnvilgerSVP() {
        //Har FP som overlapper med ny SVP sak og det er ikke gradering
        var avsluttetFPBehMor = lagBehandlingMor(FØDSELS_DATO_1, AKTØR_ID_MOR, null);
        var berResMorBeh1 = lagBeregningsresultat(FØDSELS_DATO_1, FØDSELS_DATO_1.plusWeeks(10), Inntektskategori.ARBEIDSTAKER);
        leggTilBeregningsresPeriode(berResMorBeh1, FØDSELS_DATO_1.plusWeeks(11), FØDSELS_DATO_1.plusWeeks(21), true);
        beregningsresultatRepository.lagre(avsluttetFPBehMor, berResMorBeh1);

        var nyBehSVPOverlapper = lagBehandlingSVP(AKTØR_ID_MOR);
        var berResMedOverlapp = lagBeregningsresultat(FØDSELS_DATO_1.plusWeeks(18), FØDSELS_DATO_1.plusWeeks(25), Inntektskategori.ARBEIDSTAKER);
        beregningsresultatRepository.lagre(nyBehSVPOverlapper, berResMedOverlapp);
        var fagsakNy = nyBehSVPOverlapper.getFagsak();

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(fagsakNy.getId(), nyBehSVPOverlapper.getId());

        verify(revurderingTjenesteMockFP, times(0)).opprettAutomatiskRevurdering(any(), any(), any());
    }

    @Test
    public void opphørOverlappFPMedGraderingIPeriodenNårInnvilgerSVP() {
        var avsluttetFPBehMor = lagBehandlingMor(FØDSELS_DATO_1, AKTØR_ID_MOR, null);
        var berResMorBeh1 = lagBeregningsresultat(FØDSELS_DATO_1, SISTE_DAG_MOR, Inntektskategori.ARBEIDSTAKER);
        leggTilBeregningsresPeriode(berResMorBeh1, FØDSELS_DATO_1.minusWeeks(6), FØDSELS_DATO_1.minusWeeks(5), true);
        beregningsresultatRepository.lagre(avsluttetFPBehMor, berResMorBeh1);
        var avsluttetFPSakMor = avsluttetFPBehMor.getFagsak();

        var nyBehSVPOverlapper = lagBehandlingSVP(AKTØR_ID_MOR);
        var berResMedOverlapp = lagBeregningsresultat(SKJÆRINGSTIDSPUNKT_OVERLAPPER_BEH_1, SISTE_DAG_PER_OVERLAPP, Inntektskategori.ARBEIDSTAKER);
        leggTilBRAndel(berResMedOverlapp.getBeregningsresultatPerioder().stream().findFirst().orElse(null));
        beregningsresultatRepository.lagre(nyBehSVPOverlapper, berResMedOverlapp);
        var fagsakNy = nyBehSVPOverlapper.getFagsak();

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(fagsakNy.getId(), nyBehSVPOverlapper.getId());

        verify(revurderingTjenesteMockFP, times(1)).opprettAutomatiskRevurdering(eq(avsluttetFPSakMor), eq(BehandlingÅrsakType.OPPHØR_YTELSE_NYTT_BARN), any());
    }

    @Test
    public void loggOverlappSVPNårInnvilgerSVP() {
        var avslSVPBeh = lagBehandlingSVP(AKTØR_ID_MOR);
        var berResMorBeh1 = lagBeregningsresultat(FØDSELS_DATO_1, SISTE_DAG_MOR, Inntektskategori.ARBEIDSTAKER);
        beregningsresultatRepository.lagre(avslSVPBeh, berResMorBeh1);

        var nyBehSVPOverlapper = lagBehandlingSVP(AKTØR_ID_MOR);
        var berResMedOverlapp = lagBeregningsresultat(SKJÆRINGSTIDSPUNKT_OVERLAPPER_BEH_1, SISTE_DAG_PER_OVERLAPP, Inntektskategori.ARBEIDSTAKER);
        beregningsresultatRepository.lagre(nyBehSVPOverlapper, berResMedOverlapp);
        var fagsakNy = nyBehSVPOverlapper.getFagsak();

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(fagsakNy.getId(), nyBehSVPOverlapper.getId());

        verify(revurderingTjenesteMockSVP, times(0)).opprettAutomatiskRevurdering(any(), any(), any());
    }

    @Test
    public void ikkeVurderOverlappVedAdospjonOgSammeBarn() {
        var omsorgsovertakelsedato = LocalDate.of(2020, 1, 1);
        var adopsjonFarLop = lagBehandlingFPAdopsjonFar(null, omsorgsovertakelsedato);
        var berResadopsjon = lagBeregningsresultat(FØDSELS_DATO_1, SISTE_DAG_MOR, Inntektskategori.ARBEIDSTAKER);
        beregningsresultatRepository.lagre(adopsjonFarLop, berResadopsjon);

        var morAdopsjonSammeBarnIVB = lagBehandlingFPAdopsjonMor(adopsjonFarLop.getAktørId(), omsorgsovertakelsedato);
        var berResMedOverlapp = lagBeregningsresultat(SKJÆRINGSTIDSPUNKT_OVERLAPPER_BEH_1, SISTE_DAG_PER_OVERLAPP, Inntektskategori.ARBEIDSTAKER);
        beregningsresultatRepository.lagre(morAdopsjonSammeBarnIVB, berResMedOverlapp);
        var fagsakNy = morAdopsjonSammeBarnIVB.getFagsak();

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(fagsakNy.getId(), morAdopsjonSammeBarnIVB.getId());

        verify(revurderingTjenesteMockFP, times(0)).opprettAutomatiskRevurdering(any(), any(), any());
    }

    @Test
    public void vurderOverlappVedAdospjonForskjelligeBarn() {
        var omsorgsovertakelsedato = LocalDate.of(2019, 1, 1);
        var adopsjonFarLop = lagBehandlingFPAdopsjonFar(null, omsorgsovertakelsedato);
        var berResadopsjon = lagBeregningsresultat(FØDSELS_DATO_1, SISTE_DAG_MOR, Inntektskategori.ARBEIDSTAKER);
        beregningsresultatRepository.lagre(adopsjonFarLop, berResadopsjon);
        var adopsjonFarLopFS = adopsjonFarLop.getFagsak();

        var omsorgsovertakelsedato2 = LocalDate.of(2020, 1, 1);
        var morAdopsjonIVB = lagBehandlingFPAdopsjonMor(adopsjonFarLop.getAktørId(), omsorgsovertakelsedato2);
        var berResMedOverlapp = lagBeregningsresultat(SKJÆRINGSTIDSPUNKT_OVERLAPPER_BEH_1, SISTE_DAG_PER_OVERLAPP, Inntektskategori.ARBEIDSTAKER);
        beregningsresultatRepository.lagre(morAdopsjonIVB, berResMedOverlapp);
        var fagsakNy = morAdopsjonIVB.getFagsak();

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(fagsakNy.getId(), morAdopsjonIVB.getId());

        verify(revurderingTjenesteMockFP, times(1)).opprettAutomatiskRevurdering(eq(adopsjonFarLopFS), eq(BehandlingÅrsakType.OPPHØR_YTELSE_NYTT_BARN), any());
    }

    private Behandling lagBehandlingMor( LocalDate fødselsDato, AktørId aktørId, AktørId medfAktørId)
    {
        var scenarioAvsluttetBehMor = ScenarioMorSøkerForeldrepenger.forFødselMedGittAktørId(aktørId);
        scenarioAvsluttetBehMor.medSøknadHendelse().medFødselsDato(fødselsDato);
        if (medfAktørId!= null) {
            scenarioAvsluttetBehMor.medSøknadAnnenPart().medAktørId(medfAktørId).medNavn("Seig Pinne").medType(SøknadAnnenPartType.FAR);
        }
        scenarioAvsluttetBehMor.medBehandlingsresultat(Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET));
        scenarioAvsluttetBehMor.medVilkårResultatType(VilkårResultatType.INNVILGET);
        scenarioAvsluttetBehMor.medBehandlingVedtak().medVedtakstidspunkt(LocalDateTime.now().minusMonths(2))
            .medVedtakResultatType(VedtakResultatType.INNVILGET);
        var behandling = scenarioAvsluttetBehMor.lagre(repositoryProvider);
        avsluttBehandlingOgFagsak(behandling);
        return behandling;
    }

    private Behandling lagBehandlingFar( LocalDate fødselsDato, AktørId aktørId, AktørId medfAktørId)
    {
        var scenarioAvsluttetBehFar = ScenarioFarSøkerForeldrepenger.forFødselMedGittAktørId(aktørId);
        scenarioAvsluttetBehFar.medSøknadHendelse().medFødselsDato(fødselsDato);
        if (medfAktørId!= null) {
            scenarioAvsluttetBehFar.medSøknadAnnenPart().medAktørId(medfAktørId).medNavn("Is Pinne").medType(SøknadAnnenPartType.MOR);
        }
        scenarioAvsluttetBehFar.medBehandlingsresultat(Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET));
        scenarioAvsluttetBehFar.medVilkårResultatType(VilkårResultatType.INNVILGET);
        scenarioAvsluttetBehFar.medBehandlingVedtak().medVedtakstidspunkt(LocalDateTime.now().minusMonths(2))
            .medVedtakResultatType(VedtakResultatType.INNVILGET);
        var behandling = scenarioAvsluttetBehFar.lagre(repositoryProvider);
        avsluttBehandlingOgFagsak(behandling);
        return behandling;
    }

    private Behandling lagBehandlingFPAdopsjonMor(AktørId medfAktørId, LocalDate omsorgsovertakelsedato) {
        var scenario = ScenarioMorSøkerForeldrepenger.forAdopsjon();
        scenario.medSøknadHendelse().medAdopsjon(scenario.medSøknadHendelse().getAdopsjonBuilder().medOmsorgsovertakelseDato(omsorgsovertakelsedato));
        if (medfAktørId!= null) {
            scenario.medSøknadAnnenPart().medAktørId(medfAktørId).medNavn("Seig Pinne").medType(SøknadAnnenPartType.FAR);
        }
        scenario.medBehandlingsresultat(Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET));
        scenario.medVilkårResultatType(VilkårResultatType.INNVILGET);
        scenario.medBehandlingVedtak().medVedtakstidspunkt(LocalDateTime.now().minusMonths(2))
            .medVedtakResultatType(VedtakResultatType.INNVILGET);
        var behandling = scenario.lagre(repositoryProvider);
        avsluttBehandlingOgFagsak(behandling);

        return behandling;
    }

    private Behandling lagBehandlingFPAdopsjonFar(AktørId medfAktørId, LocalDate omsorgsovertakelsedato) {
        var scenario = ScenarioFarSøkerForeldrepenger.forAdopsjon();
        scenario.medSøknadHendelse().medAdopsjon(scenario.medSøknadHendelse().getAdopsjonBuilder().medOmsorgsovertakelseDato(omsorgsovertakelsedato));
        if (medfAktørId!= null) {
            scenario.medSøknadAnnenPart().medAktørId(medfAktørId).medNavn("Seig Pinne").medType(SøknadAnnenPartType.FAR);
        }
        scenario.medBehandlingsresultat(Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET));
        scenario.medVilkårResultatType(VilkårResultatType.INNVILGET);
        scenario.medBehandlingVedtak().medVedtakstidspunkt(LocalDateTime.now().minusMonths(2))
            .medVedtakResultatType(VedtakResultatType.INNVILGET);
        var behandling = scenario.lagre(repositoryProvider);
        avsluttBehandlingOgFagsak(behandling);

        return behandling;
    }

    private Behandling lagBehandlingSVP( AktørId aktørId) {
        var scenarioAvslBeh = ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger();
        scenarioAvslBeh.medBruker(aktørId, NavBrukerKjønn.KVINNE);
        scenarioAvslBeh.medDefaultOppgittTilknytning();

        scenarioAvslBeh.medBehandlingsresultat(Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET));
        scenarioAvslBeh.medVilkårResultatType(VilkårResultatType.INNVILGET);
        scenarioAvslBeh.medBehandlingVedtak().medVedtakstidspunkt(LocalDateTime.now().minusMonths(1))
            .medVedtakResultatType(VedtakResultatType.INNVILGET);
        var behandlingSVP = scenarioAvslBeh.lagre(repositoryProvider);
        avsluttBehandlingOgFagsak(behandlingSVP);
        return behandlingSVP;
    }


    private BeregningsresultatEntitet lagBeregningsresultat(LocalDate periodeFom, LocalDate periodeTom, Inntektskategori inntektskategori) {
        var beregningsresultat = BeregningsresultatEntitet.builder().medRegelInput("input").medRegelSporing("sporing").build();
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

    private void leggTilBeregningsresPeriode(BeregningsresultatEntitet beregningsresultatEntitet, LocalDate fom, LocalDate tom, boolean gradering) {
        var beregningsresultatPeriode = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(fom, tom)
            .build(beregningsresultatEntitet);

        if (gradering) {
            BeregningsresultatAndel.builder()
                .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
                .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
                .medDagsats(gradering ? DAGSATS_GRADERING : DAGSATS)
                .medDagsatsFraBg(DAGSATS)
                .medBrukerErMottaker(true)
                .medUtbetalingsgrad(gradering ? BigDecimal.valueOf(50) : BigDecimal.valueOf(100))
                .medStillingsprosent(BigDecimal.valueOf(50))
                .build(beregningsresultatPeriode);
        }
        beregningsresultatEntitet.addBeregningsresultatPeriode(beregningsresultatPeriode);
    }

    private void leggTilBRAndel(BeregningsresultatPeriode beregningsresultatPeriode) {
        BeregningsresultatAndel.builder()
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medDagsats(DAGSATS+50)
            .medDagsatsFraBg(DAGSATS+50)
            .medBrukerErMottaker(true)
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medStillingsprosent(BigDecimal.valueOf(50))
            .build(beregningsresultatPeriode);
    }

    private void avsluttBehandlingOgFagsak(Behandling behandling) {
        behandling.avsluttBehandling();
        repositoryProvider.getBehandlingRepository().lagre(behandling, repositoryProvider.getBehandlingRepository().taSkriveLås(behandling));
        fagsakRepository.oppdaterFagsakStatus(behandling.getFagsakId(), FagsakStatus.LØPENDE);
    }
    private void lagRevurdering(Behandling originalBehandling) {
        var revurdering = Behandling.fraTidligereBehandling(originalBehandling, BehandlingType.REVURDERING)
            .medBehandlingÅrsak(
                BehandlingÅrsak.builder(BehandlingÅrsakType.OPPHØR_YTELSE_NYTT_BARN)
                    .medOriginalBehandlingId(originalBehandling.getId()))
            .build();
        repositoryProvider.getBehandlingRepository().lagre(revurdering, repositoryProvider.getBehandlingRepository().taSkriveLås(revurdering));
    }

}
