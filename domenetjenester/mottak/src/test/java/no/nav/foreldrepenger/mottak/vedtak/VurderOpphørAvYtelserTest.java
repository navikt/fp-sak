package no.nav.foreldrepenger.mottak.vedtak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import javax.persistence.EntityManager;

import no.nav.foreldrepenger.mottak.vedtak.overlapp.SjekkOverlappForeldrepengerInfotrygdTjeneste;
import no.nav.foreldrepenger.mottak.vedtak.overlapp.VurderOpphørAvYtelser;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
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
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerSvangerskapspenger;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.tid.VirkedagUtil;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.mottak.dokumentmottak.impl.KøKontroller;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.felles.prosesstask.impl.ProsessTaskEventPubliserer;
import no.nav.vedtak.felles.prosesstask.impl.ProsessTaskRepositoryImpl;

public class VurderOpphørAvYtelserTest {

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
    private SjekkOverlappForeldrepengerInfotrygdTjeneste sjekkInfotrygdTjeneste = mock(SjekkOverlappForeldrepengerInfotrygdTjeneste.class);


    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private final EntityManager entityManager = repoRule.getEntityManager();
    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(entityManager);
    private FagsakRepository fagsakRepository = repositoryProvider.getFagsakRepository();
    private BeregningsresultatRepository beregningsresultatRepository = repositoryProvider.getBeregningsresultatRepository();
    private ProsessTaskEventPubliserer eventPubliserer = Mockito.mock(ProsessTaskEventPubliserer.class);
    private ProsessTaskRepository prosessTaskRepository = new ProsessTaskRepositoryImpl(entityManager, null, eventPubliserer);
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste = Mockito.mock(BehandlendeEnhetTjeneste.class);

    @Mock
    @FagsakYtelseTypeRef("FP")
    private RevurderingTjeneste revurderingTjenesteMockFP;
    @Mock
    @FagsakYtelseTypeRef("SVP")
    private RevurderingTjeneste revurderingTjenesteMockSVP;
    @Mock
    private BehandlingProsesseringTjeneste behandlingProsesseringTjenesteMock;



    @Before
    public void setUp() {
        initMocks(this);
        vurderOpphørAvYtelser = new VurderOpphørAvYtelser(repositoryProvider, revurderingTjenesteMockFP, revurderingTjenesteMockSVP,
            prosessTaskRepository, behandlendeEnhetTjeneste, behandlingProsesseringTjenesteMock, sjekkInfotrygdTjeneste, mock(KøKontroller.class) );
    }

    @Test
    public void opphørLøpendeSakNårNySakOverlapperPåMor() {
        Behandling avsluttetBehMor = lagBehandlingMor(FØDSELS_DATO_1, AKTØR_ID_MOR, null);
        BeregningsresultatEntitet berResMorBeh1 = lagBeregningsresultat(FØDSELS_DATO_1, SISTE_DAG_MOR, Inntektskategori.ARBEIDSTAKER);
        beregningsresultatRepository.lagre(avsluttetBehMor, berResMorBeh1);
        Fagsak fsavsluttetBehMor = avsluttetBehMor.getFagsak();

        Behandling nyAvsBehandlingMor = lagBehandlingMor(SKJÆRINGSTIDSPUNKT_OVERLAPPER_BEH_1, AKTØR_ID_MOR, null);
        BeregningsresultatEntitet berResMorOverlapp = lagBeregningsresultat(SKJÆRINGSTIDSPUNKT_OVERLAPPER_BEH_1, SISTE_DAG_PER_OVERLAPP, Inntektskategori.ARBEIDSTAKER);
        beregningsresultatRepository.lagre(nyAvsBehandlingMor, berResMorOverlapp);
        Fagsak fagsakNy = nyAvsBehandlingMor.getFagsak();

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(fagsakNy.getId(), nyAvsBehandlingMor.getId());

        verify(revurderingTjenesteMockFP, times(1)).opprettAutomatiskRevurdering(eq(fsavsluttetBehMor), eq(BehandlingÅrsakType.OPPHØR_YTELSE_NYTT_BARN), any());
        verify(sjekkInfotrygdTjeneste, times(0)).harForeldrepengerInfotrygdSomOverlapper(any(), any());
    }

