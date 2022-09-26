package no.nav.foreldrepenger.mottak.vedtak.overlapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
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
    private FamilieHendelseGrunnlagEntitet familieHendelseGrunnlagEntitet, familieHendelseGrunnlagEntitetAndreBarn;
    @Mock
    private FamilieHendelseEntitet familieHendelseEntitet, familieHendelseEntitetAndreBarn;


    @BeforeEach
    public void setUp() {
        var entityManager = getEntityManager();
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        fagsakRepository = new FagsakRepository(entityManager);
        vurderOpphørAvYtelser = new VurderOpphørAvYtelser(repositoryProvider, stønadsperiodeTjeneste, taskTjeneste, familieHendelseRepository);
    }

    @Test
    public void opphørLøpendeSakNårNySakOverlapperPåMor() {
        var avsluttetBehMor = lagBehandlingMor(FØDSELS_DATO_1, AKTØR_ID_MOR, null);
        when(stønadsperiodeTjeneste.stønadsperiodeSluttdatoEnkeltSak(avsluttetBehMor.getFagsak())).thenReturn(Optional.of(SISTE_DAG_MOR));

        var nyAvsBehandlingMor = lagBehandlingMor(SKJÆRINGSTIDSPUNKT_OVERLAPPER_BEH_1, AKTØR_ID_MOR, null);
        when(stønadsperiodeTjeneste.stønadsperiodeStartdato(nyAvsBehandlingMor)).thenReturn(Optional.of(SKJÆRINGSTIDSPUNKT_OVERLAPPER_BEH_1));

        when(familieHendelseRepository.hentAggregat(nyAvsBehandlingMor.getId())).thenReturn(familieHendelseGrunnlagEntitet);
        when(familieHendelseRepository.hentAggregat(nyAvsBehandlingMor.getId()).getGjeldendeVersjon()).thenReturn(familieHendelseEntitet);
        when(familieHendelseEntitet.getSkjæringstidspunkt()).thenReturn(FØDSELS_DATO_1.plusWeeks(60));

        when(familieHendelseRepository.hentAggregat(avsluttetBehMor.getId())).thenReturn(familieHendelseGrunnlagEntitet);
        when(familieHendelseRepository.hentAggregat(avsluttetBehMor.getId()).getGjeldendeVersjon()).thenReturn(familieHendelseEntitet);
        when(familieHendelseEntitet.getSkjæringstidspunkt()).thenReturn(FØDSELS_DATO_1);

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(nyAvsBehandlingMor);

        ProsessTaskData håndterOpphør = verifiserAtProsesstaskForHåndteringAvOpphørErOpprettet(avsluttetBehMor.getFagsak(), 1);
        assertThat(håndterOpphør.getFagsakId()).isEqualTo(avsluttetBehMor.getFagsak().getId());
        assertThat(håndterOpphør.getPropertyValue(HåndterOpphørAvYtelserTask.BESKRIVELSE_KEY)).isNull();
    }

    @Test
    public void opphørSakPåMorOgMedforelderNårNySakOverlapper() {
        var avsluttetBehMor = lagBehandlingMor(FØDSELS_DATO_1, AKTØR_ID_MOR, MEDF_AKTØR_ID);
        when(stønadsperiodeTjeneste.stønadsperiodeSluttdatoEnkeltSak(avsluttetBehMor.getFagsak())).thenReturn(Optional.of(SISTE_DAG_MOR));

        var nyAvsBehandlingMor = lagBehandlingMor(SKJÆRINGSTIDSPUNKT_OVERLAPPER_BEH_1, AKTØR_ID_MOR, MEDF_AKTØR_ID);
        when(stønadsperiodeTjeneste.stønadsperiodeStartdato(nyAvsBehandlingMor)).thenReturn(Optional.of(SKJÆRINGSTIDSPUNKT_OVERLAPPER_BEH_1));

        when(familieHendelseRepository.hentAggregat(nyAvsBehandlingMor.getId())).thenReturn(familieHendelseGrunnlagEntitet);
        when(familieHendelseRepository.hentAggregat(nyAvsBehandlingMor.getId()).getGjeldendeVersjon()).thenReturn(familieHendelseEntitet);
        when(familieHendelseEntitet.getSkjæringstidspunkt()).thenReturn(FØDSELS_DATO_1.plusWeeks(60));

        when(familieHendelseRepository.hentAggregat(avsluttetBehMor.getId())).thenReturn(familieHendelseGrunnlagEntitet);
        when(familieHendelseRepository.hentAggregat(avsluttetBehMor.getId()).getGjeldendeVersjon()).thenReturn(familieHendelseEntitet);
        when(familieHendelseEntitet.getSkjæringstidspunkt()).thenReturn(FØDSELS_DATO_1);

        var avslBehFarMedOverlappMor = lagBehandlingFar(FØDSELS_DATO_1, MEDF_AKTØR_ID, AKTØR_ID_MOR);
        when(stønadsperiodeTjeneste.stønadsperiodeSluttdatoEnkeltSak(avslBehFarMedOverlappMor.getFagsak())).thenReturn(Optional.of(SISTE_DAG_PER_OVERLAPP));

        when(familieHendelseRepository.hentAggregat(avslBehFarMedOverlappMor.getId())).thenReturn(familieHendelseGrunnlagEntitet);
        when(familieHendelseRepository.hentAggregat(avslBehFarMedOverlappMor.getId()).getGjeldendeVersjon()).thenReturn(familieHendelseEntitet);
        when(familieHendelseEntitet.getSkjæringstidspunkt()).thenReturn(FØDSELS_DATO_1);

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(nyAvsBehandlingMor);

        ProsessTaskData håndterOpphør = verifiserAtProsesstaskForHåndteringAvOpphørErOpprettet(avsluttetBehMor.getFagsak(), 2);
        assertThat(håndterOpphør.getFagsakId()).isEqualTo(avsluttetBehMor.getFagsak().getId());
        assertThat(håndterOpphør.getPropertyValue(HåndterOpphørAvYtelserTask.BESKRIVELSE_KEY)).isNull();
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

        when(familieHendelseRepository.hentAggregat(adopsjonFarLop.getId())).thenReturn(familieHendelseGrunnlagEntitet);
        when(familieHendelseRepository.hentAggregat(adopsjonFarLop.getId()).getGjeldendeVersjon()).thenReturn(familieHendelseEntitet);
        when(familieHendelseEntitet.getSkjæringstidspunkt()).thenReturn(omsorgsovertakelsedato);

        when(familieHendelseRepository.hentAggregat(morAdopsjonIVB.getId())).thenReturn(familieHendelseGrunnlagEntitet);
        when(familieHendelseRepository.hentAggregat(morAdopsjonIVB.getId()).getGjeldendeVersjon()).thenReturn(familieHendelseEntitet);
        when(familieHendelseEntitet.getSkjæringstidspunkt()).thenReturn(omsorgsovertakelsedato2);

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(morAdopsjonIVB);

        ProsessTaskData håndterOpphør = verifiserAtProsesstaskForHåndteringAvOpphørErOpprettet(adopsjonFarLop.getFagsak(), 1);
        assertThat(håndterOpphør.getFagsakId()).isEqualTo(adopsjonFarLop.getFagsak().getId());
        assertThat(håndterOpphør.getPropertyValue(HåndterOpphørAvYtelserTask.BESKRIVELSE_KEY)).isNull();

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

        when(familieHendelseRepository.hentAggregat(nyBehMorSomIkkeOverlapper.getId())).thenReturn(familieHendelseGrunnlagEntitet);
        when(familieHendelseRepository.hentAggregat(nyBehMorSomIkkeOverlapper.getId()).getGjeldendeVersjon()).thenReturn(familieHendelseEntitet);
        when(familieHendelseEntitet.getSkjæringstidspunkt()).thenReturn(FØDSELS_DATO_1.plusWeeks(60));

        when(familieHendelseRepository.hentAggregat(avslBehFarMedOverlappMor.getId())).thenReturn(familieHendelseGrunnlagEntitet);
        when(familieHendelseRepository.hentAggregat(avslBehFarMedOverlappMor.getId()).getGjeldendeVersjon()).thenReturn(familieHendelseEntitet);
        when(familieHendelseEntitet.getSkjæringstidspunkt()).thenReturn(FØDSELS_DATO_1);

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(nyBehMorSomIkkeOverlapper);

        ProsessTaskData håndterOpphør = verifiserAtProsesstaskForHåndteringAvOpphørErOpprettet(avslBehFarMedOverlappMor.getFagsak(), 1);
        assertThat(håndterOpphør.getFagsakId()).isEqualTo(avslBehFarMedOverlappMor.getFagsak().getId());
        assertThat(håndterOpphør.getPropertyValue(HåndterOpphørAvYtelserTask.BESKRIVELSE_KEY)).isNull();

    }

    @Test
    public void opphørSakPåFarNårNySakPåFarOverlapper() {
        var avslBehFar = lagBehandlingFar(FØDSELS_DATO_1, MEDF_AKTØR_ID, AKTØR_ID_MOR);
        when(stønadsperiodeTjeneste.stønadsperiodeSluttdatoEnkeltSak(avslBehFar.getFagsak())).thenReturn(Optional.of(SISTE_DAG_MOR));

        var nyBehFar = lagBehandlingFar(SKJÆRINGSTIDSPUNKT_OVERLAPPER_BEH_1, MEDF_AKTØR_ID, AKTØR_ID_MOR);
        when(stønadsperiodeTjeneste.stønadsperiodeStartdato(nyBehFar)).thenReturn(Optional.of(SKJÆRINGSTIDSPUNKT_OVERLAPPER_BEH_1));

        lenient().when(familieHendelseRepository.hentAggregat(avslBehFar.getId())).thenReturn(familieHendelseGrunnlagEntitet);
        lenient().when(familieHendelseRepository.hentAggregat(avslBehFar.getId()).getGjeldendeVersjon()).thenReturn(familieHendelseEntitet);
        lenient().when(familieHendelseEntitet.getSkjæringstidspunkt()).thenReturn(FØDSELS_DATO_1.plusWeeks(60));

        when(familieHendelseRepository.hentAggregat(nyBehFar.getId())).thenReturn(familieHendelseGrunnlagEntitet);
        when(familieHendelseRepository.hentAggregat(nyBehFar.getId()).getGjeldendeVersjon()).thenReturn(familieHendelseEntitet);
        when(familieHendelseEntitet.getSkjæringstidspunkt()).thenReturn(FØDSELS_DATO_1);

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(nyBehFar);

        ProsessTaskData håndterOpphør = verifiserAtProsesstaskForHåndteringAvOpphørErOpprettet(avslBehFar.getFagsak(), 1);
        assertThat(håndterOpphør.getFagsakId()).isEqualTo(avslBehFar.getFagsak().getId());
        assertThat(håndterOpphør.getPropertyValue(HåndterOpphørAvYtelserTask.BESKRIVELSE_KEY)).isNull();

    }

    @Test
    public void opphørSakPåMorNårSisteUttakLikStartPåNyttUttak() {
        var avsluttetBehMor = lagBehandlingMor(FØDSELS_DATO_1, AKTØR_ID_MOR, null);
        when(stønadsperiodeTjeneste.stønadsperiodeSluttdatoEnkeltSak(avsluttetBehMor.getFagsak())).thenReturn(Optional.of(SISTE_DAG_MOR));

        var nyAvsBehandlingMor = lagBehandlingMor(SKJÆRINGSTIDSPUNKT_OVERLAPPER_BEH_1, AKTØR_ID_MOR, null);
        when(stønadsperiodeTjeneste.stønadsperiodeStartdato(nyAvsBehandlingMor)).thenReturn(Optional.of(SISTE_DAG_MOR));


        lenient().when(familieHendelseRepository.hentAggregat(avsluttetBehMor.getId())).thenReturn(familieHendelseGrunnlagEntitet);
        lenient().when(familieHendelseRepository.hentAggregat(avsluttetBehMor.getId()).getGjeldendeVersjon()).thenReturn(familieHendelseEntitet);
        lenient().when(familieHendelseEntitet.getSkjæringstidspunkt()).thenReturn(FØDSELS_DATO_1);

        when(familieHendelseRepository.hentAggregat(nyAvsBehandlingMor.getId())).thenReturn(familieHendelseGrunnlagEntitet);
        when(familieHendelseRepository.hentAggregat(nyAvsBehandlingMor.getId()).getGjeldendeVersjon()).thenReturn(familieHendelseEntitet);
        when(familieHendelseEntitet.getSkjæringstidspunkt()).thenReturn(FØDSELS_DATO_1);

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(nyAvsBehandlingMor);

        ProsessTaskData håndterOpphør = verifiserAtProsesstaskForHåndteringAvOpphørErOpprettet(avsluttetBehMor.getFagsak(), 1);
        assertThat(håndterOpphør.getFagsakId()).isEqualTo(avsluttetBehMor.getFagsak().getId());
        assertThat(håndterOpphør.getPropertyValue(HåndterOpphørAvYtelserTask.BESKRIVELSE_KEY)).isNull();
    }

    @Test
    public void opphørSakPåMorNårToTette() {
        var avsluttetBehMor = lagBehandlingMor(LocalDate.now(), AKTØR_ID_MOR, null);
        when(stønadsperiodeTjeneste.stønadsperiodeSluttdatoEnkeltSak(avsluttetBehMor.getFagsak())).thenReturn(Optional.of(SISTE_DAG_MOR));

        var nyAvsBehandlingMor = lagBehandlingMor(LocalDate.now().plusWeeks(20), AKTØR_ID_MOR, null);
        when(stønadsperiodeTjeneste.stønadsperiodeStartdato(nyAvsBehandlingMor)).thenReturn(Optional.of(SISTE_DAG_MOR));

        //første barn
        when(familieHendelseRepository.hentAggregat(avsluttetBehMor.getId())).thenReturn(familieHendelseGrunnlagEntitet);
        when(familieHendelseRepository.hentAggregat(avsluttetBehMor.getId()).getGjeldendeVersjon()).thenReturn(familieHendelseEntitet);
        when(familieHendelseEntitet.getSkjæringstidspunkt()).thenReturn(LocalDate.now());
        //andre barn
        when(familieHendelseRepository.hentAggregat(nyAvsBehandlingMor.getId())).thenReturn(familieHendelseGrunnlagEntitetAndreBarn);
        when(familieHendelseRepository.hentAggregat(nyAvsBehandlingMor.getId()).getGjeldendeVersjon()).thenReturn(familieHendelseEntitetAndreBarn);
        when(familieHendelseEntitetAndreBarn.getSkjæringstidspunkt()).thenReturn(LocalDate.now().plusWeeks(20));

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(nyAvsBehandlingMor);
        verifiserAtProsesstaskForHåndteringAvOpphørErOpprettetOgToTetteBeskrivelse(avsluttetBehMor.getFagsak());
    }

    @Test
    public void opphørSelvOmSkjæringstidspunktErNull() {
        var avsluttetBehMor = lagBehandlingMor(FØDSELS_DATO_1, AKTØR_ID_MOR, null);
        when(stønadsperiodeTjeneste.stønadsperiodeSluttdatoEnkeltSak(avsluttetBehMor.getFagsak())).thenReturn(Optional.of(SISTE_DAG_MOR));

        var nyAvsBehandlingMor = lagBehandlingMor(SKJÆRINGSTIDSPUNKT_OVERLAPPER_BEH_1, AKTØR_ID_MOR, null);
        when(stønadsperiodeTjeneste.stønadsperiodeStartdato(nyAvsBehandlingMor)).thenReturn(Optional.of(SISTE_DAG_MOR));


        lenient().when(familieHendelseRepository.hentAggregat(avsluttetBehMor.getId())).thenReturn(familieHendelseGrunnlagEntitet);
        lenient().when(familieHendelseRepository.hentAggregat(avsluttetBehMor.getId()).getGjeldendeVersjon()).thenReturn(familieHendelseEntitet);
        lenient().when(familieHendelseEntitet.getSkjæringstidspunkt()).thenReturn(null);

        when(familieHendelseRepository.hentAggregat(nyAvsBehandlingMor.getId())).thenReturn(familieHendelseGrunnlagEntitet);
        when(familieHendelseRepository.hentAggregat(nyAvsBehandlingMor.getId()).getGjeldendeVersjon()).thenReturn(familieHendelseEntitet);
        when(familieHendelseEntitet.getSkjæringstidspunkt()).thenReturn(FØDSELS_DATO_1);

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(nyAvsBehandlingMor);

        ProsessTaskData håndterOpphør = verifiserAtProsesstaskForHåndteringAvOpphørErOpprettet(avsluttetBehMor.getFagsak(), 1);
        assertThat(håndterOpphør.getFagsakId()).isEqualTo(avsluttetBehMor.getFagsak().getId());
        assertThat(håndterOpphør.getPropertyValue(HåndterOpphørAvYtelserTask.BESKRIVELSE_KEY)).isNull();
    }

    @Test
    public void opprettHåndteringNårOverlappMedFPNårInnvSVPPåSammeBarn() {
        var avsluttetFPBehMor = lagBehandlingMor(FØDSELS_DATO_1, AKTØR_ID_MOR, null);
        when(stønadsperiodeTjeneste.utbetalingsperiodeEnkeltSak(avsluttetFPBehMor.getFagsak())).thenReturn(Optional.of(new LocalDateInterval(FØDSELS_DATO_1, SISTE_DAG_MOR)));

        var nyBehSVPOverlapper = lagBehandlingSVP(AKTØR_ID_MOR);
        when(stønadsperiodeTjeneste.stønadsperiode(nyBehSVPOverlapper)).thenReturn(Optional.of(new LocalDateInterval(FØDSELS_DATO_1.minusWeeks(3), FØDSELS_DATO_1.plusDays(4))));

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(nyBehSVPOverlapper);

        ProsessTaskData håndterOpphør = verifiserAtProsesstaskForHåndteringAvOpphørErOpprettet(nyBehSVPOverlapper.getFagsak(), 1);
        assertThat(håndterOpphør.getFagsakId()).isEqualTo(nyBehSVPOverlapper.getFagsak().getId());
        assertThat(håndterOpphør.getPropertyValue(HåndterOpphørAvYtelserTask.BESKRIVELSE_KEY)).contains("Overlapp identifisert:");
    }

    @Test
    public void overlappNårSVPInnvilgesForLøpendeFP() {
        var løpendeSVP = lagBehandlingSVP(AKTØR_ID_MOR);
        when(stønadsperiodeTjeneste.stønadsperiode(løpendeSVP)).thenReturn(Optional.of(new LocalDateInterval(FØDSELS_DATO_1.minusWeeks(3), FØDSELS_DATO_1.plusDays(4))));

        var starterFPSomOverlapperSVP = lagBehandlingMor(FØDSELS_DATO_1, AKTØR_ID_MOR, null);
        when(stønadsperiodeTjeneste.utbetalingsperiodeEnkeltSak(starterFPSomOverlapperSVP.getFagsak())).thenReturn(Optional.of(new LocalDateInterval(FØDSELS_DATO_1, SISTE_DAG_MOR)));

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(løpendeSVP);

        ProsessTaskData håndterOpphør = verifiserAtProsesstaskForHåndteringAvOpphørErOpprettet(løpendeSVP.getFagsak(), 1);
        assertThat(håndterOpphør.getFagsakId()).isEqualTo(løpendeSVP.getFagsak().getId());
        assertThat(håndterOpphør.getPropertyValue(HåndterOpphørAvYtelserTask.BESKRIVELSE_KEY)).contains("Overlapp identifisert:");
    }

    @Test
    public void opprettHåndteringNårOverlappMedFPNårInnvilgerSVPPåNyttBarn() {
        var avsluttetFPBehMor = lagBehandlingMor(FØDSELS_DATO_1, AKTØR_ID_MOR, null);
        // PGA sjekk gradering
        when(stønadsperiodeTjeneste.utbetalingsperiodeEnkeltSak(avsluttetFPBehMor.getFagsak())).thenReturn(Optional.of(new LocalDateInterval(FØDSELS_DATO_1, SISTE_DAG_MOR)));
        when(stønadsperiodeTjeneste.fullUtbetalingSisteUtbetalingsperiode(avsluttetFPBehMor.getFagsak())).thenReturn(true);

        var nyBehSVPOverlapper = lagBehandlingSVP(AKTØR_ID_MOR);
        when(stønadsperiodeTjeneste.stønadsperiode(nyBehSVPOverlapper)).thenReturn(Optional.of(new LocalDateInterval(SKJÆRINGSTIDSPUNKT_OVERLAPPER_BEH_1, SISTE_DAG_PER_OVERLAPP)));

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(nyBehSVPOverlapper);

        ProsessTaskData håndterOpphør = verifiserAtProsesstaskForHåndteringAvOpphørErOpprettet(avsluttetFPBehMor.getFagsak(), 1);
        assertThat(håndterOpphør.getFagsakId()).isEqualTo(avsluttetFPBehMor.getFagsak().getId());
        assertThat(håndterOpphør.getPropertyValue(HåndterOpphørAvYtelserTask.BESKRIVELSE_KEY)).contains("Overlapp identifisert:");
    }

    @Test
    public void loggOverlappFPMedGraderingNårInnvilgerSVP() {
        //Har FP som overlapper med ny SVP sak og det er ikke gradering
        var avsluttetFPBehMor = lagBehandlingMor(FØDSELS_DATO_1, AKTØR_ID_MOR, null);
        when(stønadsperiodeTjeneste.utbetalingsperiodeEnkeltSak(avsluttetFPBehMor.getFagsak())).thenReturn(Optional.of(new LocalDateInterval(FØDSELS_DATO_1, FØDSELS_DATO_1.plusWeeks(21))));
        when(stønadsperiodeTjeneste.fullUtbetalingSisteUtbetalingsperiode(avsluttetFPBehMor.getFagsak())).thenReturn(false);

        var nyBehSVPOverlapper = lagBehandlingSVP(AKTØR_ID_MOR);
        when(stønadsperiodeTjeneste.stønadsperiode(nyBehSVPOverlapper)).thenReturn(Optional.of(new LocalDateInterval(FØDSELS_DATO_1.plusWeeks(18), FØDSELS_DATO_1.plusWeeks(25))));

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(nyBehSVPOverlapper);

        verifiserAtProsesstaskForHåndteringAvOpphørIkkeErOpprettet();
    }

    @Test
    public void opphørOverlappFPMedGraderingIPeriodenNårInnvilgerSVP() {
        var avsluttetFPBehMor = lagBehandlingMor(FØDSELS_DATO_1, AKTØR_ID_MOR, null);
        when(stønadsperiodeTjeneste.utbetalingsperiodeEnkeltSak(avsluttetFPBehMor.getFagsak())).thenReturn(Optional.of(new LocalDateInterval(FØDSELS_DATO_1.minusWeeks(6), SISTE_DAG_MOR)));
        when(stønadsperiodeTjeneste.fullUtbetalingSisteUtbetalingsperiode(avsluttetFPBehMor.getFagsak())).thenReturn(true);

        var nyBehSVPOverlapper = lagBehandlingSVP(AKTØR_ID_MOR);
        when(stønadsperiodeTjeneste.stønadsperiode(nyBehSVPOverlapper)).thenReturn(Optional.of(new LocalDateInterval(SKJÆRINGSTIDSPUNKT_OVERLAPPER_BEH_1, SISTE_DAG_PER_OVERLAPP)));

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(nyBehSVPOverlapper);

        ProsessTaskData håndterOpphør = verifiserAtProsesstaskForHåndteringAvOpphørErOpprettet(avsluttetFPBehMor.getFagsak(), 1);
        assertThat(håndterOpphør.getFagsakId()).isEqualTo(avsluttetFPBehMor.getFagsak().getId());
        assertThat(håndterOpphør.getPropertyValue(HåndterOpphørAvYtelserTask.BESKRIVELSE_KEY)).contains("Overlapp identifisert:");
    }

    @Test
    public void loggOverlappSVPNårInnvilgerSVP() {
        var avslSVPBeh = lagBehandlingSVP(AKTØR_ID_MOR);
        when(stønadsperiodeTjeneste.utbetalingsperiodeEnkeltSak(avslSVPBeh.getFagsak())).thenReturn(Optional.of(new LocalDateInterval(SISTE_DAG_MOR.minusMonths(2), SISTE_DAG_MOR)));

        var nyBehSVPOverlapper = lagBehandlingSVP(AKTØR_ID_MOR);
        when(stønadsperiodeTjeneste.stønadsperiode(nyBehSVPOverlapper)).thenReturn(Optional.of(new LocalDateInterval(SKJÆRINGSTIDSPUNKT_OVERLAPPER_BEH_1, SISTE_DAG_PER_OVERLAPP)));

        vurderOpphørAvYtelser.vurderOpphørAvYtelser(nyBehSVPOverlapper);

        verifiserAtProsesstaskForHåndteringAvOpphørIkkeErOpprettet();
    }

    @Test
    public void opprettIkkeRevNårOverlappMedFPNårInnvilgerSVPPåNyttBarn() {
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
        scenarioAvslBeh.medSøknadHendelse().medTerminbekreftelse(scenarioAvslBeh.medSøknadHendelse().getTerminbekreftelseBuilder()
                .medNavnPå("LEGEN MIN")
                .medTermindato(FØDSELS_DATO_1)
                .medUtstedtDato(LocalDate.now().minusDays(3)))
            .medAntallBarn(1);

        scenarioAvslBeh.medBruker(aktørId, NavBrukerKjønn.KVINNE)
            .medDefaultOppgittTilknytning();
        scenarioAvslBeh.medBehandlingsresultat(
            Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET));
        scenarioAvslBeh.medVilkårResultatType(VilkårResultatType.INNVILGET);
        scenarioAvslBeh.medBehandlingVedtak()
            .medVedtakstidspunkt(LocalDateTime.now().minusMonths(1))
            .medVedtakResultatType(VedtakResultatType.INNVILGET);;

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

    private ProsessTaskData verifiserAtProsesstaskForHåndteringAvOpphørErOpprettet(Fagsak fagsak, int times) {
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
        assertThat(håndterOpphør.getPropertyValue(HåndterOpphørAvYtelserTask.BESKRIVELSE_KEY)).contains("Overlapp på sak med minsterett(to tette) identifisert");
    }

    private void verifiserAtProsesstaskForHåndteringAvOpphørIkkeErOpprettet() {
        verifyNoInteractions(taskTjeneste);
    }
}
