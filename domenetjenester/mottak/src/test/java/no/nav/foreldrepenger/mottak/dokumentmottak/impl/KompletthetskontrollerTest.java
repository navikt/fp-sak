package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import static java.time.LocalDate.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatSnapshot;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.kompletthet.KompletthetResultat;
import no.nav.foreldrepenger.kompletthet.Kompletthetsjekker;
import no.nav.foreldrepenger.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@ExtendWith(MockitoExtension.class)
class KompletthetskontrollerTest {

    @Mock
    private DokumentmottakerFelles dokumentmottakerFelles;

    @Mock
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    @Mock
    private Kompletthetsjekker kompletthetsjekker;

    @Mock
    private MottatteDokumentTjeneste mottatteDokumentTjeneste;

    @Mock
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;

    private Kompletthetskontroller kompletthetskontroller;
    private Behandling behandling;
    private MottattDokument mottattDokument;

    @BeforeEach
    void oppsett() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        behandling = scenario.lagMocked();
        kompletthetskontroller = new Kompletthetskontroller(dokumentmottakerFelles, mottatteDokumentTjeneste, behandlingProsesseringTjeneste, skjæringstidspunktTjeneste, kompletthetsjekker);
        mottattDokument = DokumentmottakTestUtil.byggMottattDokument(DokumentTypeId.INNTEKTSMELDING, behandling.getFagsakId(), "", now(), true, null);

    }

    @Test
    void skal_sette_behandling_på_vent_dersom_kompletthet_ikke_er_oppfylt() {
        // Arrange
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.AUTO_VENTER_PÅ_KOMPLETT_SØKNAD, BehandlingStegType.VURDER_KOMPLETT_BEH);
        var behandling = scenario.lagMocked();
        var ventefrist = LocalDateTime.now().plusDays(1);

        when(kompletthetsjekker.vurderForsendelseKomplett(any(), any())).thenReturn(KompletthetResultat.ikkeOppfylt(ventefrist, Venteårsak.AVV_FODSEL));

        kompletthetskontroller.persisterDokumentOgVurderKompletthet(behandling, mottattDokument);

        verify(behandlingProsesseringTjeneste, times(0)).opprettTasksForGjenopptaOppdaterFortsett(eq(behandling), any());
    }

    @Test
    void skal_beholde_behandling_på_vent_dersom_kompletthet_ikke_er_oppfylt_deretter_slippe_videre() {
        // Arrange
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.AUTO_VENT_ETTERLYST_INNTEKTSMELDING, BehandlingStegType.INREG_AVSL);
        var behandling = scenario.lagMocked();
        var ventefrist = LocalDateTime.now().plusDays(1);

        when(kompletthetsjekker.vurderEtterlysningInntektsmelding(any(), any())).thenReturn(
            KompletthetResultat.ikkeOppfylt(ventefrist, Venteårsak.AVV_FODSEL));
        lenient().when(behandlingProsesseringTjeneste.erBehandlingEtterSteg(behandling, BehandlingStegType.REGISTRER_SØKNAD)).thenReturn(true);

        // Act
        kompletthetskontroller.persisterDokumentOgVurderKompletthet(behandling, mottattDokument);

        // Assert
        verify(behandlingProsesseringTjeneste, times(0)).opprettTasksForGjenopptaOppdaterFortsett(eq(behandling), any());

        // Arrange 2
        when(kompletthetsjekker.vurderEtterlysningInntektsmelding(any(), any())).thenReturn(KompletthetResultat.oppfylt());

        // Act 2
        kompletthetskontroller.persisterDokumentOgVurderKompletthet(behandling, mottattDokument);

        // Assert 2
        verify(behandlingProsesseringTjeneste).opprettTasksForFortsettBehandling(behandling);
    }

    @Test
    void skal_gjenoppta_behandling_dersom_behandling_er_komplett_og_kompletthet_ikke_passert() {
        // Arrange
        when(behandlingProsesseringTjeneste.erBehandlingEtterSteg(behandling, BehandlingStegType.INNHENT_REGISTEROPP)).thenReturn(false);
        when(behandlingProsesseringTjeneste.erBehandlingFørSteg(behandling, BehandlingStegType.VURDER_KOMPLETT_TIDLIG)).thenReturn(false);

        kompletthetskontroller.persisterDokumentOgVurderKompletthet(behandling, mottattDokument);

        verify(behandlingProsesseringTjeneste).opprettTasksForFortsettBehandling(behandling);
    }

    @Test
    void skal_ikke_gjenoppta_behandling_dersom_behandling_er_komplett_og_regsok_ikke_passert() {
        // Arrange
        when(behandlingProsesseringTjeneste.erBehandlingFørSteg(behandling, BehandlingStegType.VURDER_KOMPLETT_TIDLIG)).thenReturn(true);

        kompletthetskontroller.persisterDokumentOgVurderKompletthet(behandling, mottattDokument);

        verify(behandlingProsesseringTjeneste).taSnapshotAvBehandlingsgrunnlag(behandling);
        verify(behandlingProsesseringTjeneste, times(0)).opprettTasksForFortsettBehandling(behandling);
    }

    @Test
    void skal_gjenoppta_behandling_ved_mottak_av_ny_forretningshendelse() {
        // Arrange
        when(behandlingProsesseringTjeneste.erBehandlingEtterSteg(behandling, BehandlingStegType.INNHENT_REGISTEROPP)).thenReturn(true);

        kompletthetskontroller.vurderNyForretningshendelse(behandling, BehandlingÅrsakType.RE_HENDELSE_FØDSEL);

        verify(behandlingProsesseringTjeneste).opprettTasksForGjenopptaOppdaterFortsett(eq(behandling), any());
    }

    @Test
    void skal_fortsette_behandling_i_kompletthet_ved_mottak_av_ny_forretningshendelse() {
        // Arrange
        var scenario2 = ScenarioMorSøkerForeldrepenger.forFødsel()
            .leggTilAksjonspunkt(AksjonspunktDefinisjon.VENT_PGA_FOR_TIDLIG_SØKNAD, BehandlingStegType.VURDER_KOMPLETT_TIDLIG);
        var behandling2 = scenario2.lagMocked();
        when(behandlingProsesseringTjeneste.erBehandlingEtterSteg(behandling2, BehandlingStegType.INNHENT_REGISTEROPP)).thenReturn(false);

        kompletthetskontroller.vurderNyForretningshendelse(behandling2, BehandlingÅrsakType.RE_HENDELSE_DØD_FORELDER);

        verify(behandlingProsesseringTjeneste).opprettTasksForFortsettBehandling(behandling2);
    }

    @Test
    void skal_spole_til_startpunkt_dersom_komplett_og_vurder_kompletthet_er_passert() {
        // Arrange
        when(behandlingProsesseringTjeneste.erBehandlingEtterSteg(behandling, BehandlingStegType.INNHENT_REGISTEROPP)).thenReturn(true);
        when(behandlingProsesseringTjeneste.erBehandlingFørSteg(behandling, BehandlingStegType.VURDER_KOMPLETT_TIDLIG)).thenReturn(false);

        var endringsresultatSnapshot = EndringsresultatSnapshot.opprett();
        when(behandlingProsesseringTjeneste.taSnapshotAvBehandlingsgrunnlag(behandling)).thenReturn(endringsresultatSnapshot);

        var endringsresultat = EndringsresultatDiff.opprett();
        endringsresultat.leggTilIdDiff(EndringsresultatDiff.medDiff(PersonInformasjonEntitet.class, endringsresultatSnapshot.getGrunnlagRef(), 1L));

        // Act - send inntektsmelding
        kompletthetskontroller.persisterDokumentOgVurderKompletthet(behandling, mottattDokument);

        // Assert
        verify(behandlingProsesseringTjeneste).opprettTasksForGjenopptaOppdaterFortsett(eq(behandling), any());
    }

    /**
     * Test at det ikke legges inn flere autopunkter i samme behandlingssteg for kompletthet.
     * Hvis du skal legge inn flere autopunkter i samme steg så må du gjennomgå kompletthetskontrolleren
     */
    @Test
    void ikke_legg_inn_flere_autopunkter_i_samme_behandlingset_for_kompletthet() {
        assertThat(AksjonspunktDefinisjon.VENT_PÅ_SØKNAD.getBehandlingSteg()).isEqualTo(BehandlingStegType.REGISTRER_SØKNAD);
        assertThat(Arrays.stream(AksjonspunktDefinisjon.values())
            .filter(a -> AksjonspunktType.AUTOPUNKT.equals(a.getAksjonspunktType()))
            .filter(a -> BehandlingStegType.REGISTRER_SØKNAD.equals(a.getBehandlingSteg()))
            .toList())
            .hasSize(1);

        assertThat(AksjonspunktDefinisjon.VENT_PGA_FOR_TIDLIG_SØKNAD.getBehandlingSteg()).isEqualTo(BehandlingStegType.VURDER_KOMPLETT_TIDLIG);
        assertThat(Arrays.stream(AksjonspunktDefinisjon.values())
            .filter(a -> AksjonspunktType.AUTOPUNKT.equals(a.getAksjonspunktType()))
            .filter(a -> BehandlingStegType.VURDER_KOMPLETT_TIDLIG.equals(a.getBehandlingSteg()))
            .toList())
            .hasSize(1);

        assertThat(AksjonspunktDefinisjon.AUTO_VENTER_PÅ_KOMPLETT_SØKNAD.getBehandlingSteg()).isEqualTo(BehandlingStegType.VURDER_KOMPLETT_BEH);
        assertThat(Arrays.stream(AksjonspunktDefinisjon.values())
            .filter(a -> AksjonspunktType.AUTOPUNKT.equals(a.getAksjonspunktType()))
            .filter(a -> BehandlingStegType.VURDER_KOMPLETT_BEH.equals(a.getBehandlingSteg()))
            .toList())
            .hasSize(1);

        assertThat(AksjonspunktDefinisjon.AUTO_VENT_ETTERLYST_INNTEKTSMELDING.getBehandlingSteg()).isEqualTo(BehandlingStegType.INREG_AVSL);
        assertThat(Arrays.stream(AksjonspunktDefinisjon.values())
            .filter(a -> AksjonspunktType.AUTOPUNKT.equals(a.getAksjonspunktType()))
            .filter(a -> BehandlingStegType.INREG_AVSL.equals(a.getBehandlingSteg()))
            .toList())
            .hasSize(1);
    }
}
