package no.nav.foreldrepenger.mottak.vedtak.overlapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
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
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerSvangerskapspenger;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.tid.VirkedagUtil;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.task.OpprettOppgaveVurderKonsekvensTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.felles.prosesstask.api.TaskType;

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
    @Mock
    private ProsessTaskTjeneste taskTjeneste;
    private final BehandlendeEnhetTjeneste behandlendeEnhetTjeneste = Mockito.mock(BehandlendeEnhetTjeneste.class);

    @Mock
    private RevurderingTjeneste revurderingTjenesteMockFP;
    @Mock
    private RevurderingTjeneste revurderingTjenesteMockSVP;

    @BeforeEach
    public void setUp() {
        var entityManager = getEntityManager();
        lenient().when(behandlendeEnhetTjeneste.gyldigEnhetNfpNk(any())).thenReturn(true);
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        beregningsresultatRepository = new BeregningsresultatRepository(entityManager);
        fagsakRepository = new FagsakRepository(entityManager);
        vurderOpphørAvYtelser = new VurderOpphørAvYtelser(repositoryProvider, revurderingTjenesteMockFP, revurderingTjenesteMockSVP,
            taskTjeneste, behandlendeEnhetTjeneste, sjekkInfotrygdTjeneste, entityManager);
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

        verifiserAtProsesstaskForHåndteringAvOpphørErOpprettet(fsavsluttetBehMor);
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

        verifiserAtProsesstaskForHåndteringAvOpphørErOpprettet(avsluttetBehMor.getFagsak(), 2);
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
        verifiserAtProsesstaskForHåndteringAvOpphørErOpprettet(fsavsluttetBehFar);
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
        verifiserAtProsesstaskForHåndteringAvOpphørIkkeErOpprettet();
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
        verifiserAtProsesstaskForHåndteringAvOpphørErOpprettet(fsavsluttetBehFar);
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
        verifiserAtProsesstaskForHåndteringAvOpphørErOpprettet(fsavsluttetBehFar);
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
        verifiserAtProsesstaskForHåndteringAvOpphørErOpprettet(fsavsluttetBehMor);
        verify(sjekkInfotrygdTjeneste, times(0)).harForeldrepengerInfotrygdSomOverlapper(any(),any() );
    }

    @Test
    public void oppretteTaskVurderKonsekvensIngenGjeldendeAktørId() {
        var KEY_GJELDENDE_AKTØR_ID="aktuellAktoerId";

        var nyBehMorSomIkkeOverlapper = lagBehandlingMor(SKJÆRINGSTIDSPUNKT_OVERLAPPER_IKKE_BEH_1, AKTØR_ID_MOR, null);
        var fagsakNy = nyBehMorSomIkkeOverlapper.getFagsak();

        vurderOpphørAvYtelser.opprettTaskForÅVurdereKonsekvens(fagsakNy.getId(),"Test", "Test", Optional.empty() );

        var captor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(taskTjeneste).lagre(captor.capture());
        var gjeldendeAktørId = captor.getAllValues().stream()
            .filter(p -> p.taskType().equals(TaskType.forProsessTask(OpprettOppgaveVurderKonsekvensTask.class)))
            .map(p -> p.getPropertyValue(KEY_GJELDENDE_AKTØR_ID))
            .filter(Objects::nonNull)
            .findFirst();

        assertThat(gjeldendeAktørId).isEmpty();
    }

    @Test
    public void oppretteTaskVurderKonsekvensMedAktørId() {
        var nyBehMorSomIkkeOverlapper = lagBehandlingMor(SKJÆRINGSTIDSPUNKT_OVERLAPPER_IKKE_BEH_1, AKTØR_ID_MOR, null);
        var fagsakNy = nyBehMorSomIkkeOverlapper.getFagsak();

        vurderOpphørAvYtelser.opprettTaskForÅVurdereKonsekvens(fagsakNy.getId(), "Test2",
            "Test2", Optional.of(nyBehMorSomIkkeOverlapper.getAktørId().getId()));
        var captor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(taskTjeneste).lagre(captor.capture());
        var vurderKonsekvensTaskForFagsak = captor.getAllValues().stream()
            .filter(p -> p.taskType().equals(TaskType.forProsessTask(OpprettOppgaveVurderKonsekvensTask.class)))
            .filter(p -> Objects.equals(p.getFagsakId(), nyBehMorSomIkkeOverlapper.getFagsakId()))
            .collect(Collectors.toList());
        assertThat(vurderKonsekvensTaskForFagsak).hasSize(1);
    }

    @Test
    public void opprettHåndteringNårOverlappMedFPNårInnvSVPPåSammeBarn() {
        var avsluttetFPBehMor = lagBehandlingMor(FØDSELS_DATO_1, AKTØR_ID_MOR, null);
        var berResMorBeh1 = lagBeregningsresultat(FØDSELS_DATO_1, SISTE_DAG_MOR, Inntektskategori.ARBEIDSTAKER);
        beregningsresultatRepository.lagre(avsluttetFPBehMor, berResMorBeh1);

        var nyBehSVPOverlapper = lagBehandlingSVP(AKTØR_ID_MOR);
        var berResMedOverlapp = lagBeregningsresultat(FØDSELS_DATO_1.minusWeeks(3), FØDSELS_DATO_1.plusDays(4), Inntektskategori.ARBEIDSTAKER);
        beregningsresultatRepository.lagre(nyBehSVPOverlapper, berResMedOverlapp);
        var fagsakNy = nyBehSVPOverlapper.getFagsak();

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(fagsakNy.getId(), nyBehSVPOverlapper.getId());

        verifiserAtProsesstaskForHåndteringAvOpphørErOpprettet(fagsakNy);
    }

    @Test
    public void opprettHåndteringNårOverlappMedFPNårInnvilgerSVPPåNyttBarn() {
        var avsluttetFPBehMor = lagBehandlingMor(FØDSELS_DATO_1, AKTØR_ID_MOR, null);
        var berResMorBeh1 = lagBeregningsresultat(FØDSELS_DATO_1, SISTE_DAG_MOR, Inntektskategori.ARBEIDSTAKER);
        beregningsresultatRepository.lagre(avsluttetFPBehMor, berResMorBeh1);
        var avsluttetFPSakMor = avsluttetFPBehMor.getFagsak();

        var nyBehSVPOverlapper = lagBehandlingSVP(AKTØR_ID_MOR);
        var berResMedOverlapp = lagBeregningsresultat(SKJÆRINGSTIDSPUNKT_OVERLAPPER_BEH_1, SISTE_DAG_PER_OVERLAPP, Inntektskategori.ARBEIDSTAKER);
        beregningsresultatRepository.lagre(nyBehSVPOverlapper, berResMedOverlapp);
        var fagsakNy = nyBehSVPOverlapper.getFagsak();

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(fagsakNy.getId(), nyBehSVPOverlapper.getId());

        verifiserAtProsesstaskForHåndteringAvOpphørErOpprettet(avsluttetFPSakMor);
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

        verifiserAtProsesstaskForHåndteringAvOpphørIkkeErOpprettet();
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

        verifiserAtProsesstaskForHåndteringAvOpphørErOpprettet(avsluttetFPSakMor);
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

        verifiserAtProsesstaskForHåndteringAvOpphørIkkeErOpprettet();
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

        verifiserAtProsesstaskForHåndteringAvOpphørIkkeErOpprettet();
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

        verifiserAtProsesstaskForHåndteringAvOpphørErOpprettet(adopsjonFarLopFS);
    }

    @Test
    public void opprettIkkeRevNårOverlappMedFPNårInnvilgerSVPPåNyttBarn() {
        var fud = LocalDate.of(2021,6,4);
        var avsluttetFPBehMor = lagBehandlingMor(fud.plusWeeks(3), AKTØR_ID_MOR, null);
        var berResMorBeh1 = lagBeregningsresultat(fud, fud.plusMonths(3), Inntektskategori.ARBEIDSTAKER);
        beregningsresultatRepository.lagre(avsluttetFPBehMor, berResMorBeh1);

        var nyBehSVPOverlapper = lagBehandlingSVP(AKTØR_ID_MOR);
        var berResMedOverlapp = lagBeregningsresultat(LocalDate.of(2021,5,3), fud.minusDays(1), Inntektskategori.ARBEIDSTAKER);
        leggTilBeregningsresPeriodeBeløp(berResMedOverlapp, fud, LocalDate.of(2021,8,1), 0);
        beregningsresultatRepository.lagre(nyBehSVPOverlapper, berResMedOverlapp);
        var fagsakNy = nyBehSVPOverlapper.getFagsak();

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(fagsakNy.getId(), nyBehSVPOverlapper.getId());

        verifiserAtProsesstaskForHåndteringAvOpphørIkkeErOpprettet();
    }

    private void leggTilBeregningsresPeriodeBeløp(BeregningsresultatEntitet beregningsresultatEntitet, LocalDate fom, LocalDate tom, int dagsats) {
        var beregningsresultatPeriode = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(fom, tom)
            .build(beregningsresultatEntitet);

        BeregningsresultatAndel.builder()
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medDagsats(dagsats)
            .medDagsatsFraBg(dagsats)
            .medBrukerErMottaker(true)
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medStillingsprosent(BigDecimal.valueOf(50))
            .build(beregningsresultatPeriode);
        beregningsresultatEntitet.addBeregningsresultatPeriode(beregningsresultatPeriode);
    }


    private Behandling lagBehandlingMor( LocalDate fødselsDato, AktørId aktørId, AktørId medfAktørId) {
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

    private Behandling lagBehandlingFar( LocalDate fødselsDato, AktørId aktørId, AktørId medfAktørId) {
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

    private void verifiserAtProsesstaskForHåndteringAvOpphørErOpprettet(Fagsak fagsak) {
        verifiserAtProsesstaskForHåndteringAvOpphørErOpprettet(fagsak, 1);
    }

    private void verifiserAtProsesstaskForHåndteringAvOpphørErOpprettet(Fagsak fagsak, int times) {
        var captor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(taskTjeneste, times(times)).lagre(captor.capture());
        var håndterOpphør = captor.getAllValues().stream().filter(t -> t.taskType().equals(TaskType.forProsessTask(HåndterOpphørAvYtelserTask.class))).findFirst().orElse(null);
        assertThat(håndterOpphør.getFagsakId()).isEqualTo(fagsak.getId());
        assertThat(håndterOpphør.getPropertyValue(HåndterOpphørAvYtelserTask.BESKRIVELSE_KEY)).isNotNull();
        assertThat(håndterOpphør.getPropertyValue(HåndterOpphørAvYtelserTask.BEHANDLING_ÅRSAK_KEY)).isEqualTo(BehandlingÅrsakType.OPPHØR_YTELSE_NYTT_BARN.getKode());
    }

    private void verifiserAtProsesstaskForHåndteringAvOpphørIkkeErOpprettet() {
        verifyNoInteractions(taskTjeneste);
    }
}
