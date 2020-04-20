package no.nav.foreldrepenger.mottak.dokumentmottak.es;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.mottak.Behandlingsoppretter;
import no.nav.foreldrepenger.mottak.dokumentmottak.HistorikkinnslagTjeneste;
import no.nav.foreldrepenger.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.foreldrepenger.mottak.dokumentmottak.impl.Dokumentmottaker;
import no.nav.foreldrepenger.mottak.dokumentmottak.impl.DokumentmottakerFelles;
import no.nav.foreldrepenger.mottak.dokumentmottak.impl.DokumentmottakerTestsupport;
import no.nav.foreldrepenger.mottak.dokumentmottak.impl.KøKontroller;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

public class DokumentmottakerSøknadEngangsstønadHåndteringVedAvslåttBehandlingTest extends DokumentmottakerTestsupport {

    private Dokumentmottaker dokumentmottakerSøknad;
    private Behandlingsoppretter behandlingsoppretterSpied;
    private KøKontroller køKontroller;

    @Before
    public void setup() {
        this.behandlingsoppretterSpied = Mockito.spy(behandlingsoppretter);
        this.køKontroller = Mockito.mock(KøKontroller.class);
        dokumentmottakerSøknad = new DokumentmottakerSøknadEngangsstønad(
            repositoryProvider,
            dokumentmottakerFelles,
            behandlingsoppretterSpied,
            kompletthetskontroller,
            køKontroller, fpUttakTjeneste);
    }

    @Test
    public void skal_opprette_ny_førstegangsbehandling_når_forrige_behandling_var_avslått() {
        //Arrange
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
        dokumentmottakerSøknad = new DokumentmottakerSøknadEngangsstønad(
            repositoryProvider,
            felles,
            behandlingsoppretterSpied,
            kompletthetskontroller,
            køKontroller, fpUttakTjeneste);
        Behandling nyBehandling = opprettNyBehandlingUtenVedtak(FagsakYtelseType.ENGANGSTØNAD);
        Mockito.doReturn(nyBehandling).when(behandlingsoppretterSpied).opprettNyFørstegangsbehandlingMedImOgVedleggFraForrige(Mockito.any(), Mockito.any(), any(), anyBoolean());
        doNothing().when(mockMD).persisterDokumentinnhold(any(), any(), any());

        Behandling behandling = opprettBehandling(
            FagsakYtelseType.ENGANGSTØNAD,
            BehandlingType.FØRSTEGANGSSØKNAD,
            BehandlingResultatType.AVSLÅTT,
            Avslagsårsak.MANGLENDE_DOKUMENTASJON,
            VedtakResultatType.AVSLAG,
            DATO_FØR_INNSENDINGSFRISTEN);
        MottattDokument søknadDokument = dummySøknadDokument(behandling);

        // Act
        dokumentmottakerSøknad.mottaDokument(søknadDokument, behandling.getFagsak(), BehandlingÅrsakType.RE_ANNET);

        // Assert
        Mockito.verify(behandlingsoppretterSpied, Mockito.times(1)).opprettNyFørstegangsbehandlingMedImOgVedleggFraForrige(Mockito.any(), Mockito.any(), any(), anyBoolean());
    }

    @Test
    public void gittAvslåttBehandlingForEngangsstønadSkalOppretteNyFørstegangsbehandlingFraGrunnlag() {
        //Arrange
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
        dokumentmottakerSøknad = new DokumentmottakerSøknadEngangsstønad(
            repositoryProvider,
            felles,
            behandlingsoppretterSpied,
            kompletthetskontroller,
            køKontroller, fpUttakTjeneste);
        Behandling nyBehandling = opprettNyBehandlingUtenVedtak(FagsakYtelseType.ENGANGSTØNAD);
        Mockito.doReturn(nyBehandling).when(behandlingsoppretterSpied).opprettNyFørstegangsbehandlingFraTidligereSøknad(Mockito.any(),  Mockito.any(), Mockito.any());
        doNothing().when(mockMD).persisterDokumentinnhold(any(), any(), any());

        Behandling behandling = opprettBehandling(
            FagsakYtelseType.ENGANGSTØNAD,
            BehandlingType.FØRSTEGANGSSØKNAD,
            BehandlingResultatType.AVSLÅTT,
            Avslagsårsak.MANGLENDE_DOKUMENTASJON,
            VedtakResultatType.AVSLAG,
            DATO_FØR_INNSENDINGSFRISTEN);
        MottattDokument søknadDokument = dummySøknadDokument(behandling);

        // Act
        dokumentmottakerSøknad.opprettFraTidligereAvsluttetBehandling(behandling.getFagsak(), behandling.getId(), søknadDokument, BehandlingÅrsakType.RE_ANNET, false);

        // Assert
        Mockito.verify(behandlingsoppretterSpied, Mockito.times(1)).opprettNyFørstegangsbehandlingFraTidligereSøknad(behandling.getFagsak(), BehandlingÅrsakType.RE_ANNET, behandling);
    }

}
