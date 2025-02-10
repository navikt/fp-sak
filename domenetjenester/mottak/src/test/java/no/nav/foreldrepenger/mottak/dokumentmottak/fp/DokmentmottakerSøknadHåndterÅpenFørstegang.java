package no.nav.foreldrepenger.mottak.dokumentmottak.fp;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.mottak.Behandlingsoppretter;
import no.nav.foreldrepenger.mottak.dokumentmottak.HistorikkinnslagTjeneste;
import no.nav.foreldrepenger.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.foreldrepenger.mottak.dokumentmottak.impl.Dokumentmottaker;
import no.nav.foreldrepenger.mottak.dokumentmottak.impl.DokumentmottakerFelles;
import no.nav.foreldrepenger.mottak.dokumentmottak.impl.DokumentmottakerSøknadDefault;
import no.nav.foreldrepenger.mottak.dokumentmottak.impl.DokumentmottakerTestsupport;
import no.nav.foreldrepenger.mottak.sakskompleks.KøKontroller;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.TomtUttakTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

class DokmentmottakerSøknadHåndterÅpenFørstegang extends DokumentmottakerTestsupport {

    private Dokumentmottaker dokumentmottakerSøknad;
    private Behandlingsoppretter behandlingsoppretterSpied;
    private KøKontroller køKontroller;

    @BeforeEach
    void setUp() {
        this.behandlingsoppretterSpied = Mockito.spy(behandlingsoppretter);
        this.køKontroller = Mockito.mock(KøKontroller.class);
        this.dokumentmottakerSøknad = new DokumentmottakerSøknadDefault(
            repositoryProvider.getBehandlingRepository(),
            dokumentmottakerFelles,
            behandlingsoppretterSpied,
            kompletthetskontroller,
            køKontroller, fpUttakTjeneste, behandlingRevurderingTjeneste);
    }

    @Test
    void skal_opprette_ny_førstegangsbehandling_ved_innsending_av_ny_søknad_kompletthet_ikke_passert() {
        // Pre-Arrange: Registerdata + fagsak
        var mockMD = Mockito.mock(MottatteDokumentTjeneste.class);
        var mockHist = Mockito.mock(HistorikkinnslagTjeneste.class);
        var enhetsTjeneste = mock(BehandlendeEnhetTjeneste.class);
        var taskrepo = mock(ProsessTaskTjeneste.class);
        var felles = new DokumentmottakerFelles(repositoryProvider,
                behandlingRevurderingTjeneste,
            taskrepo,
            enhetsTjeneste,
            mockHist,
            mockMD,
            behandlingsoppretterSpied,
            mock(TomtUttakTjeneste.class));
        dokumentmottakerSøknad = new DokumentmottakerSøknadDefault(
            repositoryProvider.getBehandlingRepository(), felles, behandlingsoppretterSpied, kompletthetskontroller, køKontroller, fpUttakTjeneste,
                behandlingRevurderingTjeneste);
        var nyBehandling = opprettNyBehandlingUtenVedtak(FagsakYtelseType.FORELDREPENGER);
        Mockito.doReturn(nyBehandling).when(behandlingsoppretterSpied).oppdaterBehandlingViaHenleggelse(Mockito.any(),  Mockito.any());
        doNothing().when(mockMD).persisterDokumentinnhold(any(), any(), any());
        when(mockMD.harMottattDokumentSet(any(), any())).thenReturn(Boolean.TRUE);

        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medSøknadHendelse().medFødselsDato(LocalDate.now().minusDays(6)).medAntallBarn(1);
        scenario.medBehandlingStegStart(BehandlingStegType.VURDER_KOMPLETT_BEH);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.AUTO_VENTER_PÅ_KOMPLETT_SØKNAD, BehandlingStegType.VURDER_KOMPLETT_BEH);
        var behandling = scenario.lagre(repositoryProvider);

        behandling = repositoryProvider.getBehandlingRepository().hentBehandling(behandling.getId());

        var søknadDokument = dummySøknadDokument(behandling);

        // Act
        dokumentmottakerSøknad.mottaDokument(søknadDokument, behandling.getFagsak(), BehandlingÅrsakType.RE_ANNET);

        // Assert
        Mockito.verify(behandlingsoppretterSpied, Mockito.times(1)).oppdaterBehandlingViaHenleggelse(Mockito.any(), Mockito.any());
    }

}
