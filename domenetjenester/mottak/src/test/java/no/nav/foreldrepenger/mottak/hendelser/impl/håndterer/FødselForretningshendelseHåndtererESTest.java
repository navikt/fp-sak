package no.nav.foreldrepenger.mottak.hendelser.impl.håndterer;

import static no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType.RE_HENDELSE_FØDSEL;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import no.nav.foreldrepenger.behandling.revurdering.etterkontroll.EtterkontrollRepository;
import no.nav.foreldrepenger.behandling.revurdering.etterkontroll.KontrollType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSats;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSatsType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregning;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.hendelser.ForretningshendelseType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.mottak.Behandlingsoppretter;
import no.nav.foreldrepenger.mottak.dokumentmottak.HistorikkinnslagTjeneste;
import no.nav.foreldrepenger.mottak.dokumentmottak.impl.Kompletthetskontroller;
import no.nav.foreldrepenger.mottak.dokumentmottak.impl.KøKontroller;
import no.nav.foreldrepenger.mottak.hendelser.es.FødselForretningshendelseHåndtererImpl;
import no.nav.foreldrepenger.mottak.hendelser.håndterer.ForretningshendelseHåndtererFelles;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.es.SkjæringstidspunktTjenesteImpl;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class FødselForretningshendelseHåndtererESTest {

    private static final BeregningSats GJELDENDE_SATS = new BeregningSats(BeregningSatsType.ENGANG,
        DatoIntervallEntitet.fraOgMed(LocalDate.now().minusMonths(1)), 90000L);

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    @Inject
    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());

    @Inject
    private BehandlingRepository behandlingRepository;

    private ForretningshendelseHåndtererFelles håndtererFelles;
    private FødselForretningshendelseHåndtererImpl håndterer;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private Kompletthetskontroller kompletthetskontroller = mock(Kompletthetskontroller.class);

    @Mock
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;

    private Behandling behandling;
    @Mock
    private HistorikkinnslagTjeneste historikkinnslagTjeneste;

    @Mock
    private KøKontroller køKontroller;
    @Mock
    private LegacyESBeregningRepository beregningRepository;
    @Mock
    private EtterkontrollRepository etterkontrollRepository;
    @Mock
    private Behandlingsoppretter behandlingsoppretter;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        skjæringstidspunktTjeneste = new SkjæringstidspunktTjenesteImpl(repositoryProvider, null);
        håndtererFelles = new ForretningshendelseHåndtererFelles(historikkinnslagTjeneste, kompletthetskontroller,
            behandlingProsesseringTjeneste, behandlingsoppretter, køKontroller);
        håndterer = new FødselForretningshendelseHåndtererImpl(håndtererFelles, Period.ofWeeks(11), skjæringstidspunktTjeneste, etterkontrollRepository, beregningRepository);
    }

    @Test
    public void skal_ta_av_vent_når_hendelse_er_fødsel_mangler_registrering() {
        // Arrange
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse().medFødselsDato(LocalDate.now().minusDays(2)).medAntallBarn(1);
        scenario.medBehandlingStegStart(BehandlingStegType.SØKERS_RELASJON_TIL_BARN);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.AUTO_VENT_PÅ_FØDSELREGISTRERING, BehandlingStegType.KONTROLLER_FAKTA);
        behandling = scenario.lagre(repositoryProvider);

        behandling = behandlingRepository.hentBehandling(behandling.getId());

        // Act
        håndterer.håndterÅpenBehandling(behandling, RE_HENDELSE_FØDSEL);

        // Assert
        verify(kompletthetskontroller).vurderNyForretningshendelse(eq(behandling));
    }

    @Test
    public void skal_opprette_revurdering_ved_ulik_sats() {
        // Arrange
        var sats = GJELDENDE_SATS.getVerdi() - 1000;
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse().medFødselsDato(LocalDate.now().minusDays(2)).medAntallBarn(1);
        scenario.medBehandlingStegStart(BehandlingStegType.SØKERS_RELASJON_TIL_BARN);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.AUTO_VENT_PÅ_FØDSELREGISTRERING, BehandlingStegType.KONTROLLER_FAKTA);
        behandling = scenario.lagre(repositoryProvider);
        behandling.avsluttBehandling();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        behandling = behandlingRepository.hentBehandling(behandling.getId());
        var beregning = mock(LegacyESBeregning.class);
        when(beregning.getSatsVerdi()).thenReturn(sats);
        when(beregningRepository.getSisteBeregning(anyLong())).thenReturn(Optional.of(beregning));
        when(beregningRepository.finnEksaktSats(any(), any())).thenReturn(GJELDENDE_SATS);

        // Act
        håndterer.håndterAvsluttetBehandling(behandling, ForretningshendelseType.FØDSEL, RE_HENDELSE_FØDSEL);

        // Assert
        verify(behandlingsoppretter).opprettRevurdering(any(), any());
        verify(beregningRepository).getSisteBeregning(eq(behandling.getId()));
    }

    @Test
    public void skal_ikke_opprette_revurdering_ved_samme_sats() {
        // Arrange
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse().medFødselsDato(LocalDate.now().minusDays(2)).medAntallBarn(1);
        scenario.medBehandlingStegStart(BehandlingStegType.SØKERS_RELASJON_TIL_BARN);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.AUTO_VENT_PÅ_FØDSELREGISTRERING, BehandlingStegType.KONTROLLER_FAKTA);
        behandling = scenario.lagre(repositoryProvider);
        behandling.avsluttBehandling();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        behandling = behandlingRepository.hentBehandling(behandling.getId());
        var beregning = mock(LegacyESBeregning.class);
        when(beregning.getSatsVerdi()).thenReturn(GJELDENDE_SATS.getVerdi());
        when(beregningRepository.getSisteBeregning(anyLong())).thenReturn(Optional.of(beregning));
        when(beregningRepository.finnEksaktSats(any(), any())).thenReturn(GJELDENDE_SATS);

        // Act
        håndterer.håndterAvsluttetBehandling(behandling, ForretningshendelseType.FØDSEL, RE_HENDELSE_FØDSEL);

        // Assert
        verify(etterkontrollRepository).avflaggDersomEksisterer(eq(behandling.getFagsakId()), eq(KontrollType.MANGLENDE_FØDSEL));
        verify(beregningRepository).getSisteBeregning(eq(behandling.getId()));
    }

    @Test
    public void skal_opprette_revurdering_ved_opprinnelig_avslag() {
        // Arrange
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse().medFødselsDato(LocalDate.now().minusDays(2)).medAntallBarn(1);
        scenario.medBehandlingStegStart(BehandlingStegType.SØKERS_RELASJON_TIL_BARN);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.AUTO_VENT_PÅ_FØDSELREGISTRERING, BehandlingStegType.KONTROLLER_FAKTA);
        behandling = scenario.lagre(repositoryProvider);
        behandling.avsluttBehandling();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        behandling = behandlingRepository.hentBehandling(behandling.getId());
        when(beregningRepository.getSisteBeregning(anyLong())).thenReturn(Optional.empty());
        when(beregningRepository.finnEksaktSats(any(), any())).thenReturn(GJELDENDE_SATS);

        // Act
        håndterer.håndterAvsluttetBehandling(behandling, ForretningshendelseType.FØDSEL, RE_HENDELSE_FØDSEL);

        // Assert
        verify(behandlingsoppretter).opprettRevurdering(any(), any());
        verify(beregningRepository).getSisteBeregning(eq(behandling.getId()));
    }

    @Test
    public void skal_ikke_opprette_revurdering_ved_sen_registrering() {
        // Arrange
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse().medFødselsDato(LocalDate.now().minusMonths(6)).medAntallBarn(1);
        scenario.medBehandlingStegStart(BehandlingStegType.SØKERS_RELASJON_TIL_BARN);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.AUTO_VENT_PÅ_FØDSELREGISTRERING, BehandlingStegType.KONTROLLER_FAKTA);
        behandling = scenario.lagre(repositoryProvider);
        behandling.avsluttBehandling();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        behandling = behandlingRepository.hentBehandling(behandling.getId());

        // Act
        håndterer.håndterAvsluttetBehandling(behandling, ForretningshendelseType.FØDSEL, RE_HENDELSE_FØDSEL);

        // Assert
        verify(etterkontrollRepository).avflaggDersomEksisterer(eq(behandling.getFagsakId()), eq(KontrollType.MANGLENDE_FØDSEL));
        verifyNoInteractions(beregningRepository);
    }
}
