package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import static java.time.LocalDate.now;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandling.BehandlingRevurderingTjeneste;
import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.mottak.Behandlingsoppretter;
import no.nav.foreldrepenger.mottak.dokumentmottak.HistorikkinnslagTjeneste;
import no.nav.foreldrepenger.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.TomtUttakTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@CdiDbAwareTest
class DokumentmottakerInntektsmeldingTest {

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    @Inject
    private BehandlingRepository behandlingRepository;
    @Inject
    private FagsakRepository fagsakRepository;
    @Inject
    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;
    @Inject
    private ForeldrepengerUttakTjeneste fpUttakTjeneste;
    @Inject
    private BehandlingRevurderingTjeneste behandlingRevurderingTjeneste;

    @Mock
    private ProsessTaskTjeneste taskTjeneste;
    @Mock
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;
    @Mock
    private MottatteDokumentTjeneste mottatteDokumentTjeneste;
    @Mock
    private Kompletthetskontroller kompletthetskontroller;
    @Mock
    private Behandlingsoppretter behandlingsoppretter;
    @Mock
    private HistorikkinnslagTjeneste historikkinnslagTjeneste;

    private DokumentmottakerInntektsmelding dokumentmottaker;
    private DokumentmottakerFelles dokumentmottakerFelles;

    @BeforeEach
    public void oppsett() {
        dokumentmottakerFelles = new DokumentmottakerFelles(repositoryProvider, behandlingRevurderingTjeneste, taskTjeneste, behandlendeEnhetTjeneste,
                historikkinnslagTjeneste, mottatteDokumentTjeneste, behandlingsoppretter, mock(TomtUttakTjeneste.class));

        dokumentmottakerFelles = Mockito.spy(dokumentmottakerFelles);

        dokumentmottaker = new DokumentmottakerInntektsmelding(dokumentmottakerFelles, behandlingsoppretter,
                kompletthetskontroller, repositoryProvider.getBehandlingRepository(), behandlingRevurderingTjeneste, fpUttakTjeneste);
        dokumentmottaker = Mockito.spy(dokumentmottaker);

        var enhet = new OrganisasjonsEnhet("0312", "enhetNavn");
        lenient().when(behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(any(Fagsak.class))).thenReturn(enhet);
    }

    @Test
    void skal_oppdatere_ukomplett_behandling_med_IM_dersom_fagsak_har_avsluttet_behandling_og_åpen_behandling_og_kompletthet_ikke_passert() {
        // Arrange
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var behandling = scenario.lagre(repositoryProvider);
        behandling.avsluttBehandling();
        var behandlingLås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, behandlingLås);

