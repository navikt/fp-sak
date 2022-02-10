package no.nav.foreldrepenger.mottak.vedtak.overlapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
import no.nav.foreldrepenger.skjæringstidspunkt.StønadsperiodeTjeneste;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.felles.prosesstask.api.TaskType;

@ExtendWith(MockitoExtension.class)
public class VurderOpphørAvYtelserTest extends EntityManagerAwareTest {

    private static final LocalDate FØDSELS_DATO_1 = VirkedagUtil.fomVirkedag(LocalDate.now().minusMonths(2));
    private static final LocalDate SISTE_DAG_MOR = FØDSELS_DATO_1.plusWeeks(6);

    private static final LocalDate SKJÆRINGSTIDSPUNKT_OVERLAPPER_BEH_1 = VirkedagUtil.fomVirkedag(LocalDate.now().minusMonths(1));
    private static final LocalDate SISTE_DAG_PER_OVERLAPP = SKJÆRINGSTIDSPUNKT_OVERLAPPER_BEH_1.plusWeeks(6);

    private static final LocalDate SKJÆRINGSTIDSPUNKT_OVERLAPPER_IKKE_BEH_1 = VirkedagUtil.fomVirkedag(
        LocalDate.now().plusMonths(1));
    private static final LocalDate SISTE_DAG_IKKE_OVERLAPP = SKJÆRINGSTIDSPUNKT_OVERLAPPER_IKKE_BEH_1.plusWeeks(6);

    private static final int DAGSATS = 100;
    private static final int DAGSATS_GRADERING = 50;
    private static final AktørId AKTØR_ID_MOR = AktørId.dummy();
    private static final AktørId MEDF_AKTØR_ID = AktørId.dummy();

    private VurderOpphørAvYtelser vurderOpphørAvYtelser;

    private BehandlingRepositoryProvider repositoryProvider;
    private FagsakRepository fagsakRepository;
    private BeregningsresultatRepository beregningsresultatRepository;
    @Mock
    private ProsessTaskTjeneste taskTjeneste;
    @Mock
    private StønadsperiodeTjeneste stønadsperiodeTjeneste;


    @BeforeEach
    public void setUp() {
        var entityManager = getEntityManager();
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        beregningsresultatRepository = new BeregningsresultatRepository(entityManager);
        fagsakRepository = new FagsakRepository(entityManager);
        vurderOpphørAvYtelser = new VurderOpphørAvYtelser(repositoryProvider, stønadsperiodeTjeneste, taskTjeneste);
    }

    @Test
    public void opphørLøpendeSakNårNySakOverlapperPåMor() {
        var avsluttetBehMor = lagBehandlingMor(FØDSELS_DATO_1, AKTØR_ID_MOR, null);
        when(stønadsperiodeTjeneste.stønadsperiodeSluttdatoEnkeltSak(avsluttetBehMor.getFagsak())).thenReturn(Optional.of(SISTE_DAG_MOR));

        var nyAvsBehandlingMor = lagBehandlingMor(SKJÆRINGSTIDSPUNKT_OVERLAPPER_BEH_1, AKTØR_ID_MOR, null);
        when(stønadsperiodeTjeneste.stønadsperiodeStartdato(nyAvsBehandlingMor)).thenReturn(Optional.of(SKJÆRINGSTIDSPUNKT_OVERLAPPER_BEH_1));

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(nyAvsBehandlingMor);

        verifiserAtProsesstaskForHåndteringAvOpphørErOpprettet(avsluttetBehMor.getFagsak());
    }

    @Test
    public void opphørSakPåMorOgMedforelderNårNySakOverlapper() {
        var avsluttetBehMor = lagBehandlingMor(FØDSELS_DATO_1, AKTØR_ID_MOR, MEDF_AKTØR_ID);
        when(stønadsperiodeTjeneste.stønadsperiodeSluttdatoEnkeltSak(avsluttetBehMor.getFagsak())).thenReturn(Optional.of(SISTE_DAG_MOR));

        var nyAvsBehandlingMor = lagBehandlingMor(SKJÆRINGSTIDSPUNKT_OVERLAPPER_BEH_1, AKTØR_ID_MOR, MEDF_AKTØR_ID);
        when(stønadsperiodeTjeneste.stønadsperiodeStartdato(nyAvsBehandlingMor)).thenReturn(Optional.of(SKJÆRINGSTIDSPUNKT_OVERLAPPER_BEH_1));

        var avslBehFarMedOverlappMor = lagBehandlingFar(FØDSELS_DATO_1, MEDF_AKTØR_ID, AKTØR_ID_MOR);
        when(stønadsperiodeTjeneste.stønadsperiodeSluttdatoEnkeltSak(avslBehFarMedOverlappMor.getFagsak())).thenReturn(Optional.of(SISTE_DAG_PER_OVERLAPP));

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(nyAvsBehandlingMor);

        verifiserAtProsesstaskForHåndteringAvOpphørErOpprettet(avsluttetBehMor.getFagsak(), 2);
    }