    @Test
    public void opphørSakPåMorOgMedforelderNårNySakOverlapper() {
        Behandling avsluttetBehMor = lagBehandlingMor(FØDSELS_DATO_1, AKTØR_ID_MOR, MEDF_AKTØR_ID);
        BeregningsresultatEntitet berResMorBeh1 = lagBeregningsresultat(FØDSELS_DATO_1, SISTE_DAG_MOR, Inntektskategori.ARBEIDSTAKER);
        beregningsresultatRepository.lagre(avsluttetBehMor, berResMorBeh1);
        Fagsak fsavsluttetBehMor = avsluttetBehMor.getFagsak();

        Behandling nyAvsBehandlingMor = lagBehandlingMor(SKJÆRINGSTIDSPUNKT_OVERLAPPER_BEH_1, AKTØR_ID_MOR, MEDF_AKTØR_ID);
        BeregningsresultatEntitet berResMorOverlapp = lagBeregningsresultat(SKJÆRINGSTIDSPUNKT_OVERLAPPER_BEH_1, SISTE_DAG_PER_OVERLAPP, Inntektskategori.ARBEIDSTAKER);
        beregningsresultatRepository.lagre(nyAvsBehandlingMor, berResMorOverlapp);
        Fagsak fagsakNy = nyAvsBehandlingMor.getFagsak();

        Behandling avslBehFarMedOverlappMor = lagBehandlingFar(FØDSELS_DATO_1, MEDF_AKTØR_ID, AKTØR_ID_MOR);
        BeregningsresultatEntitet berResFar = lagBeregningsresultat(SKJÆRINGSTIDSPUNKT_OVERLAPPER_BEH_1, SISTE_DAG_PER_OVERLAPP, Inntektskategori.ARBEIDSTAKER);
        beregningsresultatRepository.lagre(avslBehFarMedOverlappMor, berResFar);
        Fagsak fsavsluttetBehFar = avslBehFarMedOverlappMor.getFagsak();

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(fagsakNy.getId(),nyAvsBehandlingMor.getId());

        verify(revurderingTjenesteMockFP, times(1)).opprettAutomatiskRevurdering(eq(fsavsluttetBehFar), eq(BehandlingÅrsakType.OPPHØR_YTELSE_NYTT_BARN), any());
        verify(sjekkInfotrygdTjeneste, times(1)).harForeldrepengerInfotrygdSomOverlapper(fsavsluttetBehFar.getAktørId(),SKJÆRINGSTIDSPUNKT_OVERLAPPER_BEH_1 );
    }

    @Test
    public void adopsjonFarFørstSkalIkkeOpphøresAvMor() {

        Behandling avsluttetBehFar = lagBehandlingFar(FØDSELS_DATO_1, MEDF_AKTØR_ID, AKTØR_ID_MOR);
        BeregningsresultatEntitet berResFarBeh1 = lagBeregningsresultat(FØDSELS_DATO_1, SISTE_DAG_MOR, Inntektskategori.ARBEIDSTAKER);
        beregningsresultatRepository.lagre(avsluttetBehFar, berResFarBeh1);
        Fagsak fsavsluttetBehFar = avsluttetBehFar.getFagsak();

        Behandling avsluttetBehMor = lagBehandlingMor(FØDSELS_DATO_1, AKTØR_ID_MOR, MEDF_AKTØR_ID);
        BeregningsresultatEntitet berResMorBeh1 = lagBeregningsresultat(SKJÆRINGSTIDSPUNKT_OVERLAPPER_BEH_1, SISTE_DAG_PER_OVERLAPP, Inntektskategori.ARBEIDSTAKER);
        beregningsresultatRepository.lagre(avsluttetBehMor, berResMorBeh1);
        Fagsak fsavsluttetBehMor = avsluttetBehMor.getFagsak();

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(fsavsluttetBehMor.getId(),avsluttetBehMor.getId());

        // OBS: Dette vil vi helst ikke. Trenger sjekk på om sak gjelder samme barn
        verify(revurderingTjenesteMockFP, times(1)).opprettAutomatiskRevurdering(eq(fsavsluttetBehFar), eq(BehandlingÅrsakType.OPPHØR_YTELSE_NYTT_BARN), any());
    }

