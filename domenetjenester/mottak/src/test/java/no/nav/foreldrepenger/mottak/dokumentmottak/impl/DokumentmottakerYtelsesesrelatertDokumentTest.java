package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import static java.time.LocalDate.now;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.mottak.Behandlingsoppretter;
import no.nav.foreldrepenger.mottak.dokumentmottak.HistorikkinnslagTjeneste;
import no.nav.foreldrepenger.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.TomtUttakTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@CdiDbAwareTest
public class DokumentmottakerYtelsesesrelatertDokumentTest {

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    @Inject
    private BehandlingRepository behandlingRepository;
    @Inject
    private ForeldrepengerUttakTjeneste fpUttakTjeneste;

    @Mock
    private ProsessTaskTjeneste taskTjeneste;
    @Mock
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;
    @Mock
    private MottatteDokumentTjeneste mottatteDokumentTjeneste;
    @Mock
    private DokumentmottakerFelles dokumentmottakerFelles;
    @Mock
    private Kompletthetskontroller kompletthetskontroller;
    @Mock
    private Behandlingsoppretter behandlingsoppretter;
    @Mock
    private HistorikkinnslagTjeneste historikkinnslagTjeneste;

    private DokumentmottakerYtelsesesrelatertDokument dokumentmottaker;

    @BeforeEach
    public void oppsett() {
        lenient().when(behandlendeEnhetTjeneste.gyldigEnhetNfpNk(any())).thenReturn(true);
        dokumentmottakerFelles = new DokumentmottakerFelles(repositoryProvider, taskTjeneste, behandlendeEnhetTjeneste,
                historikkinnslagTjeneste, mottatteDokumentTjeneste, behandlingsoppretter, mock(TomtUttakTjeneste.class));

        dokumentmottakerFelles = Mockito.spy(dokumentmottakerFelles);

        dokumentmottaker = new DokumentmottakerInntektsmelding(dokumentmottakerFelles, behandlingsoppretter,
                kompletthetskontroller, repositoryProvider, fpUttakTjeneste);

        dokumentmottaker = Mockito.spy(dokumentmottaker);

        var enhet = new OrganisasjonsEnhet("0312", "enhetNavn");
        // when(behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(any(Fagsak.class))).thenReturn(enhet);
    }

    @Test
    public void skal_opprette_vurder_dokument_oppgave_dersom_avslått_behandling() {
        // Arrange - opprette avsluttet førstegangsbehandling
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medVilkårResultatType(VilkårResultatType.AVSLÅTT);
        var behandling = scenario.lagre(repositoryProvider);
        avsluttBehandling(behandling, VedtakResultatType.AVSLAG);
        behandling = behandlingRepository.hentBehandling(behandling.getId());

        var dokumentTypeId = DokumentTypeId.INNTEKTSMELDING;
        var mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, behandling.getFagsakId(), "", now(), true,
                "123");
        when(behandlingsoppretter.erAvslåttBehandling(behandling)).thenReturn(true);

        // Act
        dokumentmottaker.mottaDokument(mottattDokument, behandling.getFagsak(), BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING);