    @Test
    public void adopsjonFarFørstSkalIkkeOpphøresAvMor() {
        var avsluttetBehFar = lagBehandlingFPAdopsjonFar(AKTØR_ID_MOR, FØDSELS_DATO_1);
        // Lenient for pattern. Vil ikke sjekke koblet sak.
        lenient().when(stønadsperiodeTjeneste.stønadsperiodeSluttdatoEnkeltSak(avsluttetBehFar.getFagsak())).thenReturn(Optional.of(SISTE_DAG_MOR));

        var avsluttetBehMor = lagBehandlingFPAdopsjonMor(MEDF_AKTØR_ID, FØDSELS_DATO_1);
        when(stønadsperiodeTjeneste.stønadsperiodeStartdato(avsluttetBehMor)).thenReturn(Optional.of(SKJÆRINGSTIDSPUNKT_OVERLAPPER_BEH_1));

        repositoryProvider.getFagsakRelasjonRepository().kobleFagsaker(avsluttetBehFar.getFagsak(), avsluttetBehMor.getFagsak(), avsluttetBehFar);

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(avsluttetBehMor);

        verifiserAtProsesstaskForHåndteringAvOpphørIkkeErOpprettet();
    }

    @Test
    public void vurderOverlappVedAdospjonForskjelligeBarn() {
        var omsorgsovertakelsedato = LocalDate.of(2019, 1, 1);
        var adopsjonFarLop = lagBehandlingFPAdopsjonFar(null, omsorgsovertakelsedato);
        when(stønadsperiodeTjeneste.stønadsperiodeSluttdatoEnkeltSak(adopsjonFarLop.getFagsak())).thenReturn(Optional.of(SISTE_DAG_MOR));

        var omsorgsovertakelsedato2 = LocalDate.of(2020, 1, 1);
        var morAdopsjonIVB = lagBehandlingFPAdopsjonMor(adopsjonFarLop.getAktørId(), omsorgsovertakelsedato2);
        when(stønadsperiodeTjeneste.stønadsperiodeStartdato(morAdopsjonIVB)).thenReturn(Optional.of(SKJÆRINGSTIDSPUNKT_OVERLAPPER_BEH_1));

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(morAdopsjonIVB);

        verifiserAtProsesstaskForHåndteringAvOpphørErOpprettet(adopsjonFarLop.getFagsak());
    }

    @Test
    public void ikkeOpphørSakNårNySakIkkeOverlapper() {
        var avsluttetBehMor = lagBehandlingMor(FØDSELS_DATO_1, AKTØR_ID_MOR, null);
        when(stønadsperiodeTjeneste.stønadsperiodeSluttdatoEnkeltSak(avsluttetBehMor.getFagsak())).thenReturn(Optional.of(SISTE_DAG_MOR));

        var nyAvsBehandlingMor = lagBehandlingMor(SKJÆRINGSTIDSPUNKT_OVERLAPPER_IKKE_BEH_1, AKTØR_ID_MOR, null);
        when(stønadsperiodeTjeneste.stønadsperiodeStartdato(nyAvsBehandlingMor)).thenReturn(Optional.of(SKJÆRINGSTIDSPUNKT_OVERLAPPER_IKKE_BEH_1));

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(nyAvsBehandlingMor);
        verifiserAtProsesstaskForHåndteringAvOpphørIkkeErOpprettet();
    }

