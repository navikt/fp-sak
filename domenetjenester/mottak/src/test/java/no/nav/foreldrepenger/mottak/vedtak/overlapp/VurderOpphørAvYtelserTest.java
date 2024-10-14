package no.nav.foreldrepenger.mottak.vedtak.overlapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadAnnenPartType;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerSvangerskapspenger;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.tid.VirkedagUtil;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.StønadsperiodeTjeneste;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.felles.prosesstask.api.TaskType;

@ExtendWith(MockitoExtension.class)
class VurderOpphørAvYtelserTest extends EntityManagerAwareTest {

    private static final LocalDate DATO = LocalDate.of(2024, 6, 6);
    private static final LocalDate START_PERIODEDAG_LØPENDE_BEHANDLING = VirkedagUtil.fomVirkedag(DATO.minusWeeks(100));
    private static final LocalDate SISTE_PERIODEDAG_LØPENDE_BEHANDLING = START_PERIODEDAG_LØPENDE_BEHANDLING.plusWeeks(60);

    private static final LocalDate START_PERIODEDAG_OVERLAPP = VirkedagUtil.fomVirkedag(SISTE_PERIODEDAG_LØPENDE_BEHANDLING.minusWeeks(1));
    private static final LocalDate SISTE_PERIODEDAG_OVERLAPP = START_PERIODEDAG_OVERLAPP.plusWeeks(6);

    private static final LocalDate START_PERIODEDAG_IKKE_OVERLAPP = VirkedagUtil.fomVirkedag(DATO);

    private static final AktørId AKTØR_ID_MOR = AktørId.dummy();
    private static final AktørId MEDF_AKTØR_ID = AktørId.dummy();
    private VurderOpphørAvYtelser vurderOpphørAvYtelser;
    private BehandlingRepositoryProvider repositoryProvider;
    private FagsakRepository fagsakRepository;
    @Mock
    private ProsessTaskTjeneste taskTjeneste;
    @Mock
    private StønadsperiodeTjeneste stønadsperiodeTjeneste;
    @Mock
    private FamilieHendelseRepository familieHendelseRepository;
    @Mock
    private FamilieHendelseGrunnlagEntitet familieHendelseGrunnlagEntitet, familieHendelseGrunnlagEntitetAndreBarn, familieHendelseGrunnlagEntitetAndreFar;
    @Mock
    private FamilieHendelseEntitet familieHendelseEntitet, familieHendelseEntitetAndreBarn, familieHendelseEntitetFar;
    @Mock
    SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;


    @BeforeEach
    public void setUp() {
        var entityManager = getEntityManager();
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        fagsakRepository = new FagsakRepository(entityManager);
        var fagsakRelasjonTjeneste = new FagsakRelasjonTjeneste(repositoryProvider);
        vurderOpphørAvYtelser = new VurderOpphørAvYtelser(repositoryProvider, stønadsperiodeTjeneste, taskTjeneste, fagsakRelasjonTjeneste, familieHendelseRepository, skjæringstidspunktTjeneste);
    }

    @Test
    void opphørLøpendeSakNårNySakOverlapperPåMor() {
        //behandling 1 mor
        var avsluttetBehMor = lagBehandlingMor(START_PERIODEDAG_LØPENDE_BEHANDLING, AKTØR_ID_MOR, null);
        when(stønadsperiodeTjeneste.stønadsperiodeStartdato(avsluttetBehMor.getFagsak())).thenReturn(Optional.of(START_PERIODEDAG_LØPENDE_BEHANDLING));
        when(stønadsperiodeTjeneste.stønadsperiodeSluttdatoEnkeltSak(avsluttetBehMor.getFagsak())).thenReturn(Optional.of(
            SISTE_PERIODEDAG_LØPENDE_BEHANDLING));
        when(familieHendelseRepository.hentAggregat(avsluttetBehMor.getId())).thenReturn(familieHendelseGrunnlagEntitet);
        when(familieHendelseRepository.hentAggregat(avsluttetBehMor.getId()).getGjeldendeVersjon()).thenReturn(familieHendelseEntitet);
        when(familieHendelseEntitet.getSkjæringstidspunkt()).thenReturn(START_PERIODEDAG_LØPENDE_BEHANDLING);

        //behandling 2 på overlapper behandling 1
        var nyAvsBehandlingMor = lagBehandlingMor(START_PERIODEDAG_OVERLAPP, AKTØR_ID_MOR, null);
        when(stønadsperiodeTjeneste.stønadsperiodeStartdato(nyAvsBehandlingMor)).thenReturn(Optional.of(START_PERIODEDAG_OVERLAPP));
        when(familieHendelseRepository.hentAggregat(nyAvsBehandlingMor.getId())).thenReturn(familieHendelseGrunnlagEntitet);
        when(familieHendelseRepository.hentAggregat(nyAvsBehandlingMor.getId()).getGjeldendeVersjon()).thenReturn(familieHendelseEntitet);
        when(familieHendelseEntitet.getSkjæringstidspunkt()).thenReturn(START_PERIODEDAG_OVERLAPP);

        var skjæringstidspunkt = Skjæringstidspunkt.builder().medUtenMinsterett(false).build();
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(any())).thenReturn(skjæringstidspunkt);

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(nyAvsBehandlingMor);