    @Test
    public void ikkeOpphørSakNårNySakIkkeOverlapper() {
        Behandling avsluttetBehMor = lagBehandlingMor(FØDSELS_DATO_1, AKTØR_ID_MOR,null);
        BeregningsresultatEntitet berResMorBeh1 = lagBeregningsresultat(FØDSELS_DATO_1, SISTE_DAG_MOR, Inntektskategori.ARBEIDSTAKER);
        leggTilBeregningsresPeriode(berResMorBeh1, SISTE_DAG_MOR, SISTE_DAG_MOR.plusWeeks(2), false);
        beregningsresultatRepository.lagre(avsluttetBehMor, berResMorBeh1);

        Behandling nyBehMorSomIkkeOverlapper = lagBehandlingMor(SKJÆRINGSTIDSPUNKT_OVERLAPPER_IKKE_BEH_1, AKTØR_ID_MOR, null);
        BeregningsresultatEntitet berResMorBeh2 = lagBeregningsresultat(SKJÆRINGSTIDSPUNKT_OVERLAPPER_IKKE_BEH_1, SKJÆRINGSTIDSPUNKT_OVERLAPPER_IKKE_BEH_1, Inntektskategori.ARBEIDSTAKER);
        leggTilBeregningsresPeriode(berResMorBeh2, SISTE_DAG_IKKE_OVERLAPP, SISTE_DAG_IKKE_OVERLAPP.plusWeeks(2), false);
        beregningsresultatRepository.lagre(nyBehMorSomIkkeOverlapper, berResMorBeh2);
        Fagsak fagsakNy = nyBehMorSomIkkeOverlapper.getFagsak();

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(fagsakNy.getId(), nyBehMorSomIkkeOverlapper.getId());
        verify(revurderingTjenesteMockFP, times(0)).opprettAutomatiskRevurdering(any(), any(), any());
        verify(sjekkInfotrygdTjeneste, times(0)).harForeldrepengerInfotrygdSomOverlapper(any(),any() );
    }

    @Test
    public void opphørSakPåMedforelderMenIkkeMor() {
        Behandling avsluttetBehMor = lagBehandlingMor(FØDSELS_DATO_1, AKTØR_ID_MOR,MEDF_AKTØR_ID);
        BeregningsresultatEntitet berResMorBeh1 = lagBeregningsresultat(FØDSELS_DATO_1, SISTE_DAG_MOR, Inntektskategori.ARBEIDSTAKER);
        leggTilBeregningsresPeriode(berResMorBeh1, SISTE_DAG_MOR, SISTE_DAG_MOR.plusWeeks(2), false);
        beregningsresultatRepository.lagre(avsluttetBehMor, berResMorBeh1);

        Behandling nyBehMorSomIkkeOverlapper = lagBehandlingMor(SKJÆRINGSTIDSPUNKT_OVERLAPPER_IKKE_BEH_1, AKTØR_ID_MOR, MEDF_AKTØR_ID);
        BeregningsresultatEntitet berResMorBeh2 = lagBeregningsresultat(SKJÆRINGSTIDSPUNKT_OVERLAPPER_IKKE_BEH_1, SKJÆRINGSTIDSPUNKT_OVERLAPPER_IKKE_BEH_1, Inntektskategori.ARBEIDSTAKER);
        leggTilBeregningsresPeriode(berResMorBeh2, SISTE_DAG_IKKE_OVERLAPP, SISTE_DAG_IKKE_OVERLAPP.plusWeeks(2), false);
        beregningsresultatRepository.lagre(nyBehMorSomIkkeOverlapper, berResMorBeh2);
        Fagsak fagsakNy = nyBehMorSomIkkeOverlapper.getFagsak();

        Behandling avslBehFarMedOverlappMor = lagBehandlingFar(SKJÆRINGSTIDSPUNKT_OVERLAPPER_IKKE_BEH_1, MEDF_AKTØR_ID, AKTØR_ID_MOR);
        BeregningsresultatEntitet berResFar = lagBeregningsresultat(SKJÆRINGSTIDSPUNKT_OVERLAPPER_BEH_1.plusMonths(2), SISTE_DAG_PER_OVERLAPP.plusMonths(2), Inntektskategori.ARBEIDSTAKER);
        beregningsresultatRepository.lagre(avslBehFarMedOverlappMor, berResFar);
        Fagsak fsavsluttetBehFar = avslBehFarMedOverlappMor.getFagsak();

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(fagsakNy.getId(), nyBehMorSomIkkeOverlapper.getId());
        verify(revurderingTjenesteMockFP, times(1)).opprettAutomatiskRevurdering(eq(fsavsluttetBehFar), eq(BehandlingÅrsakType.OPPHØR_YTELSE_NYTT_BARN), any());
        verify(sjekkInfotrygdTjeneste, times(1)).harForeldrepengerInfotrygdSomOverlapper(fsavsluttetBehFar.getAktørId(),SKJÆRINGSTIDSPUNKT_OVERLAPPER_IKKE_BEH_1 );

    }