    @Test
    public void opphørSakPåMedforelderMenIkkeMor() {
        var avsluttetBehMor = lagBehandlingMor(FØDSELS_DATO_1, AKTØR_ID_MOR, MEDF_AKTØR_ID);
        when(stønadsperiodeTjeneste.stønadsperiodeSluttdatoEnkeltSak(avsluttetBehMor.getFagsak())).thenReturn(Optional.of(SISTE_DAG_MOR.plusWeeks(2)));

        var nyBehMorSomIkkeOverlapper = lagBehandlingMor(SKJÆRINGSTIDSPUNKT_OVERLAPPER_BEH_1, AKTØR_ID_MOR, MEDF_AKTØR_ID);
        when(stønadsperiodeTjeneste.stønadsperiodeStartdato(nyBehMorSomIkkeOverlapper)).thenReturn(Optional.of(SKJÆRINGSTIDSPUNKT_OVERLAPPER_IKKE_BEH_1));

        var avslBehFarMedOverlappMor = lagBehandlingFar(FØDSELS_DATO_1, MEDF_AKTØR_ID, AKTØR_ID_MOR);
        when(stønadsperiodeTjeneste.stønadsperiodeSluttdatoEnkeltSak(avslBehFarMedOverlappMor.getFagsak())).thenReturn(Optional.of(SISTE_DAG_PER_OVERLAPP.plusMonths(2)));

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(nyBehMorSomIkkeOverlapper);
        verifiserAtProsesstaskForHåndteringAvOpphørErOpprettet(avslBehFarMedOverlappMor.getFagsak());
    }

    @Test
    public void opphørSakPåFarNårNySakPåFarOverlapper() {
        var avslBehFar = lagBehandlingFar(FØDSELS_DATO_1, MEDF_AKTØR_ID, AKTØR_ID_MOR);
        when(stønadsperiodeTjeneste.stønadsperiodeSluttdatoEnkeltSak(avslBehFar.getFagsak())).thenReturn(Optional.of(SISTE_DAG_MOR));

        var nyBehFar = lagBehandlingFar(SKJÆRINGSTIDSPUNKT_OVERLAPPER_BEH_1, MEDF_AKTØR_ID, AKTØR_ID_MOR);
        when(stønadsperiodeTjeneste.stønadsperiodeStartdato(nyBehFar)).thenReturn(Optional.of(SKJÆRINGSTIDSPUNKT_OVERLAPPER_BEH_1));

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(nyBehFar);
        verifiserAtProsesstaskForHåndteringAvOpphørErOpprettet(avslBehFar.getFagsak());
    }

    @Test
    public void opphørSakPåMorNårSisteUttakLikStartPåNyttUttak() {
        var avsluttetBehMor = lagBehandlingMor(FØDSELS_DATO_1, AKTØR_ID_MOR, null);
        when(stønadsperiodeTjeneste.stønadsperiodeSluttdatoEnkeltSak(avsluttetBehMor.getFagsak())).thenReturn(Optional.of(SISTE_DAG_MOR));

        var nyAvsBehandlingMor = lagBehandlingMor(SKJÆRINGSTIDSPUNKT_OVERLAPPER_BEH_1, AKTØR_ID_MOR, null);
        when(stønadsperiodeTjeneste.stønadsperiodeStartdato(nyAvsBehandlingMor)).thenReturn(Optional.of(SISTE_DAG_MOR));

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(nyAvsBehandlingMor);
        verifiserAtProsesstaskForHåndteringAvOpphørErOpprettet(avsluttetBehMor.getFagsak());
    }


    @Test
    public void opprettHåndteringNårOverlappMedFPNårInnvSVPPåSammeBarn() {
        var avsluttetFPBehMor = lagBehandlingMor(FØDSELS_DATO_1, AKTØR_ID_MOR, null);
        when(stønadsperiodeTjeneste.stønadsperiode(avsluttetFPBehMor.getFagsak())).thenReturn(Optional.of(new LocalDateInterval(FØDSELS_DATO_1, SISTE_DAG_MOR)));

        var nyBehSVPOverlapper = lagBehandlingSVP(AKTØR_ID_MOR);
        when(stønadsperiodeTjeneste.stønadsperiode(nyBehSVPOverlapper)).thenReturn(Optional.of(new LocalDateInterval(FØDSELS_DATO_1.minusWeeks(3), FØDSELS_DATO_1.plusDays(4))));

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(nyBehSVPOverlapper);

        verifiserAtProsesstaskForHåndteringAvOpphørErOpprettet(nyBehSVPOverlapper.getFagsak());
    }

