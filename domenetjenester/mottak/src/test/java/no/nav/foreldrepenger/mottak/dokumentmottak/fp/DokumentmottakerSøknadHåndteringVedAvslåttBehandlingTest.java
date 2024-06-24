package no.nav.foreldrepenger.mottak.dokumentmottak.fp;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
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

class DokumentmottakerSøknadHåndteringVedAvslåttBehandlingTest extends DokumentmottakerTestsupport {

    private Dokumentmottaker dokumentmottakerSøknad;
    private Behandlingsoppretter behandlingsoppretterSpied;
    private KøKontroller køKontroller;

    @BeforeEach
    void setUp() {
        this.behandlingsoppretterSpied = Mockito.spy(behandlingsoppretter);
        this.køKontroller = Mockito.mock(KøKontroller.class);
        dokumentmottakerSøknad = new DokumentmottakerSøknadDefault(repositoryProvider.getBehandlingRepository(), dokumentmottakerFelles,
            behandlingsoppretterSpied, kompletthetskontroller, køKontroller, fpUttakTjeneste, behandlingRevurderingTjeneste);
    }

    @Test
    void skal_opprette_ny_førstegangsbehandling_når_forrige_behandling_var_avslått() {
        //Arrange
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
        dokumentmottakerSøknad = new DokumentmottakerSøknadDefault(repositoryProvider.getBehandlingRepository(), felles, behandlingsoppretterSpied,
            kompletthetskontroller, køKontroller, fpUttakTjeneste, behandlingRevurderingTjeneste);
        var nyBehandling = opprettNyBehandlingUtenVedtak(FagsakYtelseType.FORELDREPENGER);
        Mockito.doReturn(nyBehandling).when(behandlingsoppretterSpied).opprettNyFørstegangsbehandlingMedImOgVedleggFraForrige(Mockito.any(), Mockito.any(), Mockito.any(), anyBoolean());
        doNothing().when(mockMD).persisterDokumentinnhold(any(), any(), any());

        var behandling = opprettBehandling(
            FagsakYtelseType.FORELDREPENGER,
            BehandlingType.FØRSTEGANGSSØKNAD,
            BehandlingResultatType.AVSLÅTT,
            Avslagsårsak.MANGLENDE_DOKUMENTASJON,
            VedtakResultatType.AVSLAG,
            DATO_FØR_INNSENDINGSFRISTEN);
        var søknadDokument = dummySøknadDokument(behandling);

        // Act
        dokumentmottakerSøknad.mottaDokument(søknadDokument, behandling.getFagsak(), BehandlingÅrsakType.RE_ANNET);

        // Assert
        Mockito.verify(behandlingsoppretterSpied, Mockito.times(1)).opprettNyFørstegangsbehandlingMedImOgVedleggFraForrige(Mockito.any(), Mockito.any(), Mockito.any(), anyBoolean());
    }
}
