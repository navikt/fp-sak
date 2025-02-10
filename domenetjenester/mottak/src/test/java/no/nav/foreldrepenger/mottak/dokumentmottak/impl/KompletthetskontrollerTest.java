package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import static java.time.LocalDate.now;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatSnapshot;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.kompletthet.KompletthetResultat;
import no.nav.foreldrepenger.kompletthet.Kompletthetsjekker;
import no.nav.foreldrepenger.kompletthet.KompletthetsjekkerProvider;
import no.nav.foreldrepenger.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.foreldrepenger.mottak.kompletthettjeneste.KompletthetModell;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@ExtendWith(MockitoExtension.class)
class KompletthetskontrollerTest {

    @Mock
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    @Mock
    private KompletthetsjekkerProvider kompletthetsjekkerProvider;

    @Mock
    private DokumentmottakerFelles dokumentmottakerFelles;

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
    public void oppsett() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        behandling = scenario.lagMocked();

        // Simuler at provider alltid gir kompletthetssjekker
        lenient().when(kompletthetsjekkerProvider.finnKompletthetsjekkerFor(any(), any())).thenReturn(kompletthetsjekker);

        var modell = new KompletthetModell(behandlingskontrollTjeneste, kompletthetsjekkerProvider);
        var skjæringstidspunktTjeneste = Mockito.mock(SkjæringstidspunktTjeneste.class);

        kompletthetskontroller = new Kompletthetskontroller(dokumentmottakerFelles, mottatteDokumentTjeneste, modell, behandlingProsesseringTjeneste,
            skjæringstidspunktTjeneste);