    @Test
    public void opprettHåndteringNårOverlappMedFPNårInnvilgerSVPPåNyttBarn() {
        var avsluttetFPBehMor = lagBehandlingMor(FØDSELS_DATO_1, AKTØR_ID_MOR, null);
        // PGA sjekk gradering
        var berResMorBeh1 = lagBeregningsresultat(FØDSELS_DATO_1, SISTE_DAG_MOR, Inntektskategori.ARBEIDSTAKER);
        beregningsresultatRepository.lagre(avsluttetFPBehMor, berResMorBeh1);
        when(stønadsperiodeTjeneste.stønadsperiode(avsluttetFPBehMor.getFagsak())).thenReturn(Optional.of(new LocalDateInterval(FØDSELS_DATO_1, SISTE_DAG_MOR)));

        var nyBehSVPOverlapper = lagBehandlingSVP(AKTØR_ID_MOR);
        when(stønadsperiodeTjeneste.stønadsperiode(nyBehSVPOverlapper)).thenReturn(Optional.of(new LocalDateInterval(SKJÆRINGSTIDSPUNKT_OVERLAPPER_BEH_1, SISTE_DAG_PER_OVERLAPP)));

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(nyBehSVPOverlapper);

        verifiserAtProsesstaskForHåndteringAvOpphørErOpprettet(avsluttetFPBehMor.getFagsak());
    }

    @Test
    public void loggOverlappFPMedGraderingNårInnvilgerSVP() {
        //Har FP som overlapper med ny SVP sak og det er ikke gradering
        var avsluttetFPBehMor = lagBehandlingMor(FØDSELS_DATO_1, AKTØR_ID_MOR, null);
        var berResMorBeh1 = lagBeregningsresultat(FØDSELS_DATO_1, FØDSELS_DATO_1.plusWeeks(10), Inntektskategori.ARBEIDSTAKER);
        leggTilBeregningsresPeriode(berResMorBeh1, FØDSELS_DATO_1.plusWeeks(11), FØDSELS_DATO_1.plusWeeks(21), true);
        beregningsresultatRepository.lagre(avsluttetFPBehMor, berResMorBeh1);
        when(stønadsperiodeTjeneste.stønadsperiode(avsluttetFPBehMor.getFagsak())).thenReturn(Optional.of(new LocalDateInterval(FØDSELS_DATO_1, FØDSELS_DATO_1.plusWeeks(21))));

        var nyBehSVPOverlapper = lagBehandlingSVP(AKTØR_ID_MOR);
        when(stønadsperiodeTjeneste.stønadsperiode(nyBehSVPOverlapper)).thenReturn(Optional.of(new LocalDateInterval(FØDSELS_DATO_1.plusWeeks(18), FØDSELS_DATO_1.plusWeeks(25))));

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(nyBehSVPOverlapper);

        verifiserAtProsesstaskForHåndteringAvOpphørIkkeErOpprettet();
    }

    @Test
    public void opphørOverlappFPMedGraderingIPeriodenNårInnvilgerSVP() {
        var avsluttetFPBehMor = lagBehandlingMor(FØDSELS_DATO_1, AKTØR_ID_MOR, null);
        var berResMorBeh1 = lagBeregningsresultat(FØDSELS_DATO_1, SISTE_DAG_MOR, Inntektskategori.ARBEIDSTAKER);
        leggTilBeregningsresPeriode(berResMorBeh1, FØDSELS_DATO_1.minusWeeks(6), FØDSELS_DATO_1.minusWeeks(5), true);
        beregningsresultatRepository.lagre(avsluttetFPBehMor, berResMorBeh1);
        when(stønadsperiodeTjeneste.stønadsperiode(avsluttetFPBehMor.getFagsak())).thenReturn(Optional.of(new LocalDateInterval(FØDSELS_DATO_1.minusWeeks(6), SISTE_DAG_MOR)));

        var nyBehSVPOverlapper = lagBehandlingSVP(AKTØR_ID_MOR);
        when(stønadsperiodeTjeneste.stønadsperiode(nyBehSVPOverlapper)).thenReturn(Optional.of(new LocalDateInterval(SKJÆRINGSTIDSPUNKT_OVERLAPPER_BEH_1, SISTE_DAG_PER_OVERLAPP)));

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(nyBehSVPOverlapper);

        verifiserAtProsesstaskForHåndteringAvOpphørErOpprettet(avsluttetFPBehMor.getFagsak());
    }