        var revurderingScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medFagsakId(behandling.getFagsakId())
                .medBehandlingStegStart(BehandlingStegType.REGISTRER_SØKNAD)
                .medOriginalBehandling(behandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);
        var revurderingBehandling = revurderingScenario.lagre(repositoryProvider);
        var dokumentTypeId = DokumentTypeId.INNTEKTSMELDING;
        var mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, revurderingBehandling.getFagsakId(), "", now(),
                true, "123");

        // Act
        dokumentmottaker.mottaDokument(mottattDokument, revurderingBehandling.getFagsak(), BehandlingÅrsakType.UDEFINERT);

        // Assert
        verify(kompletthetskontroller).persisterDokumentOgVurderKompletthet(revurderingBehandling, mottattDokument);
        verify(dokumentmottakerFelles).opprettHistorikk(revurderingBehandling, mottattDokument);
    }

    @Test
    void skal_oppdatere_behandling_vurdere_kompletthet_og_spole_til_nytt_startpunkt_dersom_fagsak_har_avsluttet_behandling_har_åpen_behandling_og_kompletthet_passert() {
        // Arrange - opprette avsluttet førstegangsbehandling
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var behandling = scenario.lagre(repositoryProvider);
        behandling.avsluttBehandling();
        var behandlingLås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, behandlingLås);

        // Arrange - opprette revurdering som har passert kompletthet
        var revurderingScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medFagsakId(behandling.getFagsakId())
                .medBehandlingStegStart(BehandlingStegType.FORESLÅ_VEDTAK)
                .medOriginalBehandling(behandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);
        var revurderingBehandling = revurderingScenario.lagre(repositoryProvider);

        // Arrange - bygg inntektsmelding
        var dokumentTypeId = DokumentTypeId.INNTEKTSMELDING;
        var mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, revurderingBehandling.getFagsakId(), "", now(),
                true, "123");

        // Act
        dokumentmottaker.mottaDokument(mottattDokument, revurderingBehandling.getFagsak(), BehandlingÅrsakType.UDEFINERT);

        // Assert - sjekk flyt
        verify(dokumentmottaker).oppdaterÅpenBehandlingMedDokument(revurderingBehandling, mottattDokument, BehandlingÅrsakType.UDEFINERT);
        verify(kompletthetskontroller).persisterDokumentOgVurderKompletthet(revurderingBehandling, mottattDokument);
        verify(dokumentmottakerFelles).opprettHistorikk(revurderingBehandling, mottattDokument);
    }

    @Test
    void skal_lagre_dokument_og_vurdere_kompletthet_dersom_inntektsmelding_på_åpen_behandling() {
        // Arrange - opprette åpen behandling
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medBehandlingStegStart(BehandlingStegType.INNHENT_SØKNADOPP);
        var behandling = scenario.lagre(repositoryProvider);
        opprettAksjonspunkt(behandling, AksjonspunktDefinisjon.AUTO_VENTER_PÅ_KOMPLETT_SØKNAD, LocalDateTime.now());

        // Arrange - bygg inntektsmelding
        var dokumentTypeId = DokumentTypeId.INNTEKTSMELDING;
        var mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, behandling.getFagsakId(), "", now(), true,
                "123");

        // Act
        dokumentmottaker.mottaDokument(mottattDokument, behandling.getFagsak(), BehandlingÅrsakType.UDEFINERT);

        // Assert - sjekk flyt
        verify(kompletthetskontroller).persisterDokumentOgVurderKompletthet(behandling, mottattDokument);
        verify(dokumentmottakerFelles).opprettHistorikk(behandling, mottattDokument);
    }

    @Test
    void skal_lagre_dokument_og_vurdere_kompletthet_dersom_inntektsmelding_etterlyst() {
        // Arrange - opprette åpen behandling
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medBehandlingStegStart(BehandlingStegType.INNHENT_SØKNADOPP);
        var behandling = scenario.lagre(repositoryProvider);
        opprettAksjonspunkt(behandling, AksjonspunktDefinisjon.AUTO_VENT_ETTERLYST_INNTEKTSMELDING, LocalDateTime.now().plusDays(1));

        // Arrange - bygg inntektsmelding
        var dokumentTypeId = DokumentTypeId.INNTEKTSMELDING;
        var mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, behandling.getFagsakId(), "", now(), true,
                "123");

        // Act
        dokumentmottaker.mottaDokument(mottattDokument, behandling.getFagsak(), BehandlingÅrsakType.UDEFINERT);

        // Assert - sjekk flyt
        verify(kompletthetskontroller).persisterDokumentOgVurderKompletthet(behandling, mottattDokument);
        verify(dokumentmottakerFelles).opprettHistorikk(behandling, mottattDokument);
    }

    @Test
    void skal_opprette_revurdering_dersom_inntektsmelding_på_avsluttet_behandling() {
        // Arrange - opprette avsluttet førstegangsbehandling
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var behandling = scenario.lagre(repositoryProvider);
        behandling.avsluttBehandling();
        var behandlingLås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, behandlingLås);

        var revurdering = mock(Behandling.class);
        when(revurdering.getId()).thenReturn(10L);
        when(revurdering.getFagsakId()).thenReturn(behandling.getFagsakId());
        when(revurdering.getFagsak()).thenReturn(behandling.getFagsak());
        when(revurdering.getSaksnummer()).thenReturn(behandling.getSaksnummer());

        var dokumentTypeId = DokumentTypeId.INNTEKTSMELDING;
        var mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, behandling.getFagsakId(), "", now(), true,
                "123");
        when(behandlingsoppretter.opprettRevurdering(behandling.getFagsak(), BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING)).thenReturn(revurdering);

        // Act
        dokumentmottaker.mottaDokument(mottattDokument, behandling.getFagsak(), BehandlingÅrsakType.UDEFINERT);

        // Assert
        verify(behandlingsoppretter).opprettRevurdering(behandling.getFagsak(), BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING);
        verify(mottatteDokumentTjeneste).persisterDokumentinnhold(revurdering, mottattDokument, Optional.empty());
    }

    @Test
    void skal_opprette_førstegangsbehandling() {

        var fagsak = DokumentmottakTestUtil.byggFagsak(AktørId.dummy(), RelasjonsRolleType.MORA, NavBrukerKjønn.KVINNE, new Saksnummer("9999"),
                fagsakRepository);
        var dokumentTypeId = DokumentTypeId.INNTEKTSMELDING;
        var mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, 123L, "", now(), true, "123");
        var førstegangsbehandling = mock(Behandling.class);
        when(førstegangsbehandling.getSaksnummer()).thenReturn(new Saksnummer("9999"));
        when(behandlingsoppretter.opprettFørstegangsbehandling(fagsak, BehandlingÅrsakType.UDEFINERT, Optional.empty()))
                .thenReturn(førstegangsbehandling);

        // Act
        dokumentmottaker.mottaDokument(mottattDokument, fagsak, BehandlingÅrsakType.UDEFINERT);

        // Assert
        verify(behandlingsoppretter).opprettFørstegangsbehandling(fagsak, BehandlingÅrsakType.UDEFINERT, Optional.empty());
        verify(dokumentmottakerFelles).opprettInitiellFørstegangsbehandling(fagsak, mottattDokument, BehandlingÅrsakType.UDEFINERT);
    }

    @Test
    void skal_opprette_køet_revurdering_og_kjøre_kompletthet_dersom_køet_behandling_ikke_finnes() {
        // Arrange - opprette avsluttet førstegangsbehandling
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medBehandlingsresultat(new Behandlingsresultat.Builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET))
                .lagre(repositoryProvider);
        behandling.avsluttBehandling();
        var vedtak = DokumentmottakTestUtil.oppdaterVedtaksresultat(behandling, VedtakResultatType.INNVILGET);
        repositoryProvider.getBehandlingRepository().lagre(behandling, repositoryProvider.getBehandlingRepository().taSkriveLås(behandling));
        repositoryProvider.getBehandlingVedtakRepository().lagre(vedtak, repositoryProvider.getBehandlingRepository().taSkriveLås(behandling));
        var fagsak = behandling.getFagsak();

        var revurdering = mock(Behandling.class);
        doReturn(revurdering).when(behandlingsoppretter).opprettRevurdering(behandling.getFagsak(), BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING);

        var dokumentTypeId = DokumentTypeId.INNTEKTSMELDING;
        var mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, behandling.getFagsakId(), "", now(), true,
                "123");

        // Act
        dokumentmottaker.mottaDokumentForKøetBehandling(mottattDokument, behandling.getFagsak(), BehandlingÅrsakType.UDEFINERT);

        // Assert
        verify(behandlingsoppretter).opprettRevurdering(fagsak, BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING);
        verify(behandlingsoppretter).settSomKøet(any());
        verify(dokumentmottakerFelles).opprettKøetRevurdering(mottattDokument, fagsak, BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING);
    }

    @Test
    void skal_opprette_køet_behandling_og_kjøre_kompletthet_dersom_køet_behandling_ikke_finnes() {
        // Arrange - opprette fagsak uten behandling
        var aktørId = AktørId.dummy();
        var saksnummer = new Saksnummer("9999");
        var fagsak = DokumentmottakTestUtil.byggFagsak(aktørId, RelasjonsRolleType.MORA, NavBrukerKjønn.KVINNE, saksnummer,
                fagsakRepository);

        // Arrange - sett opp opprettelse av køet behandling
        var behandling = mock(Behandling.class);
        // doReturn(fagsak.getId()).when(behandling).getFagsakId();
        doReturn(fagsak).when(behandling).getFagsak();
        doReturn(saksnummer).when(behandling).getSaksnummer();
        doReturn(behandling).when(behandlingsoppretter).opprettFørstegangsbehandling(fagsak, BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING,
                Optional.empty());
        /*
         * doAnswer(invocationOnMock -> { return null;
         * }).when(dokumentmottakerFelles).leggTilBehandlingsårsak(behandling,
         * BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING);
         */
        // Arrange - bygg inntektsmelding
        var dokumentTypeId = DokumentTypeId.INNTEKTSMELDING;
        var mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsak.getId(), "", now(), true, "123");

        // Act
        dokumentmottaker.mottaDokumentForKøetBehandling(mottattDokument, fagsak, BehandlingÅrsakType.UDEFINERT);

        // Assert - sjekk flyt
        verify(behandlingsoppretter).opprettFørstegangsbehandling(fagsak, BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING, Optional.empty());
        verify(dokumentmottakerFelles).opprettInitiellFørstegangsbehandling(fagsak, mottattDokument,
                BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING);
    }

    @Test
    void skal_oppdatere_køet_behandling_og_kjøre_kompletthet_dersom_køet_behandling_finnes() {
        // Arrange - opprette køet førstegangsbehandling
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var behandling = scenario.lagre(repositoryProvider);
        var behandlingLås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, behandlingLås);
        simulerKøetBehandling(behandling);

        // Act - send inntektsmelding
        var dokumentTypeId = DokumentTypeId.INNTEKTSMELDING;
        var mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, behandling.getFagsakId(), "", now(), true,
                "123");
        dokumentmottaker.mottaDokumentForKøetBehandling(mottattDokument, behandling.getFagsak(), BehandlingÅrsakType.UDEFINERT);

        // Assert - verifiser flyt
        verify(kompletthetskontroller).persisterKøetDokumentOgVurderKompletthet(behandling, mottattDokument, Optional.empty());
        verify(dokumentmottakerFelles).opprettHistorikk(behandling, mottattDokument);
    }

    @Test
    void skal_lage_ny_førstegangsbehandling_med_inntektsmeldingen_etter_henlagt_førstegangsbehandling() {
        // Arrange
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medBehandlingsresultat(
                Behandlingsresultat.builderForInngangsvilkår().medBehandlingResultatType(BehandlingResultatType.HENLAGT_SØKNAD_TRUKKET));
        var behandling = scenario.lagre(repositoryProvider);
        behandling.avsluttBehandling();
        var fagsakId = behandling.getFagsakId();
        var dokumentTypeId = DokumentTypeId.INNTEKTSMELDING;

        var mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsakId, "", now(), true, null);
        doReturn(behandling).when(behandlingsoppretter).opprettFørstegangsbehandling(any(Fagsak.class), any(BehandlingÅrsakType.class),
                any(Optional.class));

        // Act
        dokumentmottaker.opprettFraTidligereAvsluttetBehandling(behandling.getFagsak(), behandling.getId(), mottattDokument,
                BehandlingÅrsakType.UDEFINERT, false);

        // Assert
        verify(behandlingsoppretter).opprettFørstegangsbehandling(behandling.getFagsak(), BehandlingÅrsakType.UDEFINERT, Optional.of(behandling));
        verify(mottatteDokumentTjeneste).persisterDokumentinnhold(behandling, mottattDokument, Optional.empty());
        verify(dokumentmottakerFelles).opprettTaskForÅStarteBehandling(behandling);
    }

    @Test
    void skal_ikke_lage_ny_førstegangsbehandling_med_inntektsmeldingen_når_det_finnes_en_åpen_behandling() {
        // Arrange
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var behandling = scenario.lagre(repositoryProvider);
        var fagsakId = behandling.getFagsakId();
        var dokumentTypeId = DokumentTypeId.INNTEKTSMELDING;

        var mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsakId, "", now(), true, null);

        // Act
        dokumentmottaker.opprettFraTidligereAvsluttetBehandling(behandling.getFagsak(), behandling.getId(), mottattDokument,
                BehandlingÅrsakType.UDEFINERT, false);

        // Assert
        verify(behandlingsoppretter, times(0)).opprettFørstegangsbehandling(behandling.getFagsak(), BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING,
                Optional.of(behandling));
    }

    @Test
    void skal_ikke_lage_ny_førstegangsbehandling_med_inntektsmeldingen_når_forrige_behandling_er_innvilget() {
        // Arrange
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medBehandlingsresultat(Behandlingsresultat.builderForInngangsvilkår().medBehandlingResultatType(BehandlingResultatType.INNVILGET));
        var behandling = scenario.lagre(repositoryProvider);
        behandling.avsluttBehandling();
        var fagsakId = behandling.getFagsakId();
        var dokumentTypeId = DokumentTypeId.INNTEKTSMELDING;

        var mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsakId, "", now(), true, null);

        // Act
        dokumentmottaker.opprettFraTidligereAvsluttetBehandling(behandling.getFagsak(), behandling.getId(), mottattDokument,
                BehandlingÅrsakType.UDEFINERT, false);

        // Assert
        verify(behandlingsoppretter, times(0)).opprettFørstegangsbehandling(behandling.getFagsak(), BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING,
                Optional.of(behandling));
    }

    private void simulerKøetBehandling(Behandling behandling) {
        AksjonspunktTestSupport.leggTilAksjonspunkt(behandling, AksjonspunktDefinisjon.AUTO_KØET_BEHANDLING);
    }

    private Aksjonspunkt opprettAksjonspunkt(Behandling behandling,
            AksjonspunktDefinisjon aksjonspunktDefinisjon,
            LocalDateTime frist) {

        var aksjonspunkt = AksjonspunktTestSupport.leggTilAksjonspunkt(behandling, aksjonspunktDefinisjon);
        AksjonspunktTestSupport.setFrist(aksjonspunkt, frist, Venteårsak.UDEFINERT);
        return aksjonspunkt;
    }
}