        mottattDokument = DokumentmottakTestUtil.byggMottattDokument(DokumentTypeId.INNTEKTSMELDING, behandling.getFagsakId(), "", now(), true, null);

    }

    @Test
    void skal_sette_behandling_på_vent_dersom_kompletthet_ikke_er_oppfylt() {
        // Arrange
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.AUTO_VENTER_PÅ_KOMPLETT_SØKNAD, BehandlingStegType.VURDER_KOMPLETT_BEH);
        var behandling = scenario.lagMocked();
        var ventefrist = LocalDateTime.now().plusDays(1);

        when(kompletthetsjekkerProvider.finnKompletthetsjekkerFor(any(), any())).thenReturn(kompletthetsjekker);
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

        when(kompletthetsjekkerProvider.finnKompletthetsjekkerFor(any(), any())).thenReturn(kompletthetsjekker);
        when(kompletthetsjekker.vurderEtterlysningInntektsmelding(any(), any())).thenReturn(
            KompletthetResultat.ikkeOppfylt(ventefrist, Venteårsak.AVV_FODSEL));
        lenient().when(behandlingskontrollTjeneste.erStegPassert(behandling.getId(), BehandlingStegType.REGISTRER_SØKNAD)).thenReturn(true);

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
        when(behandlingskontrollTjeneste.erStegPassert(behandling.getId(), BehandlingStegType.VURDER_KOMPLETT_BEH)).thenReturn(false);
        when(behandlingskontrollTjeneste.erIStegEllerSenereSteg(behandling.getId(), BehandlingStegType.VURDER_KOMPLETT_BEH)).thenReturn(true);

        kompletthetskontroller.persisterDokumentOgVurderKompletthet(behandling, mottattDokument);

        verify(behandlingProsesseringTjeneste).opprettTasksForFortsettBehandling(behandling);
    }

    @Test
    void skal_ikke_gjenoppta_behandling_dersom_behandling_er_komplett_og_regsok_ikke_passert() {
        // Arrange
        when(behandlingskontrollTjeneste.erStegPassert(behandling.getId(), BehandlingStegType.VURDER_KOMPLETT_BEH)).thenReturn(false);
        when(behandlingskontrollTjeneste.erIStegEllerSenereSteg(behandling.getId(), BehandlingStegType.VURDER_KOMPLETT_BEH)).thenReturn(false);

        kompletthetskontroller.persisterDokumentOgVurderKompletthet(behandling, mottattDokument);

        verify(behandlingProsesseringTjeneste).taSnapshotAvBehandlingsgrunnlag(behandling);
        verify(behandlingProsesseringTjeneste, times(0)).opprettTasksForFortsettBehandling(behandling);
    }

    @Test
    void skal_gjenoppta_behandling_ved_mottak_av_ny_forretningshendelse() {
        // Arrange
        when(behandlingskontrollTjeneste.erStegPassert(behandling.getId(), BehandlingStegType.VURDER_KOMPLETT_BEH)).thenReturn(true);

        kompletthetskontroller.vurderNyForretningshendelse(behandling, BehandlingÅrsakType.RE_HENDELSE_FØDSEL);

        verify(behandlingProsesseringTjeneste).opprettTasksForGjenopptaOppdaterFortsett(eq(behandling), any());
    }

    @Test
    void skal_fortsette_behandling_i_kompletthet_ved_mottak_av_ny_forretningshendelse() {
        // Arrange
        var scenario2 = ScenarioMorSøkerForeldrepenger.forFødsel()
            .leggTilAksjonspunkt(AksjonspunktDefinisjon.VENT_PGA_FOR_TIDLIG_SØKNAD, BehandlingStegType.VURDER_KOMPLETT_BEH);
        var behandling2 = scenario2.lagMocked();
        when(behandlingskontrollTjeneste.erStegPassert(behandling2.getId(), BehandlingStegType.VURDER_KOMPLETT_BEH)).thenReturn(false);

        kompletthetskontroller.vurderNyForretningshendelse(behandling2, BehandlingÅrsakType.RE_HENDELSE_DØD_FORELDER);

        verify(behandlingProsesseringTjeneste).opprettTasksForFortsettBehandling(behandling2);
    }

    @Test
    void skal_spole_til_startpunkt_dersom_komplett_og_vurder_kompletthet_er_passert() {
        // Arrange
        when(behandlingskontrollTjeneste.erStegPassert(behandling.getId(), BehandlingStegType.VURDER_KOMPLETT_BEH)).thenReturn(true);
        when(behandlingskontrollTjeneste.erIStegEllerSenereSteg(behandling.getId(), BehandlingStegType.VURDER_KOMPLETT_BEH)).thenReturn(true);

        var endringsresultatSnapshot = EndringsresultatSnapshot.opprett();
        when(behandlingProsesseringTjeneste.taSnapshotAvBehandlingsgrunnlag(behandling)).thenReturn(endringsresultatSnapshot);

        var endringsresultat = EndringsresultatDiff.opprett();
        endringsresultat.leggTilIdDiff(EndringsresultatDiff.medDiff(PersonInformasjonEntitet.class, endringsresultatSnapshot.getGrunnlagRef(), 1L));

        // Act - send inntektsmelding
        kompletthetskontroller.persisterDokumentOgVurderKompletthet(behandling, mottattDokument);

        // Assert
        verify(behandlingProsesseringTjeneste).opprettTasksForGjenopptaOppdaterFortsett(eq(behandling), any());
    }

    @Test
    void skal_opprette_historikkinnslag_for_tidlig_mottatt_søknad() {
        // Arrange
        var frist = LocalDateTime.now().minusSeconds(30);
        when(kompletthetsjekker.vurderSøknadMottattForTidlig(any())).thenReturn(KompletthetResultat.ikkeOppfylt(frist, Venteårsak.FOR_TIDLIG_SOKNAD));

        // Act
        kompletthetskontroller.persisterKøetDokumentOgVurderKompletthet(behandling, mottattDokument, Optional.empty());

        // Assert
        verify(mottatteDokumentTjeneste).persisterDokumentinnhold(behandling, mottattDokument, Optional.empty());
        verify(dokumentmottakerFelles).opprettHistorikkinnslagForVenteFristRelaterteInnslag(behandling, frist, Venteårsak.FOR_TIDLIG_SOKNAD);
    }

    @Test
    void skal_opprette_historikkinnslag_ikke_komplett() {
        // Arrange
        var frist = LocalDateTime.now();
        when(kompletthetsjekker.vurderSøknadMottatt(any())).thenReturn(KompletthetResultat.oppfylt());
        when(kompletthetsjekker.vurderSøknadMottattForTidlig(any())).thenReturn(KompletthetResultat.oppfylt());
        when(kompletthetsjekker.vurderForsendelseKomplett(any(), any())).thenReturn(KompletthetResultat.ikkeOppfylt(frist, Venteårsak.AVV_DOK));
        when(kompletthetsjekker.vurderEtterlysningInntektsmelding(any(), any())).thenReturn(KompletthetResultat.oppfylt());

        // Act
        kompletthetskontroller.persisterKøetDokumentOgVurderKompletthet(behandling, mottattDokument, Optional.empty());

        // Assert
        verify(mottatteDokumentTjeneste).persisterDokumentinnhold(behandling, mottattDokument, Optional.empty());
        verify(dokumentmottakerFelles).opprettHistorikkinnslagForVenteFristRelaterteInnslag(behandling, frist, Venteårsak.AVV_DOK);
    }
}