    @Test
    public void opphørSakPåFarNårNySakPåFarOverlapper() {
        Behandling avslBehFar = lagBehandlingFar(FØDSELS_DATO_1, MEDF_AKTØR_ID, AKTØR_ID_MOR);
        BeregningsresultatEntitet berResFarBeh1 = lagBeregningsresultat(FØDSELS_DATO_1, SISTE_DAG_MOR, Inntektskategori.ARBEIDSTAKER);
        leggTilBeregningsresPeriode(berResFarBeh1, SISTE_DAG_MOR, SISTE_DAG_MOR.plusWeeks(2), false);
        beregningsresultatRepository.lagre(avslBehFar, berResFarBeh1);

        Fagsak fsavsluttetBehFar = avslBehFar.getFagsak();

        Behandling nyBehFar = lagBehandlingFar(SKJÆRINGSTIDSPUNKT_OVERLAPPER_BEH_1, MEDF_AKTØR_ID, AKTØR_ID_MOR);
        BeregningsresultatEntitet berResFar = lagBeregningsresultat(SKJÆRINGSTIDSPUNKT_OVERLAPPER_BEH_1, SISTE_DAG_PER_OVERLAPP, Inntektskategori.ARBEIDSTAKER);
        beregningsresultatRepository.lagre(nyBehFar, berResFar);
        Fagsak fagsakNy = nyBehFar.getFagsak();

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(fagsakNy.getId(), nyBehFar.getId());
        verify(revurderingTjenesteMockFP, times(1)).opprettAutomatiskRevurdering(eq(fsavsluttetBehFar), eq(BehandlingÅrsakType.OPPHØR_YTELSE_NYTT_BARN), any());
        verify(sjekkInfotrygdTjeneste, times(1)).harForeldrepengerInfotrygdSomOverlapper(fagsakNy.getAktørId(),SKJÆRINGSTIDSPUNKT_OVERLAPPER_BEH_1 );
    }

    @Test
    public void opphørSakPåMorNårSisteUttakLikStartPåNyttUttak() {
        Behandling avsluttetBehMor = lagBehandlingMor(FØDSELS_DATO_1, AKTØR_ID_MOR, null);
        BeregningsresultatEntitet berResMorBeh1 = lagBeregningsresultat(FØDSELS_DATO_1, SISTE_DAG_MOR, Inntektskategori.ARBEIDSTAKER);
        beregningsresultatRepository.lagre(avsluttetBehMor, berResMorBeh1);
        Fagsak fsavsluttetBehMor = avsluttetBehMor.getFagsak();

        Behandling nyAvsBehandlingMor = lagBehandlingMor(SISTE_DAG_MOR, AKTØR_ID_MOR, null);
        BeregningsresultatEntitet berResMorOverlapp = lagBeregningsresultat(SISTE_DAG_MOR, SISTE_DAG_MOR.plusWeeks(3), Inntektskategori.ARBEIDSTAKER);
        beregningsresultatRepository.lagre(nyAvsBehandlingMor, berResMorOverlapp);
        Fagsak fagsakNy = nyAvsBehandlingMor.getFagsak();

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(fagsakNy.getId(), nyAvsBehandlingMor.getId());
        verify(revurderingTjenesteMockFP, times(1)).opprettAutomatiskRevurdering(eq(fsavsluttetBehMor), eq(BehandlingÅrsakType.OPPHØR_YTELSE_NYTT_BARN), any());
        verify(sjekkInfotrygdTjeneste, times(0)).harForeldrepengerInfotrygdSomOverlapper(any(),any() );
    }