    @Test
    public void loggOverlappSVPNårInnvilgerSVP() {
        var avslSVPBeh = lagBehandlingSVP(AKTØR_ID_MOR);
        when(stønadsperiodeTjeneste.stønadsperiodeSluttdatoEnkeltSak(avslSVPBeh.getFagsak())).thenReturn(Optional.of(SISTE_DAG_MOR));

        var nyBehSVPOverlapper = lagBehandlingSVP(AKTØR_ID_MOR);
        when(stønadsperiodeTjeneste.stønadsperiode(nyBehSVPOverlapper)).thenReturn(Optional.of(new LocalDateInterval(SKJÆRINGSTIDSPUNKT_OVERLAPPER_BEH_1, SISTE_DAG_PER_OVERLAPP)));

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(nyBehSVPOverlapper);

        verifiserAtProsesstaskForHåndteringAvOpphørIkkeErOpprettet();
    }

    @Test
    public void opprettIkkeRevNårOverlappMedFPNårInnvilgerSVPPåNyttBarn() {
        var fud = LocalDate.of(2021, 6, 4);
        var avsluttetFPBehMor = lagBehandlingMor(fud.plusWeeks(3), AKTØR_ID_MOR, null);
        var berResMorBeh1 = lagBeregningsresultat(fud, fud.plusMonths(3), Inntektskategori.ARBEIDSTAKER);
        beregningsresultatRepository.lagre(avsluttetFPBehMor, berResMorBeh1);
        when(stønadsperiodeTjeneste.stønadsperiode(avsluttetFPBehMor.getFagsak())).thenReturn(Optional.of(new LocalDateInterval(fud, fud.plusMonths(3))));

        var nyBehSVPOverlapper = lagBehandlingSVP(AKTØR_ID_MOR);
        when(stønadsperiodeTjeneste.stønadsperiode(nyBehSVPOverlapper)).thenReturn(Optional.of(new LocalDateInterval(LocalDate.of(2021, 5, 3), fud.minusDays(1))));

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(nyBehSVPOverlapper);

        verifiserAtProsesstaskForHåndteringAvOpphørIkkeErOpprettet();
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
        scenarioAvsluttetBehMor.medVilkårResultatType(VilkårResultatType.INNVILGET);
        scenarioAvsluttetBehMor.medBehandlingVedtak()
            .medVedtakstidspunkt(LocalDateTime.now().minusMonths(2))
            .medVedtakResultatType(VedtakResultatType.INNVILGET);
        scenarioAvsluttetBehMor.medDefaultOppgittDekningsgrad();
        var behandling = scenarioAvsluttetBehMor.lagre(repositoryProvider);
        avsluttBehandlingOgFagsak(behandling);
        return behandling;
    }

