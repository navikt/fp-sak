package no.nav.foreldrepenger.mottak.hendelser.impl.håndterer;

import static no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType.RE_HENDELSE_FØDSEL;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.Period;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningRepository;
import no.nav.foreldrepenger.behandlingslager.hendelser.ForretningshendelseType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.mottak.Behandlingsoppretter;
import no.nav.foreldrepenger.mottak.dokumentmottak.HistorikkinnslagTjeneste;
import no.nav.foreldrepenger.mottak.dokumentmottak.impl.Kompletthetskontroller;
import no.nav.foreldrepenger.mottak.hendelser.es.FødselForretningshendelseHåndtererImpl;
import no.nav.foreldrepenger.mottak.hendelser.håndterer.ForretningshendelseHåndtererFelles;
import no.nav.foreldrepenger.mottak.sakskompleks.KøKontroller;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@ExtendWith(MockitoExtension.class)
public class FødselForretningshendelseHåndtererESTest {

    private ForretningshendelseHåndtererFelles håndtererFelles;
    private FødselForretningshendelseHåndtererImpl håndterer;
    @Mock
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
    private Behandlingsoppretter behandlingsoppretter;

    @BeforeEach
    public void setUp() {
        håndtererFelles = new ForretningshendelseHåndtererFelles(historikkinnslagTjeneste, kompletthetskontroller,
            behandlingProsesseringTjeneste, behandlingsoppretter, køKontroller);
        håndterer = new FødselForretningshendelseHåndtererImpl(håndtererFelles, Period.ofDays(60), skjæringstidspunktTjeneste, beregningRepository);
    }

    @Test
    public void skal_ta_av_vent_når_hendelse_er_fødsel_mangler_registrering() {
        // Arrange
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse().medFødselsDato(LocalDate.now().minusDays(2)).medAntallBarn(1);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.AUTO_VENT_PÅ_FØDSELREGISTRERING, BehandlingStegType.KONTROLLER_FAKTA);
        behandling = scenario.lagMocked();

        // Act
        håndterer.håndterÅpenBehandling(behandling, RE_HENDELSE_FØDSEL);

        // Assert
        verify(kompletthetskontroller).vurderNyForretningshendelse(eq(behandling));
    }

    @Test
    public void skal_opprette_revurdering_ved_ulik_sats() {
        // Arrange
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse().medFødselsDato(LocalDate.now().minusDays(2)).medAntallBarn(1);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.AUTO_VENT_PÅ_FØDSELREGISTRERING, BehandlingStegType.KONTROLLER_FAKTA);
        behandling = scenario.lagMocked();
        behandling.avsluttBehandling();
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(any())).thenReturn(Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(LocalDate.now()).build());
        when(beregningRepository.skalReberegne(anyLong(), any())).thenReturn(Boolean.TRUE);

        // Act
        håndterer.håndterAvsluttetBehandling(behandling, ForretningshendelseType.FØDSEL, RE_HENDELSE_FØDSEL);

        // Assert
        verify(behandlingsoppretter).opprettRevurdering(any(), any());
        verify(beregningRepository).skalReberegne(eq(behandling.getId()), any());
    }

    @Test
    public void skal_ikke_opprette_revurdering_ved_samme_sats() {
        // Arrange
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse().medFødselsDato(LocalDate.now().minusDays(2)).medAntallBarn(1);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.AUTO_VENT_PÅ_FØDSELREGISTRERING, BehandlingStegType.KONTROLLER_FAKTA);
        behandling = scenario.lagMocked();
        behandling.avsluttBehandling();
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(any())).thenReturn(Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(LocalDate.now()).build());
        when(beregningRepository.skalReberegne(anyLong(), any())).thenReturn(Boolean.FALSE);

        // Act
        håndterer.håndterAvsluttetBehandling(behandling, ForretningshendelseType.FØDSEL, RE_HENDELSE_FØDSEL);

        // Assert
        verifyNoInteractions(behandlingsoppretter);
        verify(beregningRepository).skalReberegne(eq(behandling.getId()),any());
    }

    @Test
    public void skal_opprette_revurdering_ved_opprinnelig_avslag() {
        // Arrange
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse().medFødselsDato(LocalDate.now().minusDays(2)).medAntallBarn(1);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.AUTO_VENT_PÅ_FØDSELREGISTRERING, BehandlingStegType.KONTROLLER_FAKTA);
        behandling = scenario.lagMocked();
        behandling.avsluttBehandling();
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(any())).thenReturn(Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(LocalDate.now()).build());
        when(beregningRepository.skalReberegne(anyLong(), any())).thenReturn(Boolean.TRUE);

        // Act
        håndterer.håndterAvsluttetBehandling(behandling, ForretningshendelseType.FØDSEL, RE_HENDELSE_FØDSEL);

        // Assert
        verify(behandlingsoppretter).opprettRevurdering(any(), any());
        verify(beregningRepository).skalReberegne(eq(behandling.getId()), any());
    }

    @Test
    public void skal_ikke_opprette_revurdering_ved_sen_registrering() {
        // Arrange
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse().medFødselsDato(LocalDate.now().minusDays(2)).medAntallBarn(1);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.AUTO_VENT_PÅ_FØDSELREGISTRERING, BehandlingStegType.KONTROLLER_FAKTA);
        behandling = scenario.lagMocked();
        behandling.avsluttBehandling();
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(any())).thenReturn(Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(LocalDate.now().minusDays(70)).build());
        lenient().when(beregningRepository.skalReberegne(anyLong(), any())).thenReturn(Boolean.TRUE);

        // Act
        håndterer.håndterAvsluttetBehandling(behandling, ForretningshendelseType.FØDSEL, RE_HENDELSE_FØDSEL);

        // Assert
        verifyNoInteractions(beregningRepository);
    }
}