    @Test
    public void ikkeOpphørSakPåMorNårOpphørErOpprettetAllerede() {
        Behandling avsluttetBehMor = lagBehandlingMor(FØDSELS_DATO_1, AKTØR_ID_MOR, null);
        BeregningsresultatEntitet berResMorBeh1 = lagBeregningsresultat(FØDSELS_DATO_1, SISTE_DAG_MOR, Inntektskategori.ARBEIDSTAKER);
        beregningsresultatRepository.lagre(avsluttetBehMor, berResMorBeh1);
        Fagsak fsavsluttetBehMor = avsluttetBehMor.getFagsak();

        Behandling nyAvsBehandlingMor = lagBehandlingMor(SISTE_DAG_MOR, AKTØR_ID_MOR, null);
        BeregningsresultatEntitet berResMorOverlapp = lagBeregningsresultat(SISTE_DAG_MOR, SISTE_DAG_MOR.plusWeeks(3), Inntektskategori.ARBEIDSTAKER);
        beregningsresultatRepository.lagre(nyAvsBehandlingMor, berResMorOverlapp);
        Fagsak fagsakNy = nyAvsBehandlingMor.getFagsak();

        lagRevurdering(avsluttetBehMor);

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(fagsakNy.getId(), nyAvsBehandlingMor.getId());
        verify(revurderingTjenesteMockFP, times(0)).opprettAutomatiskRevurdering(any(), any(), any());
        verify(sjekkInfotrygdTjeneste, times(0)).harForeldrepengerInfotrygdSomOverlapper(any(),any() );
    }

    @Test
    public void oppretteTaskVurderKonsekvensIngenGjeldendeAktørId() {
        String KEY_GJELDENDE_AKTØR_ID="aktuellAktoerId";

        Behandling nyBehMorSomIkkeOverlapper = lagBehandlingMor(SKJÆRINGSTIDSPUNKT_OVERLAPPER_IKKE_BEH_1, AKTØR_ID_MOR, null);
        Fagsak fagsakNy = nyBehMorSomIkkeOverlapper.getFagsak();

        vurderOpphørAvYtelser.opprettTaskForÅVurdereKonsekvens(fagsakNy.getId(),"Test", "Test", Optional.empty() );
        ProsessTaskData vurderKonsekvens = prosessTaskRepository.finnIkkeStartet().stream().findFirst().orElse(null);
        Optional<String> gjeldendeAktørId = Optional.ofNullable(vurderKonsekvens.getPropertyValue(KEY_GJELDENDE_AKTØR_ID));

        assertThat(gjeldendeAktørId.isPresent()).isFalse();
    }

    @Test
    public void oppretteTaskVurderKonsekvensMedAktørId() {
        String KEY_GJELDENDE_AKTØR_ID="aktuellAktoerId";

        Behandling nyBehMorSomIkkeOverlapper = lagBehandlingMor(SKJÆRINGSTIDSPUNKT_OVERLAPPER_IKKE_BEH_1, AKTØR_ID_MOR, null);
        Fagsak fagsakNy = nyBehMorSomIkkeOverlapper.getFagsak();

        vurderOpphørAvYtelser.opprettTaskForÅVurdereKonsekvens(fagsakNy.getId(), "Test2", "Test2", Optional.of(AKTØR_ID_MOR.getId()));
        ProsessTaskData vurderKonsekvens = prosessTaskRepository.finnIkkeStartet().stream().findFirst().orElse(null);
        Optional<String> gjeldendeAktørId = Optional.ofNullable(vurderKonsekvens.getPropertyValue(KEY_GJELDENDE_AKTØR_ID));

        assertThat(gjeldendeAktørId.isPresent()).isTrue();
    }