    private Behandling lagBehandlingFar(LocalDate fødselsDato, AktørId aktørId, AktørId medfAktørId) {
        var scenarioAvsluttetBehFar = ScenarioFarSøkerForeldrepenger.forFødselMedGittAktørId(aktørId);
        scenarioAvsluttetBehFar.medSøknadHendelse().medFødselsDato(fødselsDato);
        if (medfAktørId != null) {
            scenarioAvsluttetBehFar.medSøknadAnnenPart()
                .medAktørId(medfAktørId)
                .medNavn("Is Pinne")
                .medType(SøknadAnnenPartType.MOR);
        }
        scenarioAvsluttetBehFar.medBehandlingsresultat(
            Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET));
        scenarioAvsluttetBehFar.medVilkårResultatType(VilkårResultatType.INNVILGET);
        scenarioAvsluttetBehFar.medBehandlingVedtak()
            .medVedtakstidspunkt(LocalDateTime.now().minusMonths(2))
            .medVedtakResultatType(VedtakResultatType.INNVILGET);
        scenarioAvsluttetBehFar.medDefaultOppgittDekningsgrad();
        var behandling = scenarioAvsluttetBehFar.lagre(repositoryProvider);
        avsluttBehandlingOgFagsak(behandling);
        return behandling;
    }

    private Behandling lagBehandlingFPAdopsjonMor(AktørId medfAktørId, LocalDate omsorgsovertakelsedato) {
        var scenario = ScenarioMorSøkerForeldrepenger.forAdopsjon();
        scenario.medSøknadHendelse()
            .medAdopsjon(
                scenario.medSøknadHendelse().getAdopsjonBuilder().medOmsorgsovertakelseDato(omsorgsovertakelsedato));
        if (medfAktørId != null) {
            scenario.medSøknadAnnenPart()
                .medAktørId(medfAktørId)
                .medNavn("Seig Pinne")
                .medType(SøknadAnnenPartType.FAR);
        }
        scenario.medBehandlingsresultat(
            Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET));
        scenario.medVilkårResultatType(VilkårResultatType.INNVILGET);
        scenario.medBehandlingVedtak()
            .medVedtakstidspunkt(LocalDateTime.now().minusMonths(2))
            .medVedtakResultatType(VedtakResultatType.INNVILGET);
        scenario.medDefaultOppgittDekningsgrad();
        var behandling = scenario.lagre(repositoryProvider);
        avsluttBehandlingOgFagsak(behandling);

        return behandling;
    }

    private Behandling lagBehandlingFPAdopsjonFar(AktørId medfAktørId, LocalDate omsorgsovertakelsedato) {
        var scenario = ScenarioFarSøkerForeldrepenger.forAdopsjon();
        scenario.medSøknadHendelse()
            .medAdopsjon(
                scenario.medSøknadHendelse().getAdopsjonBuilder().medOmsorgsovertakelseDato(omsorgsovertakelsedato));
        if (medfAktørId != null) {
            scenario.medSøknadAnnenPart()
                .medAktørId(medfAktørId)
                .medNavn("Seig Pinne")
                .medType(SøknadAnnenPartType.FAR);
        }
        scenario.medBehandlingsresultat(
            Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET));
        scenario.medVilkårResultatType(VilkårResultatType.INNVILGET);
        scenario.medBehandlingVedtak()
            .medVedtakstidspunkt(LocalDateTime.now().minusMonths(2))
            .medVedtakResultatType(VedtakResultatType.INNVILGET);
        scenario.medDefaultOppgittDekningsgrad();
        var behandling = scenario.lagre(repositoryProvider);
        avsluttBehandlingOgFagsak(behandling);

        return behandling;
    }

    private Behandling lagBehandlingSVP(AktørId aktørId) {
        var scenarioAvslBeh = ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger();
        scenarioAvslBeh.medBruker(aktørId, NavBrukerKjønn.KVINNE);
        scenarioAvslBeh.medDefaultOppgittTilknytning();

        scenarioAvslBeh.medBehandlingsresultat(
            Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET));
        scenarioAvslBeh.medVilkårResultatType(VilkårResultatType.INNVILGET);
        scenarioAvslBeh.medBehandlingVedtak()
            .medVedtakstidspunkt(LocalDateTime.now().minusMonths(1))
            .medVedtakResultatType(VedtakResultatType.INNVILGET);
        var behandlingSVP = scenarioAvslBeh.lagre(repositoryProvider);
        avsluttBehandlingOgFagsak(behandlingSVP);
        return behandlingSVP;
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

    private void leggTilBeregningsresPeriode(BeregningsresultatEntitet beregningsresultatEntitet,
                                             LocalDate fom,
                                             LocalDate tom,
                                             boolean gradering) {
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

    private void avsluttBehandlingOgFagsak(Behandling behandling) {
        behandling.avsluttBehandling();
        repositoryProvider.getBehandlingRepository()
            .lagre(behandling, repositoryProvider.getBehandlingRepository().taSkriveLås(behandling));
        fagsakRepository.oppdaterFagsakStatus(behandling.getFagsakId(), FagsakStatus.LØPENDE);
    }

    private void verifiserAtProsesstaskForHåndteringAvOpphørErOpprettet(Fagsak fagsak) {
        verifiserAtProsesstaskForHåndteringAvOpphørErOpprettet(fagsak, 1);
    }

    private void verifiserAtProsesstaskForHåndteringAvOpphørErOpprettet(Fagsak fagsak, int times) {
        var captor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(taskTjeneste, times(times)).lagre(captor.capture());
        var håndterOpphør = captor.getAllValues()
            .stream()
            .filter(t -> t.taskType().equals(TaskType.forProsessTask(HåndterOpphørAvYtelserTask.class)))
            .findFirst()
            .orElse(null);
        assertThat(håndterOpphør.getFagsakId()).isEqualTo(fagsak.getId());
        assertThat(håndterOpphør.getPropertyValue(HåndterOpphørAvYtelserTask.BESKRIVELSE_KEY)).isNotNull();
        assertThat(håndterOpphør.getPropertyValue(HåndterOpphørAvYtelserTask.BEHANDLING_ÅRSAK_KEY)).isEqualTo(
            BehandlingÅrsakType.OPPHØR_YTELSE_NYTT_BARN.getKode());
    }

    private void verifiserAtProsesstaskForHåndteringAvOpphørIkkeErOpprettet() {
        verifyNoInteractions(taskTjeneste);
    }
}
