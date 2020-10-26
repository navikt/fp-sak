package no.nav.foreldrepenger.mottak.dokumentmottak.fp;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.mottak.Behandlingsoppretter;
import no.nav.foreldrepenger.mottak.dokumentmottak.HistorikkinnslagTjeneste;
import no.nav.foreldrepenger.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.foreldrepenger.mottak.dokumentmottak.impl.Dokumentmottaker;
import no.nav.foreldrepenger.mottak.dokumentmottak.impl.DokumentmottakerFelles;
import no.nav.foreldrepenger.mottak.dokumentmottak.impl.DokumentmottakerSøknadDefault;
import no.nav.foreldrepenger.mottak.dokumentmottak.impl.DokumentmottakerTestsupport;
import no.nav.foreldrepenger.mottak.dokumentmottak.impl.KøKontroller;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

public class DokmentmottakerSøknadHåndterÅpenFørstegang extends DokumentmottakerTestsupport {

    private Dokumentmottaker dokumentmottakerSøknad;
    private Behandlingsoppretter behandlingsoppretterSpied;
    private KøKontroller køKontroller;

    @Override
    protected void setUpBeforeEach() {
        this.behandlingsoppretterSpied = Mockito.spy(behandlingsoppretter);
        this.køKontroller = Mockito.mock(KøKontroller.class);
        this.dokumentmottakerSøknad = new DokumentmottakerSøknadDefault(
            repositoryProvider,
            dokumentmottakerFelles,
            behandlingsoppretterSpied,
            kompletthetskontroller,
            køKontroller, fpUttakTjeneste);
    }

    @Test
    public void skal_opprette_ny_førstegangsbehandling_ved_innsending_av_ny_søknad_kompletthet_ikke_passert() {
        // Pre-Arrange: Registerdata + fagsak
        MottatteDokumentTjeneste mockMD = Mockito.mock(MottatteDokumentTjeneste.class);
        HistorikkinnslagTjeneste mockHist = Mockito.mock(HistorikkinnslagTjeneste.class);
        BehandlendeEnhetTjeneste enhetsTjeneste = mock(BehandlendeEnhetTjeneste.class);
        OrganisasjonsEnhet enhet = new OrganisasjonsEnhet("0312", "enhetNavn");
        when(enhetsTjeneste.finnBehandlendeEnhetFor(any(Fagsak.class))).thenReturn(enhet);
        ProsessTaskRepository taskrepo = mock(ProsessTaskRepository.class);
        DokumentmottakerFelles felles = new DokumentmottakerFelles(repositoryProvider,
            taskrepo,
            enhetsTjeneste,
            mockHist,
            mockMD,
            behandlingsoppretterSpied);
        dokumentmottakerSøknad = new DokumentmottakerSøknadDefault(
            repositoryProvider,
            felles,
                behandlingsoppretterSpied,
            kompletthetskontroller,
            køKontroller, fpUttakTjeneste);
        Behandling nyBehandling = opprettNyBehandlingUtenVedtak(FagsakYtelseType.FORELDREPENGER);
        Mockito.doReturn(nyBehandling).when(behandlingsoppretterSpied).oppdaterBehandlingViaHenleggelse(Mockito.any(),  Mockito.any());
        doNothing().when(mockMD).persisterDokumentinnhold(any(), any(), any());
        when(mockMD.harMottattDokumentSet(any(), any())).thenReturn(Boolean.TRUE);

        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medSøknadHendelse().medFødselsDato(LocalDate.now().minusDays(6)).medAntallBarn(1);
        scenario.medBehandlingStegStart(BehandlingStegType.VURDER_KOMPLETTHET);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.AUTO_VENTER_PÅ_KOMPLETT_SØKNAD, BehandlingStegType.VURDER_KOMPLETTHET);
        Behandling behandling = scenario.lagre(repositoryProvider);

        behandling = repositoryProvider.getBehandlingRepository().hentBehandling(behandling.getId());

        MottattDokument søknadDokument = dummySøknadDokument(behandling);

        // Act
        dokumentmottakerSøknad.mottaDokument(søknadDokument, behandling.getFagsak(), BehandlingÅrsakType.RE_ANNET);

        // Assert
        Mockito.verify(behandlingsoppretterSpied, Mockito.times(1)).oppdaterBehandlingViaHenleggelse(Mockito.any(), Mockito.any());
    }

}