    @Test
    public void opprettRevNårOverlappMedFPNårInnvSVPPåSammeBarn() {
        Behandling avsluttetFPBehMor = lagBehandlingMor(FØDSELS_DATO_1, AKTØR_ID_MOR, null);
        BeregningsresultatEntitet berResMorBeh1 = lagBeregningsresultat(FØDSELS_DATO_1, SISTE_DAG_MOR, Inntektskategori.ARBEIDSTAKER);
        beregningsresultatRepository.lagre(avsluttetFPBehMor, berResMorBeh1);

        Behandling nyBehSVPOverlapper = lagBehandlingSVP(AKTØR_ID_MOR);
        BeregningsresultatEntitet berResMedOverlapp = lagBeregningsresultat(FØDSELS_DATO_1.minusWeeks(3), FØDSELS_DATO_1.plusDays(4), Inntektskategori.ARBEIDSTAKER);
        beregningsresultatRepository.lagre(nyBehSVPOverlapper, berResMedOverlapp);
        Fagsak fagsakNy = nyBehSVPOverlapper.getFagsak();

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(fagsakNy.getId(), nyBehSVPOverlapper.getId());

        verify(revurderingTjenesteMockSVP, times(1)).opprettAutomatiskRevurdering(eq(fagsakNy), eq(BehandlingÅrsakType.OPPHØR_YTELSE_NYTT_BARN), any());
    }

    @Test
    public void opprettRevNårOverlappMedFPNårInnvilgerSVPPåNyttBarn() {
        Behandling avsluttetFPBehMor = lagBehandlingMor(FØDSELS_DATO_1, AKTØR_ID_MOR, null);
        BeregningsresultatEntitet berResMorBeh1 = lagBeregningsresultat(FØDSELS_DATO_1, SISTE_DAG_MOR, Inntektskategori.ARBEIDSTAKER);
        beregningsresultatRepository.lagre(avsluttetFPBehMor, berResMorBeh1);
        Fagsak avsluttetFPSakMor = avsluttetFPBehMor.getFagsak();

        Behandling nyBehSVPOverlapper = lagBehandlingSVP(AKTØR_ID_MOR);
        BeregningsresultatEntitet berResMedOverlapp = lagBeregningsresultat(SKJÆRINGSTIDSPUNKT_OVERLAPPER_BEH_1, SISTE_DAG_PER_OVERLAPP, Inntektskategori.ARBEIDSTAKER);
        beregningsresultatRepository.lagre(nyBehSVPOverlapper, berResMedOverlapp);
        Fagsak fagsakNy = nyBehSVPOverlapper.getFagsak();

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(fagsakNy.getId(), nyBehSVPOverlapper.getId());

        verify(revurderingTjenesteMockFP, times(1)).opprettAutomatiskRevurdering(eq(avsluttetFPSakMor), eq(BehandlingÅrsakType.OPPHØR_YTELSE_NYTT_BARN), any());
    }

    @Test
    public void loggOverlappFPMedGraderingNårInnvilgerSVP() {
        //Har FP som overlapper med ny SVP sak og det er ikke gradering
        Behandling avsluttetFPBehMor = lagBehandlingMor(FØDSELS_DATO_1, AKTØR_ID_MOR, null);
        BeregningsresultatEntitet berResMorBeh1 = lagBeregningsresultat(FØDSELS_DATO_1, FØDSELS_DATO_1.plusWeeks(10), Inntektskategori.ARBEIDSTAKER);
        leggTilBeregningsresPeriode(berResMorBeh1, FØDSELS_DATO_1.plusWeeks(11), FØDSELS_DATO_1.plusWeeks(21), true);
        beregningsresultatRepository.lagre(avsluttetFPBehMor, berResMorBeh1);

        Behandling nyBehSVPOverlapper = lagBehandlingSVP(AKTØR_ID_MOR);
        BeregningsresultatEntitet berResMedOverlapp = lagBeregningsresultat(FØDSELS_DATO_1.plusWeeks(18), FØDSELS_DATO_1.plusWeeks(25), Inntektskategori.ARBEIDSTAKER);
        beregningsresultatRepository.lagre(nyBehSVPOverlapper, berResMedOverlapp);
        Fagsak fagsakNy = nyBehSVPOverlapper.getFagsak();

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(fagsakNy.getId(), nyBehSVPOverlapper.getId());

        verify(revurderingTjenesteMockFP, times(0)).opprettAutomatiskRevurdering(any(), any(), any());
    }