        var håndterOpphør = verifiserAtProsesstaskForHåndteringAvOpphørErOpprettet(1);
        assertThat(håndterOpphør.getFagsakId()).isEqualTo(avsluttetBehMor.getFagsak().getId());
        assertThat(håndterOpphør.getPropertyValue(HåndterOpphørAvYtelserTask.BESKRIVELSE_KEY)).isNull();
    }

    @Test
    void opphørSakPåMorOgMedforelderNårNySakOverlapper() {
        //behandling 1 mor barn 1
        var avsluttetBehMor = lagBehandlingMor(START_PERIODEDAG_LØPENDE_BEHANDLING, AKTØR_ID_MOR, MEDF_AKTØR_ID);
        when(stønadsperiodeTjeneste.stønadsperiodeStartdato(avsluttetBehMor.getFagsak())).thenReturn(Optional.of(START_PERIODEDAG_LØPENDE_BEHANDLING));
        when(stønadsperiodeTjeneste.stønadsperiodeSluttdatoEnkeltSak(avsluttetBehMor.getFagsak())).thenReturn(Optional.of(
            SISTE_PERIODEDAG_LØPENDE_BEHANDLING));
        when(familieHendelseRepository.hentAggregat(avsluttetBehMor.getId())).thenReturn(familieHendelseGrunnlagEntitet);
        when(familieHendelseRepository.hentAggregat(avsluttetBehMor.getId()).getGjeldendeVersjon()).thenReturn(familieHendelseEntitet);
        when(familieHendelseEntitet.getSkjæringstidspunkt()).thenReturn(START_PERIODEDAG_LØPENDE_BEHANDLING);

        //behandling 1 far barn 1
        var avslBehFarMedOverlappMor = lagBehandlingFar(START_PERIODEDAG_OVERLAPP.plusMonths(1), MEDF_AKTØR_ID, AKTØR_ID_MOR);
        when(stønadsperiodeTjeneste.stønadsperiodeStartdato(avslBehFarMedOverlappMor.getFagsak())).thenReturn(Optional.of(START_PERIODEDAG_OVERLAPP.plusMonths(1)));
        when(stønadsperiodeTjeneste.stønadsperiodeSluttdatoEnkeltSak(avslBehFarMedOverlappMor.getFagsak())).thenReturn(Optional.of(
            SISTE_PERIODEDAG_LØPENDE_BEHANDLING));
        when(familieHendelseRepository.hentAggregat(avslBehFarMedOverlappMor.getId())).thenReturn(familieHendelseGrunnlagEntitet);
        when(familieHendelseRepository.hentAggregat(avslBehFarMedOverlappMor.getId()).getGjeldendeVersjon()).thenReturn(familieHendelseEntitet);
        when(familieHendelseEntitet.getSkjæringstidspunkt()).thenReturn(START_PERIODEDAG_LØPENDE_BEHANDLING);

        //Barn 2 - ny behandling mor som overlapper forrige sak på mor og far
        var nyAvsBehandlingMor = lagBehandlingMor(SISTE_PERIODEDAG_LØPENDE_BEHANDLING.minusWeeks(1), AKTØR_ID_MOR, MEDF_AKTØR_ID);
        when(stønadsperiodeTjeneste.stønadsperiodeStartdato(nyAvsBehandlingMor)).thenReturn(Optional.of(START_PERIODEDAG_OVERLAPP));
        when(familieHendelseRepository.hentAggregat(nyAvsBehandlingMor.getId())).thenReturn(familieHendelseGrunnlagEntitet);
        when(familieHendelseRepository.hentAggregat(nyAvsBehandlingMor.getId()).getGjeldendeVersjon()).thenReturn(familieHendelseEntitet);
        when(familieHendelseEntitet.getSkjæringstidspunkt()).thenReturn(START_PERIODEDAG_OVERLAPP);

        //ingen minsterett
        var skjæringstidspunkt = Skjæringstidspunkt.builder().medUtenMinsterett(false).build();
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(any())).thenReturn(skjæringstidspunkt);

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(nyAvsBehandlingMor);

        var håndterOpphør = verifiserAtProsesstaskForHåndteringAvOpphørErOpprettet(2);
        assertThat(håndterOpphør.getFagsakId()).isEqualTo(avsluttetBehMor.getFagsak().getId());
        assertThat(håndterOpphør.getPropertyValue(HåndterOpphørAvYtelserTask.BESKRIVELSE_KEY)).isNull();
    }

    @Test
    void adopsjonFarFørstSkalIkkeOpphøresAvMor() {
        var avsluttetBehFar = lagBehandlingFPAdopsjonFar(AKTØR_ID_MOR, START_PERIODEDAG_LØPENDE_BEHANDLING);
        // Lenient for pattern. Vil ikke sjekke koblet sak.
        lenient().when(stønadsperiodeTjeneste.stønadsperiodeStartdato(avsluttetBehFar.getFagsak())).thenReturn(Optional.of(START_PERIODEDAG_LØPENDE_BEHANDLING));
        lenient().when(stønadsperiodeTjeneste.stønadsperiodeSluttdatoEnkeltSak(avsluttetBehFar.getFagsak())).thenReturn(Optional.of(
            SISTE_PERIODEDAG_LØPENDE_BEHANDLING));

        var avsluttetBehMor = lagBehandlingFPAdopsjonMor(MEDF_AKTØR_ID, START_PERIODEDAG_LØPENDE_BEHANDLING);
        when(stønadsperiodeTjeneste.stønadsperiodeStartdato(avsluttetBehMor)).thenReturn(Optional.of(START_PERIODEDAG_OVERLAPP));

        repositoryProvider.getFagsakRelasjonRepository().kobleFagsaker(avsluttetBehFar.getFagsak(), avsluttetBehMor.getFagsak());

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(avsluttetBehMor);

        verifiserAtProsesstaskForHåndteringAvOpphørIkkeErOpprettet();
    }

    @Test
    void vurderOverlappVedAdospjonForskjelligeBarn() {
        //behandling 1 med adopsjon
        var omsorgsovertakelsedato = LocalDate.of(2019, 1, 1);
        var adopsjonFarLop = lagBehandlingFPAdopsjonFar(null, omsorgsovertakelsedato);
        when(stønadsperiodeTjeneste.stønadsperiodeStartdato(adopsjonFarLop.getFagsak())).thenReturn(Optional.of(START_PERIODEDAG_LØPENDE_BEHANDLING));
        when(stønadsperiodeTjeneste.stønadsperiodeSluttdatoEnkeltSak(adopsjonFarLop.getFagsak())).thenReturn(Optional.of(
            SISTE_PERIODEDAG_LØPENDE_BEHANDLING));

        when(familieHendelseRepository.hentAggregat(adopsjonFarLop.getId())).thenReturn(familieHendelseGrunnlagEntitet);
        when(familieHendelseRepository.hentAggregat(adopsjonFarLop.getId()).getGjeldendeVersjon()).thenReturn(familieHendelseEntitet);
        when(familieHendelseEntitet.getSkjæringstidspunkt()).thenReturn(omsorgsovertakelsedato);

        //behandling 2 med adopsjon nytt barn to med overlapp
        var omsorgsovertakelsedato2 = LocalDate.of(2020, 1, 1);
        var morAdopsjonIVB = lagBehandlingFPAdopsjonMor(adopsjonFarLop.getAktørId(), omsorgsovertakelsedato2);
        when(stønadsperiodeTjeneste.stønadsperiodeStartdato(morAdopsjonIVB)).thenReturn(Optional.of(START_PERIODEDAG_OVERLAPP));

        when(familieHendelseRepository.hentAggregat(morAdopsjonIVB.getId())).thenReturn(familieHendelseGrunnlagEntitet);
        when(familieHendelseRepository.hentAggregat(morAdopsjonIVB.getId()).getGjeldendeVersjon()).thenReturn(familieHendelseEntitet);
        when(familieHendelseEntitet.getSkjæringstidspunkt()).thenReturn(omsorgsovertakelsedato2);

        var skjæringstidspunkt = Skjæringstidspunkt.builder().medUtenMinsterett(false).build();
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(any())).thenReturn(skjæringstidspunkt);

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(morAdopsjonIVB);

        var håndterOpphør = verifiserAtProsesstaskForHåndteringAvOpphørErOpprettet(1);
        assertThat(håndterOpphør.getFagsakId()).isEqualTo(adopsjonFarLop.getFagsak().getId());
        assertThat(håndterOpphør.getPropertyValue(HåndterOpphørAvYtelserTask.BESKRIVELSE_KEY)).isNull();

    }

    @Test
    void ikkeOpphørSakNårNySakIkkeOverlapper() {
        var avsluttetBehMor = lagBehandlingMor(START_PERIODEDAG_LØPENDE_BEHANDLING, AKTØR_ID_MOR, null);
        when(stønadsperiodeTjeneste.stønadsperiodeSluttdatoEnkeltSak(avsluttetBehMor.getFagsak())).thenReturn(Optional.of(
            SISTE_PERIODEDAG_LØPENDE_BEHANDLING));

        var nyAvsBehandlingMor = lagBehandlingMor(START_PERIODEDAG_IKKE_OVERLAPP, AKTØR_ID_MOR, null);
        when(stønadsperiodeTjeneste.stønadsperiodeStartdato(nyAvsBehandlingMor)).thenReturn(Optional.of(START_PERIODEDAG_IKKE_OVERLAPP));

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(nyAvsBehandlingMor);
        verifiserAtProsesstaskForHåndteringAvOpphørIkkeErOpprettet();
    }

    @Test
    void opphørSakPåMedforelderMenIkkeMor() {
        var startPeriodedagAvslutttetFar = START_PERIODEDAG_LØPENDE_BEHANDLING.plusMonths(3);
        var sistePeriodedagAvsluttetFar = SISTE_PERIODEDAG_LØPENDE_BEHANDLING.plusMonths(2);
        var startPeriodedagNyMorOverlappFar = sistePeriodedagAvsluttetFar.minusDays(3);

        //behandling 1 mor
        var avsluttetBehMor = lagBehandlingMor(START_PERIODEDAG_LØPENDE_BEHANDLING, AKTØR_ID_MOR, MEDF_AKTØR_ID);
        when(stønadsperiodeTjeneste.stønadsperiodeStartdato(avsluttetBehMor.getFagsak())).thenReturn(Optional.of(START_PERIODEDAG_LØPENDE_BEHANDLING));
        when(stønadsperiodeTjeneste.stønadsperiodeSluttdatoEnkeltSak(avsluttetBehMor.getFagsak())).thenReturn(Optional.of(SISTE_PERIODEDAG_LØPENDE_BEHANDLING.plusWeeks(2)));
        //behandling 2 far
        var avsluttetBehFar = lagBehandlingFar(startPeriodedagAvslutttetFar, MEDF_AKTØR_ID, AKTØR_ID_MOR);
        when(stønadsperiodeTjeneste.stønadsperiodeStartdato(avsluttetBehFar.getFagsak())).thenReturn(Optional.of(startPeriodedagAvslutttetFar));
        when(stønadsperiodeTjeneste.stønadsperiodeSluttdatoEnkeltSak(avsluttetBehFar.getFagsak())).thenReturn(Optional.of(sistePeriodedagAvsluttetFar));
        when(familieHendelseRepository.hentAggregat(avsluttetBehFar.getId())).thenReturn(familieHendelseGrunnlagEntitetAndreBarn);
        when(familieHendelseRepository.hentAggregat(avsluttetBehFar.getId()).getGjeldendeVersjon()).thenReturn(familieHendelseEntitetAndreBarn);
        when(familieHendelseEntitet.getSkjæringstidspunkt()).thenReturn(startPeriodedagAvslutttetFar);
        //behandling 3 på nytt barn overlapper med behandling 2
        var nyBehMorSomOverlapperFar = lagBehandlingMor(startPeriodedagNyMorOverlappFar, AKTØR_ID_MOR, MEDF_AKTØR_ID);
        when(stønadsperiodeTjeneste.stønadsperiodeStartdato(nyBehMorSomOverlapperFar)).thenReturn(Optional.of(startPeriodedagNyMorOverlappFar));

        lenient().when(familieHendelseRepository.hentAggregat(nyBehMorSomOverlapperFar.getId())).thenReturn(familieHendelseGrunnlagEntitetAndreFar);
        lenient().when(familieHendelseRepository.hentAggregat(nyBehMorSomOverlapperFar.getId()).getGjeldendeVersjon()).thenReturn(familieHendelseEntitetFar);
        lenient().when(familieHendelseEntitet.getSkjæringstidspunkt()).thenReturn(startPeriodedagNyMorOverlappFar);

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(nyBehMorSomOverlapperFar);

        var håndterOpphør = verifiserAtProsesstaskForHåndteringAvOpphørErOpprettet(1);
        assertThat(håndterOpphør.getFagsakId()).isEqualTo(avsluttetBehFar.getFagsak().getId());
        assertThat(håndterOpphør.getPropertyValue(HåndterOpphørAvYtelserTask.BESKRIVELSE_KEY)).isNull();

    }

    @Test
    void opphørSakPåFarNårNySakPåFarOverlapper() {
        //Behandling 1 far
        var avslBehFar = lagBehandlingFar(START_PERIODEDAG_LØPENDE_BEHANDLING, MEDF_AKTØR_ID, AKTØR_ID_MOR);
        when(stønadsperiodeTjeneste.stønadsperiodeStartdato(avslBehFar.getFagsak())).thenReturn(Optional.of(START_PERIODEDAG_LØPENDE_BEHANDLING));
        when(stønadsperiodeTjeneste.stønadsperiodeSluttdatoEnkeltSak(avslBehFar.getFagsak())).thenReturn(Optional.of(
            SISTE_PERIODEDAG_LØPENDE_BEHANDLING));

        lenient().when(familieHendelseRepository.hentAggregat(avslBehFar.getId())).thenReturn(familieHendelseGrunnlagEntitet);
        lenient().when(familieHendelseRepository.hentAggregat(avslBehFar.getId()).getGjeldendeVersjon()).thenReturn(familieHendelseEntitet);
        lenient().when(familieHendelseEntitet.getSkjæringstidspunkt()).thenReturn(START_PERIODEDAG_LØPENDE_BEHANDLING.plusWeeks(60));
        //Behandling 2 på far med overlapp
        var nyBehFar = lagBehandlingFar(START_PERIODEDAG_OVERLAPP, MEDF_AKTØR_ID, AKTØR_ID_MOR);
        when(stønadsperiodeTjeneste.stønadsperiodeStartdato(nyBehFar)).thenReturn(Optional.of(START_PERIODEDAG_OVERLAPP));

        when(familieHendelseRepository.hentAggregat(nyBehFar.getId())).thenReturn(familieHendelseGrunnlagEntitet);
        when(familieHendelseRepository.hentAggregat(nyBehFar.getId()).getGjeldendeVersjon()).thenReturn(familieHendelseEntitet);
        when(familieHendelseEntitet.getSkjæringstidspunkt()).thenReturn(START_PERIODEDAG_LØPENDE_BEHANDLING);

        var skjæringstidspunkt = Skjæringstidspunkt.builder().medUtenMinsterett(false).build();
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(any())).thenReturn(skjæringstidspunkt);

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(nyBehFar);

        var håndterOpphør = verifiserAtProsesstaskForHåndteringAvOpphørErOpprettet(1);
        assertThat(håndterOpphør.getFagsakId()).isEqualTo(avslBehFar.getFagsak().getId());
        assertThat(håndterOpphør.getPropertyValue(HåndterOpphørAvYtelserTask.BESKRIVELSE_KEY)).isNull();

    }

    @Test
    void opphørSakPåMorNårSisteUttakLikStartPåNyttUttak() {
        //Behandling 1 mor
        var avsluttetBehMor = lagBehandlingMor(START_PERIODEDAG_LØPENDE_BEHANDLING, AKTØR_ID_MOR, null);
        when(stønadsperiodeTjeneste.stønadsperiodeStartdato(avsluttetBehMor.getFagsak())).thenReturn(Optional.of(START_PERIODEDAG_LØPENDE_BEHANDLING));
        when(stønadsperiodeTjeneste.stønadsperiodeSluttdatoEnkeltSak(avsluttetBehMor.getFagsak())).thenReturn(Optional.of(
            SISTE_PERIODEDAG_LØPENDE_BEHANDLING));
        lenient().when(familieHendelseRepository.hentAggregat(avsluttetBehMor.getId())).thenReturn(familieHendelseGrunnlagEntitet);
        lenient().when(familieHendelseRepository.hentAggregat(avsluttetBehMor.getId()).getGjeldendeVersjon()).thenReturn(familieHendelseEntitet);
        lenient().when(familieHendelseEntitet.getSkjæringstidspunkt()).thenReturn(START_PERIODEDAG_LØPENDE_BEHANDLING);
        //Behandling 2 på mor med nytt barn som overlapper behandling 1
        var nyAvsBehandlingMor = lagBehandlingMor(START_PERIODEDAG_OVERLAPP, AKTØR_ID_MOR, null);
        when(stønadsperiodeTjeneste.stønadsperiodeStartdato(nyAvsBehandlingMor)).thenReturn(Optional.of(SISTE_PERIODEDAG_LØPENDE_BEHANDLING));
        var skjæringstidspunkt = Skjæringstidspunkt.builder().medUtenMinsterett(false).build();
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(any())).thenReturn(skjæringstidspunkt);
        when(familieHendelseRepository.hentAggregat(nyAvsBehandlingMor.getId())).thenReturn(familieHendelseGrunnlagEntitet);
        when(familieHendelseRepository.hentAggregat(nyAvsBehandlingMor.getId()).getGjeldendeVersjon()).thenReturn(familieHendelseEntitet);
        when(familieHendelseEntitet.getSkjæringstidspunkt()).thenReturn(START_PERIODEDAG_OVERLAPP);

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(nyAvsBehandlingMor);

        var håndterOpphør = verifiserAtProsesstaskForHåndteringAvOpphørErOpprettet(1);
        assertThat(håndterOpphør.getFagsakId()).isEqualTo(avsluttetBehMor.getFagsak().getId());
        assertThat(håndterOpphør.getPropertyValue(HåndterOpphørAvYtelserTask.BESKRIVELSE_KEY)).isNull();
    }

    @Test
    void opphørSakPåMorNårToTetteMedTekstOmToTette() {
        //Behandling 1 mor
        var avsluttetBehMor = lagBehandlingMor(DATO, AKTØR_ID_MOR, null);
        when(stønadsperiodeTjeneste.stønadsperiodeStartdato(avsluttetBehMor.getFagsak())).thenReturn(Optional.of(START_PERIODEDAG_LØPENDE_BEHANDLING));
        when(stønadsperiodeTjeneste.stønadsperiodeSluttdatoEnkeltSak(avsluttetBehMor.getFagsak())).thenReturn(Optional.of(
            SISTE_PERIODEDAG_LØPENDE_BEHANDLING));
        when(stønadsperiodeTjeneste.utbetalingsTidslinjeEnkeltSak(avsluttetBehMor.getFagsak()))
            .thenReturn(new LocalDateTimeline<>(SISTE_PERIODEDAG_LØPENDE_BEHANDLING.minusWeeks(1), SISTE_PERIODEDAG_LØPENDE_BEHANDLING, Boolean.TRUE));
        when(familieHendelseRepository.hentAggregat(avsluttetBehMor.getId())).thenReturn(familieHendelseGrunnlagEntitet);
        when(familieHendelseRepository.hentAggregat(avsluttetBehMor.getId()).getGjeldendeVersjon()).thenReturn(familieHendelseEntitet);
        when(familieHendelseEntitet.getSkjæringstidspunkt()).thenReturn(DATO);

        //Behandling 2 på mor med nytt barn som overlapper behandling 1, og to tette
        var nyAvsBehandlingMor = lagBehandlingMor(DATO.plusWeeks(20), AKTØR_ID_MOR, null);
        when(stønadsperiodeTjeneste.stønadsperiodeStartdato(nyAvsBehandlingMor)).thenReturn(Optional.of(SISTE_PERIODEDAG_LØPENDE_BEHANDLING));
        when(stønadsperiodeTjeneste.utbetalingsTidslinjeEnkeltSak(nyAvsBehandlingMor))
            .thenReturn(new LocalDateTimeline<>(SISTE_PERIODEDAG_LØPENDE_BEHANDLING, SISTE_PERIODEDAG_LØPENDE_BEHANDLING.plusWeeks(1), Boolean.TRUE));
        when(familieHendelseRepository.hentAggregat(nyAvsBehandlingMor.getId())).thenReturn(familieHendelseGrunnlagEntitetAndreBarn);
        when(familieHendelseRepository.hentAggregat(nyAvsBehandlingMor.getId()).getGjeldendeVersjon()).thenReturn(familieHendelseEntitetAndreBarn);
        when(familieHendelseEntitetAndreBarn.getSkjæringstidspunkt()).thenReturn(DATO.plusWeeks(20));
        var skjæringstidspunkt = Skjæringstidspunkt.builder().medUtenMinsterett(false).build();
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(any())).thenReturn(skjæringstidspunkt);

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(nyAvsBehandlingMor);
        verifiserAtProsesstaskForHåndteringAvOpphørErOpprettetOgToTetteBeskrivelse(avsluttetBehMor.getFagsak());
    }

    @Test
    void skalIkkeFåTekstForToTetteNårUtenMinsterett() {
        var fødselDatoFørMinsterett = LocalDate.of(2022, 1, 3);
        var sisteDagAvsluttetBehandling = fødselDatoFørMinsterett.plusWeeks(50);
        //Behandling1 på mor før minsterett
        var avsluttetBehMor = lagBehandlingMor(fødselDatoFørMinsterett, AKTØR_ID_MOR, null);
        when(stønadsperiodeTjeneste.stønadsperiodeStartdato(avsluttetBehMor.getFagsak())).thenReturn(Optional.of(fødselDatoFørMinsterett.minusWeeks(3)));
        when(stønadsperiodeTjeneste.stønadsperiodeSluttdatoEnkeltSak(avsluttetBehMor.getFagsak())).thenReturn(Optional.of(
            fødselDatoFørMinsterett.plusWeeks(50)));
        when(familieHendelseRepository.hentAggregat(avsluttetBehMor.getId())).thenReturn(familieHendelseGrunnlagEntitet);
        when(familieHendelseRepository.hentAggregat(avsluttetBehMor.getId()).getGjeldendeVersjon()).thenReturn(familieHendelseEntitet);
        when(familieHendelseEntitet.getSkjæringstidspunkt()).thenReturn(fødselDatoFørMinsterett);

        //Behandling 2 på nytt barn som overlapper med behandling 1, og barnet er født innenfor 48 uker, og ingen minsterett
        var behandlingBarnToMedToTette = lagBehandlingMor(fødselDatoFørMinsterett.plusWeeks(20), AKTØR_ID_MOR, null);
        when(stønadsperiodeTjeneste.stønadsperiodeStartdato(behandlingBarnToMedToTette)).thenReturn(Optional.of(sisteDagAvsluttetBehandling));
        when(familieHendelseRepository.hentAggregat(behandlingBarnToMedToTette.getId())).thenReturn(familieHendelseGrunnlagEntitetAndreBarn);
        when(familieHendelseRepository.hentAggregat(behandlingBarnToMedToTette.getId()).getGjeldendeVersjon()).thenReturn(familieHendelseEntitetAndreBarn);
        when(familieHendelseEntitetAndreBarn.getSkjæringstidspunkt()).thenReturn(fødselDatoFørMinsterett.plusWeeks(20));
        //ingen minsterett
        var skjæringstidspunkt = Skjæringstidspunkt.builder().medUtenMinsterett(true).build();
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(avsluttetBehMor.getId())).thenReturn(skjæringstidspunkt);

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(behandlingBarnToMedToTette);
        var håndterOpphør = verifiserAtProsesstaskForHåndteringAvOpphørErOpprettet(1);
        assertThat(håndterOpphør.getFagsakId()).isEqualTo(avsluttetBehMor.getFagsak().getId());
        assertThat(håndterOpphør.getPropertyValue(HåndterOpphørAvYtelserTask.BESKRIVELSE_KEY)).isNull();
    }

    @Test
    void opphørSelvOmSkjæringstidspunktErNull() {
        //Behandling 1 mor
        var avsluttetBehMor = lagBehandlingMor(START_PERIODEDAG_LØPENDE_BEHANDLING, AKTØR_ID_MOR, null);
        when(stønadsperiodeTjeneste.stønadsperiodeStartdato(avsluttetBehMor.getFagsak())).thenReturn(Optional.of(START_PERIODEDAG_LØPENDE_BEHANDLING));
        when(stønadsperiodeTjeneste.stønadsperiodeSluttdatoEnkeltSak(avsluttetBehMor.getFagsak())).thenReturn(Optional.of(
            SISTE_PERIODEDAG_LØPENDE_BEHANDLING));
        lenient().when(familieHendelseRepository.hentAggregat(avsluttetBehMor.getId())).thenReturn(familieHendelseGrunnlagEntitet);
        lenient().when(familieHendelseRepository.hentAggregat(avsluttetBehMor.getId()).getGjeldendeVersjon()).thenReturn(familieHendelseEntitet);
        lenient().when(familieHendelseEntitet.getSkjæringstidspunkt()).thenReturn(null);
        //Behandling 2 mor overlapper behandling 1
        var nyAvsBehandlingMor = lagBehandlingMor(START_PERIODEDAG_OVERLAPP, AKTØR_ID_MOR, null);
        when(stønadsperiodeTjeneste.stønadsperiodeStartdato(nyAvsBehandlingMor)).thenReturn(Optional.of(SISTE_PERIODEDAG_LØPENDE_BEHANDLING));
        var skjæringstidspunkt = Skjæringstidspunkt.builder().medUtenMinsterett(false).build();
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(any())).thenReturn(skjæringstidspunkt);
        when(familieHendelseRepository.hentAggregat(nyAvsBehandlingMor.getId())).thenReturn(familieHendelseGrunnlagEntitet);
        when(familieHendelseRepository.hentAggregat(nyAvsBehandlingMor.getId()).getGjeldendeVersjon()).thenReturn(familieHendelseEntitet);
        when(familieHendelseEntitet.getSkjæringstidspunkt()).thenReturn(START_PERIODEDAG_LØPENDE_BEHANDLING);

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(nyAvsBehandlingMor);

        var håndterOpphør = verifiserAtProsesstaskForHåndteringAvOpphørErOpprettet(1);
        assertThat(håndterOpphør.getFagsakId()).isEqualTo(avsluttetBehMor.getFagsak().getId());
        assertThat(håndterOpphør.getPropertyValue(HåndterOpphørAvYtelserTask.BESKRIVELSE_KEY)).isNull();
    }

    @Test
    void opprettHåndteringNårOverlappMedFPNårInnvSVPPåSammeBarn() {
        var avsluttetFPBehMor = lagBehandlingMor(START_PERIODEDAG_LØPENDE_BEHANDLING, AKTØR_ID_MOR, null);
        when(stønadsperiodeTjeneste.utbetalingsperiodeEnkeltSak(avsluttetFPBehMor.getFagsak())).thenReturn(Optional.of(new LocalDateInterval(
            START_PERIODEDAG_LØPENDE_BEHANDLING,
            SISTE_PERIODEDAG_LØPENDE_BEHANDLING)));

        var nyBehSVPOverlapper = lagBehandlingSVP(AKTØR_ID_MOR);
        when(stønadsperiodeTjeneste.stønadsperiode(nyBehSVPOverlapper)).thenReturn(Optional.of(new LocalDateInterval(
            START_PERIODEDAG_LØPENDE_BEHANDLING.minusWeeks(3), START_PERIODEDAG_LØPENDE_BEHANDLING.plusDays(4))));

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(nyBehSVPOverlapper);

        var håndterOpphør = verifiserAtProsesstaskForHåndteringAvOpphørErOpprettet(1);
        assertThat(håndterOpphør.getFagsakId()).isEqualTo(nyBehSVPOverlapper.getFagsak().getId());
        assertThat(håndterOpphør.getPropertyValue(HåndterOpphørAvYtelserTask.BESKRIVELSE_KEY)).contains("Overlapp identifisert:");
    }

    @Test
    void overlappNårSVPInnvilgesForLøpendeFP() {
        var løpendeSVP = lagBehandlingSVP(AKTØR_ID_MOR);
        when(stønadsperiodeTjeneste.stønadsperiode(løpendeSVP)).thenReturn(Optional.of(new LocalDateInterval(START_PERIODEDAG_LØPENDE_BEHANDLING.minusWeeks(3), START_PERIODEDAG_LØPENDE_BEHANDLING.plusDays(4))));

        var starterFPSomOverlapperSVP = lagBehandlingMor(START_PERIODEDAG_LØPENDE_BEHANDLING, AKTØR_ID_MOR, null);
        when(stønadsperiodeTjeneste.utbetalingsperiodeEnkeltSak(starterFPSomOverlapperSVP.getFagsak())).thenReturn(Optional.of(new LocalDateInterval(
            START_PERIODEDAG_LØPENDE_BEHANDLING,
            SISTE_PERIODEDAG_LØPENDE_BEHANDLING)));

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(løpendeSVP);

        var håndterOpphør = verifiserAtProsesstaskForHåndteringAvOpphørErOpprettet(1);
        assertThat(håndterOpphør.getFagsakId()).isEqualTo(løpendeSVP.getFagsak().getId());
        assertThat(håndterOpphør.getPropertyValue(HåndterOpphørAvYtelserTask.BESKRIVELSE_KEY)).contains("Overlapp identifisert:");
    }

    @Test
    void identifiserOverlappNårInnvilgerFPOgHarLøpendeSVP() {
        var løpendeSVP = lagBehandlingSVP(AKTØR_ID_MOR);

        var starterFPSomOverlapperSVP = lagBehandlingMor(SISTE_PERIODEDAG_LØPENDE_BEHANDLING.minusDays(2), AKTØR_ID_MOR, null);

        when(stønadsperiodeTjeneste.stønadsperiodeStartdato(starterFPSomOverlapperSVP)).thenReturn(Optional.of(SISTE_PERIODEDAG_LØPENDE_BEHANDLING.minusDays(2)));
        when(stønadsperiodeTjeneste.stønadsperiodeStartdato(løpendeSVP.getFagsak())).thenReturn(Optional.of(START_PERIODEDAG_LØPENDE_BEHANDLING));
        when(stønadsperiodeTjeneste.stønadsperiodeSluttdatoEnkeltSak(løpendeSVP.getFagsak())).thenReturn(Optional.of(SISTE_PERIODEDAG_LØPENDE_BEHANDLING));

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(starterFPSomOverlapperSVP);

        var håndterOpphør = verifiserAtProsesstaskForHåndteringAvOpphørErOpprettet(1);
        assertThat(håndterOpphør.getFagsakId()).isEqualTo(løpendeSVP.getFagsakId());
        assertThat(håndterOpphør.getPropertyValue(HåndterOpphørAvYtelserTask.BESKRIVELSE_KEY)).isNull();
    }

    @Test
    void opprettHåndteringNårOverlappMedFPNårInnvilgerSVPPåNyttBarn() {
        var løpendeFPMor = lagBehandlingMor(START_PERIODEDAG_LØPENDE_BEHANDLING, AKTØR_ID_MOR, null);
        // PGA sjekk gradering
        when(stønadsperiodeTjeneste.utbetalingsperiodeEnkeltSak(løpendeFPMor.getFagsak())).thenReturn(Optional.of(new LocalDateInterval(START_PERIODEDAG_LØPENDE_BEHANDLING,SISTE_PERIODEDAG_LØPENDE_BEHANDLING)));
        lenient().when(stønadsperiodeTjeneste.fullUtbetalingSisteUtbetalingsperiode(løpendeFPMor.getFagsak())).thenReturn(true);

        var nySVPNyttBarnOverlapperFP = lagBehandlingSVP(AKTØR_ID_MOR);
        when(stønadsperiodeTjeneste.stønadsperiode(nySVPNyttBarnOverlapperFP)).thenReturn(Optional.of(new LocalDateInterval(START_PERIODEDAG_OVERLAPP, SISTE_PERIODEDAG_OVERLAPP)));

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(nySVPNyttBarnOverlapperFP);

        var håndterOpphør = verifiserAtProsesstaskForHåndteringAvOpphørErOpprettet(1);
        assertThat(håndterOpphør.getFagsakId()).isEqualTo(løpendeFPMor.getFagsak().getId());
        assertThat(håndterOpphør.getPropertyValue(HåndterOpphørAvYtelserTask.BESKRIVELSE_KEY)).contains("Overlapp identifisert:");
    }

    @Test
    void loggOverlappFPMedGraderingNårInnvilgerSVP() {
        //Har FP som overlapper med ny SVP sak og det er ikke gradering
        var avsluttetFPBehMor = lagBehandlingMor(START_PERIODEDAG_LØPENDE_BEHANDLING, AKTØR_ID_MOR, null);
        when(stønadsperiodeTjeneste.utbetalingsperiodeEnkeltSak(avsluttetFPBehMor.getFagsak())).thenReturn(Optional.of(new LocalDateInterval(
            START_PERIODEDAG_LØPENDE_BEHANDLING, START_PERIODEDAG_LØPENDE_BEHANDLING.plusWeeks(21))));
        when(stønadsperiodeTjeneste.fullUtbetalingSisteUtbetalingsperiode(avsluttetFPBehMor.getFagsak())).thenReturn(false);

        var nyBehSVPOverlapper = lagBehandlingSVP(AKTØR_ID_MOR);
        when(stønadsperiodeTjeneste.stønadsperiode(nyBehSVPOverlapper)).thenReturn(Optional.of(new LocalDateInterval(
            START_PERIODEDAG_LØPENDE_BEHANDLING.plusWeeks(18), START_PERIODEDAG_LØPENDE_BEHANDLING.plusWeeks(25))));

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(nyBehSVPOverlapper);

        verifiserAtProsesstaskForHåndteringAvOpphørIkkeErOpprettet();
    }

    @Test
    void opphørOverlappFPMedGraderingIPeriodenNårInnvilgerSVP() {
        var avsluttetFPBehMor = lagBehandlingMor(START_PERIODEDAG_LØPENDE_BEHANDLING, AKTØR_ID_MOR, null);
        when(stønadsperiodeTjeneste.utbetalingsperiodeEnkeltSak(avsluttetFPBehMor.getFagsak())).thenReturn(Optional.of(new LocalDateInterval(
            START_PERIODEDAG_LØPENDE_BEHANDLING.minusWeeks(6),
            SISTE_PERIODEDAG_LØPENDE_BEHANDLING)));
        when(stønadsperiodeTjeneste.fullUtbetalingSisteUtbetalingsperiode(avsluttetFPBehMor.getFagsak())).thenReturn(true);

        var nyBehSVPOverlapper = lagBehandlingSVP(AKTØR_ID_MOR);
        when(stønadsperiodeTjeneste.stønadsperiode(nyBehSVPOverlapper)).thenReturn(Optional.of(new LocalDateInterval(START_PERIODEDAG_OVERLAPP,
            SISTE_PERIODEDAG_OVERLAPP)));

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(nyBehSVPOverlapper);

        var håndterOpphør = verifiserAtProsesstaskForHåndteringAvOpphørErOpprettet(1);
        assertThat(håndterOpphør.getFagsakId()).isEqualTo(avsluttetFPBehMor.getFagsak().getId());
        assertThat(håndterOpphør.getPropertyValue(HåndterOpphørAvYtelserTask.BESKRIVELSE_KEY)).contains("Overlapp identifisert:");
    }

    @Test
    void loggOverlappSVPNårInnvilgerSVP() {
        var avslSVPBeh = lagBehandlingSVP(AKTØR_ID_MOR);
        when(stønadsperiodeTjeneste.utbetalingsperiodeEnkeltSak(avslSVPBeh.getFagsak())).thenReturn(Optional.of(new LocalDateInterval(
            SISTE_PERIODEDAG_LØPENDE_BEHANDLING.minusMonths(2), SISTE_PERIODEDAG_LØPENDE_BEHANDLING)));

        var nyBehSVPOverlapper = lagBehandlingSVP(AKTØR_ID_MOR);
        when(stønadsperiodeTjeneste.stønadsperiode(nyBehSVPOverlapper)).thenReturn(Optional.of(new LocalDateInterval(START_PERIODEDAG_OVERLAPP,
            SISTE_PERIODEDAG_OVERLAPP)));

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(nyBehSVPOverlapper);

        verifiserAtProsesstaskForHåndteringAvOpphørIkkeErOpprettet();
    }

    @Test
    void opprettIkkeRevNårOverlappMedFPNårInnvilgerSVPPåNyttBarn() {
        var fud = LocalDate.of(2021, 6, 4);
        var avsluttetFPBehMor = lagBehandlingMor(fud.plusWeeks(3), AKTØR_ID_MOR, null);
        when(stønadsperiodeTjeneste.utbetalingsperiodeEnkeltSak(avsluttetFPBehMor.getFagsak())).thenReturn(Optional.of(new LocalDateInterval(fud, fud.plusMonths(3))));

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
        scenarioAvsluttetBehMor.medBehandlingVedtak()
            .medVedtakstidspunkt(DATO.atStartOfDay().minusMonths(2))
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
        scenarioAvsluttetBehFar.medBehandlingVedtak()
            .medVedtakstidspunkt(DATO.atStartOfDay().minusMonths(2))
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
        scenario.medBehandlingVedtak()
            .medVedtakstidspunkt(DATO.atStartOfDay().minusMonths(2))
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
        scenario.medBehandlingVedtak()
            .medVedtakstidspunkt(DATO.atStartOfDay().minusMonths(2))
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
        scenarioAvslBeh.medSøknadHendelse().medTerminbekreftelse(scenarioAvslBeh.medSøknadHendelse().getTerminbekreftelseBuilder()
                .medNavnPå("LEGEN MIN")
                .medTermindato(START_PERIODEDAG_LØPENDE_BEHANDLING)
                .medUtstedtDato(DATO.minusDays(3)))
            .medAntallBarn(1);

        scenarioAvslBeh.medBruker(aktørId, NavBrukerKjønn.KVINNE)
            .medDefaultOppgittTilknytning();
        scenarioAvslBeh.medBehandlingsresultat(
            Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET));
        scenarioAvslBeh.medBehandlingVedtak()
            .medVedtakstidspunkt(DATO.atStartOfDay().minusMonths(1))
            .medVedtakResultatType(VedtakResultatType.INNVILGET);

        var behandlingSVP = scenarioAvslBeh.lagre(repositoryProvider);
        avsluttBehandlingOgFagsak(behandlingSVP);
        return behandlingSVP;
    }

    private void avsluttBehandlingOgFagsak(Behandling behandling) {
        behandling.avsluttBehandling();
        repositoryProvider.getBehandlingRepository()
            .lagre(behandling, repositoryProvider.getBehandlingRepository().taSkriveLås(behandling));
        fagsakRepository.oppdaterFagsakStatus(behandling.getFagsakId(), FagsakStatus.LØPENDE);
    }

    private ProsessTaskData verifiserAtProsesstaskForHåndteringAvOpphørErOpprettet(int times) {
        var captor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(taskTjeneste, times(times)).lagre(captor.capture());
        return captor.getAllValues()
            .stream()
            .filter(t -> t.taskType().equals(TaskType.forProsessTask(HåndterOpphørAvYtelserTask.class)))
            .findFirst()
            .orElse(null);
    }

    private void verifiserAtProsesstaskForHåndteringAvOpphørErOpprettetOgToTetteBeskrivelse(Fagsak fagsak) {
        var captor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(taskTjeneste, times(1)).lagre(captor.capture());
        var håndterOpphør = captor.getAllValues()
            .stream()
            .filter(t -> t.taskType().equals(TaskType.forProsessTask(HåndterOpphørAvYtelserTask.class)))
            .findFirst()
            .orElse(null);
        assertThat(håndterOpphør.getFagsakId()).isEqualTo(fagsak.getId());
        assertThat(håndterOpphør.getPropertyValue(HåndterOpphørAvYtelserTask.BESKRIVELSE_KEY)).contains("Overlapp på sak med minsterett ved tette fødsler identifisert");
    }

    private void verifiserAtProsesstaskForHåndteringAvOpphørIkkeErOpprettet() {
        verifyNoInteractions(taskTjeneste);
    }
}