        // Assert
        verify(dokumentmottaker).håndterAvslåttEllerOpphørtBehandling(mottattDokument, behandling.getFagsak(), behandling,
                BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING);
        verify(dokumentmottakerFelles).opprettTaskForÅVurdereDokument(behandling.getFagsak(), behandling, mottattDokument);
    }

    @Test
    public void skal_opprette_førstegangsbehandling_dersom_avslått_behandling_har_entydig_avslag() {
        // Arrange - opprette avsluttet førstegangsbehandling
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medVilkårResultatType(VilkårResultatType.AVSLÅTT)
                .leggTilVilkår(VilkårType.SØKERSOPPLYSNINGSPLIKT, VilkårUtfallType.IKKE_OPPFYLT);
        var behandling = scenario.lagre(repositoryProvider);
        behandling.avsluttBehandling();
        avsluttBehandling(behandling, VedtakResultatType.AVSLAG);
        behandling = behandlingRepository.hentBehandling(behandling.getId());

        var dokumentTypeId = DokumentTypeId.LEGEERKLÆRING;
        var mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, behandling.getFagsakId(), "", now(), true,
                "123");
        doReturn(true).when(behandlingsoppretter).erAvslåttBehandling(behandling);

        var nyBehandling = Behandling.fraTidligereBehandling(behandling, BehandlingType.FØRSTEGANGSSØKNAD).build();
        // Hack, men det blir feil å lagre Behandlingen før Act da det påvirker
        // scenarioet, og mock(Behandling) er heller ikke pent...
        nyBehandling.setId(9999L);
        // Act
        dokumentmottaker.mottaDokument(mottattDokument, behandling.getFagsak(), BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING);

        // Assert
        verify(dokumentmottaker).håndterAvslåttEllerOpphørtBehandling(mottattDokument, behandling.getFagsak(), behandling,
                BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING);
    }

    @Test
    public void skal_opprette_vurder_dokument_oppgave_dersom_opphørt_behandling() {
        // Arrange - opprette avsluttet førstegangsbehandling
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medVilkårResultatType(VilkårResultatType.AVSLÅTT);
        var behandling = scenario.lagre(repositoryProvider);
        behandling.avsluttBehandling();
        avsluttBehandling(behandling, VedtakResultatType.OPPHØR);
        behandling = behandlingRepository.hentBehandling(behandling.getId());

        var dokumentTypeId = DokumentTypeId.INNTEKTSMELDING;
        var mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, behandling.getFagsakId(), "", now(), true,
                "123");
        when(behandlingsoppretter.erOpphørtBehandling(behandling)).thenReturn(true);
        Mockito.doReturn(false).when(dokumentmottaker).harInnvilgetPeriode(behandling);

        // Act
        dokumentmottaker.mottaDokument(mottattDokument, behandling.getFagsak(), BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING);

        // Assert
        verify(dokumentmottaker).håndterAvslåttEllerOpphørtBehandling(mottattDokument, behandling.getFagsak(), behandling,
                BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING);
        verify(dokumentmottakerFelles).opprettTaskForÅVurdereDokument(behandling.getFagsak(), behandling, mottattDokument);
    }

    @Test
    public void skal_ikke_opprette_førstegangsbehandling_dersom_opphørt_behandling() {
        // Arrange - opprette avsluttet førstegangsbehandling
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medVilkårResultatType(VilkårResultatType.INNVILGET)
                .leggTilVilkår(VilkårType.SØKERSOPPLYSNINGSPLIKT, VilkårUtfallType.IKKE_OPPFYLT);
        var behandling = scenario.lagre(repositoryProvider);
        behandling.avsluttBehandling();
        avsluttBehandling(behandling, VedtakResultatType.OPPHØR);
        behandling = behandlingRepository.hentBehandling(behandling.getId());

        var dokumentTypeId = DokumentTypeId.INNTEKTSMELDING;
        var mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, behandling.getFagsakId(), "", now(), true,
                "123");
        doReturn(true).when(behandlingsoppretter).erOpphørtBehandling(behandling);

        // Act
        dokumentmottaker.mottaDokument(mottattDokument, behandling.getFagsak(), BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING);

        // Assert
        verify(dokumentmottaker).håndterAvslåttEllerOpphørtBehandling(mottattDokument, behandling.getFagsak(), behandling,
                BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING);
        verify(behandlingsoppretter, times(0)).opprettFørstegangsbehandling(any(), any(), any());
    }

    private void avsluttBehandling(Behandling behandling, VedtakResultatType avslag) {
        behandling.avsluttBehandling();
        var behandlingLås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, behandlingLås);

        var vedtak = DokumentmottakTestUtil.oppdaterVedtaksresultat(behandling, avslag);
        repositoryProvider.getBehandlingVedtakRepository().lagre(vedtak, behandlingLås);
    }
}