    @Test
    public void opphørOverlappFPMedGraderingIPeriodenNårInnvilgerSVP() {
        Behandling avsluttetFPBehMor = lagBehandlingMor(FØDSELS_DATO_1, AKTØR_ID_MOR, null);
        BeregningsresultatEntitet berResMorBeh1 = lagBeregningsresultat(FØDSELS_DATO_1, SISTE_DAG_MOR, Inntektskategori.ARBEIDSTAKER);
        leggTilBeregningsresPeriode(berResMorBeh1, FØDSELS_DATO_1.minusWeeks(6), FØDSELS_DATO_1.minusWeeks(5), true);
        beregningsresultatRepository.lagre(avsluttetFPBehMor, berResMorBeh1);
        Fagsak avsluttetFPSakMor = avsluttetFPBehMor.getFagsak();

        Behandling nyBehSVPOverlapper = lagBehandlingSVP(AKTØR_ID_MOR);
        BeregningsresultatEntitet berResMedOverlapp = lagBeregningsresultat(SKJÆRINGSTIDSPUNKT_OVERLAPPER_BEH_1, SISTE_DAG_PER_OVERLAPP, Inntektskategori.ARBEIDSTAKER);
        leggTilBRAndel(berResMedOverlapp.getBeregningsresultatPerioder().stream().findFirst().orElse(null));
        beregningsresultatRepository.lagre(nyBehSVPOverlapper, berResMedOverlapp);
        Fagsak fagsakNy = nyBehSVPOverlapper.getFagsak();

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(fagsakNy.getId(), nyBehSVPOverlapper.getId());

        verify(revurderingTjenesteMockFP, times(1)).opprettAutomatiskRevurdering(eq(avsluttetFPSakMor), eq(BehandlingÅrsakType.OPPHØR_YTELSE_NYTT_BARN), any());
    }

    @Test
    public void loggOverlappSVPNårInnvilgerSVP() {
        Behandling avslSVPBeh = lagBehandlingSVP(AKTØR_ID_MOR);
        BeregningsresultatEntitet berResMorBeh1 = lagBeregningsresultat(FØDSELS_DATO_1, SISTE_DAG_MOR, Inntektskategori.ARBEIDSTAKER);
        beregningsresultatRepository.lagre(avslSVPBeh, berResMorBeh1);

        Behandling nyBehSVPOverlapper = lagBehandlingSVP(AKTØR_ID_MOR);
        BeregningsresultatEntitet berResMedOverlapp = lagBeregningsresultat(SKJÆRINGSTIDSPUNKT_OVERLAPPER_BEH_1, SISTE_DAG_PER_OVERLAPP, Inntektskategori.ARBEIDSTAKER);
        beregningsresultatRepository.lagre(nyBehSVPOverlapper, berResMedOverlapp);
        Fagsak fagsakNy = nyBehSVPOverlapper.getFagsak();

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(fagsakNy.getId(), nyBehSVPOverlapper.getId());

        verify(revurderingTjenesteMockSVP, times(0)).opprettAutomatiskRevurdering(any(), any(), any());
    }

    private Behandling lagBehandlingMor( LocalDate fødselsDato, AktørId aktørId, AktørId medfAktørId)
    {
        ScenarioMorSøkerForeldrepenger scenarioAvsluttetBehMor = ScenarioMorSøkerForeldrepenger.forFødselMedGittAktørId(aktørId);
        scenarioAvsluttetBehMor.medSøknadHendelse().medFødselsDato(fødselsDato);
        if (medfAktørId!= null) {
            scenarioAvsluttetBehMor.medSøknadAnnenPart().medAktørId(medfAktørId).medNavn("Seig Pinne").medType(SøknadAnnenPartType.FAR);
        }
        scenarioAvsluttetBehMor.medBehandlingsresultat(Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET));
        scenarioAvsluttetBehMor.medVilkårResultatType(VilkårResultatType.INNVILGET);
        scenarioAvsluttetBehMor.medBehandlingVedtak().medVedtakstidspunkt(LocalDateTime.now().minusMonths(2))
            .medVedtakResultatType(VedtakResultatType.INNVILGET);
        Behandling behandling = scenarioAvsluttetBehMor.lagre(repositoryProvider);
        avsluttBehandlingOgFagsak(behandling);
        return behandling;
    }

    private Behandling lagBehandlingFar( LocalDate fødselsDato, AktørId aktørId, AktørId medfAktørId)
    {
        ScenarioFarSøkerForeldrepenger scenarioAvsluttetBehFar = ScenarioFarSøkerForeldrepenger.forFødselMedGittAktørId(aktørId);
        scenarioAvsluttetBehFar.medSøknadHendelse().medFødselsDato(fødselsDato);
        if (medfAktørId!= null) {
            scenarioAvsluttetBehFar.medSøknadAnnenPart().medAktørId(medfAktørId).medNavn("Is Pinne").medType(SøknadAnnenPartType.MOR);
        }
        scenarioAvsluttetBehFar.medBehandlingsresultat(Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET));
        scenarioAvsluttetBehFar.medVilkårResultatType(VilkårResultatType.INNVILGET);
        scenarioAvsluttetBehFar.medBehandlingVedtak().medVedtakstidspunkt(LocalDateTime.now().minusMonths(2))
            .medVedtakResultatType(VedtakResultatType.INNVILGET);
        Behandling behandling = scenarioAvsluttetBehFar.lagre(repositoryProvider);
        avsluttBehandlingOgFagsak(behandling);
        return behandling;
    }

    private Behandling lagBehandlingSVP( AktørId aktørId)
    {
        ScenarioMorSøkerSvangerskapspenger scenarioAvslBeh = ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger();
        scenarioAvslBeh.medBruker(aktørId, NavBrukerKjønn.KVINNE);
        scenarioAvslBeh.medDefaultOppgittTilknytning();

        scenarioAvslBeh.medBehandlingsresultat(Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET));
        scenarioAvslBeh.medVilkårResultatType(VilkårResultatType.INNVILGET);
        scenarioAvslBeh.medBehandlingVedtak().medVedtakstidspunkt(LocalDateTime.now().minusMonths(2))
            .medVedtakResultatType(VedtakResultatType.INNVILGET);
        Behandling behandlingSVP = scenarioAvslBeh.lagre(repositoryProvider);
        avsluttBehandlingOgFagsak(behandlingSVP);
        return behandlingSVP;
    }


    private BeregningsresultatEntitet lagBeregningsresultat(LocalDate periodeFom, LocalDate periodeTom, Inntektskategori inntektskategori) {
        BeregningsresultatEntitet beregningsresultat = BeregningsresultatEntitet.builder().medRegelInput("input").medRegelSporing("sporing").build();
        BeregningsresultatPeriode beregningsresultatPeriode = BeregningsresultatPeriode.builder()
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
        BeregningsresultatPeriode beregningsresultatPeriode = BeregningsresultatPeriode.builder()
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
        Behandling revurdering = Behandling.fraTidligereBehandling(originalBehandling, BehandlingType.REVURDERING)
            .medBehandlingÅrsak(
                BehandlingÅrsak.builder(BehandlingÅrsakType.OPPHØR_YTELSE_NYTT_BARN)
                    .medOriginalBehandling(originalBehandling))
            .build();
        repositoryProvider.getBehandlingRepository().lagre(revurdering, repositoryProvider.getBehandlingRepository().taSkriveLås(revurdering));
    }

}
