package no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.Period;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.impl.BehandlingModellRepository;
import no.nav.foreldrepenger.behandlingskontroll.impl.BehandlingskontrollTjenesteImpl;
import no.nav.foreldrepenger.behandlingskontroll.spi.BehandlingskontrollServiceProvider;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.historikk.OppgaveÅrsak;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveBehandlingKobling;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveBehandlingKoblingRepository;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.vedtak.exception.FunksjonellException;

@CdiDbAwareTest
public class BehandlingsutredningTjenesteTest {
    @Inject
    private OppgaveBehandlingKoblingRepository oppgaveBehandlingKoblingRepository;

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;

    @Inject
    private BehandlingRepository behandlingRepository;

    @Inject
    private BehandlingskontrollServiceProvider behandlingskontrollServiceProvider;

    @Mock
    private OppgaveTjeneste oppgaveTjenesteMock;

    @Mock
    private BehandlingModellRepository behandlingModellRepositoryMock;

    @Mock
    private RevurderingTjeneste revurderingTjenesteMock;

    @Mock
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;

    private BehandlingsutredningTjeneste behandlingsutredningTjeneste;

    private Long behandlingId;

    @BeforeEach
    public void setUp() {
        behandlingRepository = repositoryProvider.getBehandlingRepository();
        var behandling = ScenarioMorSøkerEngangsstønad
                .forFødsel()
                .lagre(repositoryProvider);
        behandlingId = behandling.getId();

        var behandlingskontrollTjenesteImpl = new BehandlingskontrollTjenesteImpl(behandlingskontrollServiceProvider);

        lenient().when(behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(any(Fagsak.class)))
                .thenReturn(new OrganisasjonsEnhet("1234", "Testlokasjon"));

        behandlingsutredningTjeneste = new BehandlingsutredningTjeneste(
                Period.parse("P4W"),
                repositoryProvider,
                oppgaveTjenesteMock,
                behandlendeEnhetTjeneste,
                behandlingskontrollTjenesteImpl);
    }

    @Test
    public void skal_sette_behandling_pa_vent() {
        // Act
        behandlingsutredningTjeneste.settBehandlingPaVent(behandlingId, LocalDate.now(), Venteårsak.AVV_DOK);

        // Assert
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        assertThat(behandling.isBehandlingPåVent()).isTrue();
        assertThat(behandling.getÅpneAksjonspunkter()).hasSize(1);
        assertThat(behandling.getÅpneAksjonspunkter().get(0)).isExactlyInstanceOf(Aksjonspunkt.class);
    }

    @Test
    public void skal_oppdatere_ventefrist_og_arsakskode() {
        // Arrange
        var toUkerFrem = LocalDate.now().plusWeeks(2);

        // Act
        behandlingsutredningTjeneste.settBehandlingPaVent(behandlingId, LocalDate.now(), Venteårsak.AVV_DOK);
        behandlingsutredningTjeneste.endreBehandlingPaVent(behandlingId, toUkerFrem, Venteårsak.AVV_FODSEL);

        // Assert
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        assertThat(behandling.getFristDatoBehandlingPåVent()).isEqualTo(toUkerFrem);
        assertThat(behandling.getVenteårsak()).isEqualTo(Venteårsak.AVV_FODSEL);
    }

    @Test
    public void skal_kaste_feil_når_oppdatering_av_ventefrist_av_behandling_som_ikke_er_på_vent() {
        // Arrange
        var toUkerFrem = LocalDate.now().plusWeeks(2);

        // Act
        assertThrows(FunksjonellException.class,
                () -> behandlingsutredningTjeneste.endreBehandlingPaVent(behandlingId, toUkerFrem, Venteårsak.AVV_FODSEL));
    }

    @Test
    public void skal_sette_behandling_med_oppgave_pa_vent_og_opprette_task_avslutt_oppgave() {
        // Arrange
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var oppgave = new OppgaveBehandlingKobling(OppgaveÅrsak.BEHANDLE_SAK, "1",
                behandling.getFagsak().getSaksnummer(), behandling.getId());
        oppgaveBehandlingKoblingRepository.lagre(oppgave);

        // Act
        behandlingsutredningTjeneste.settBehandlingPaVent(behandlingId, LocalDate.now(), Venteårsak.AVV_DOK);

        // Assert
        verify(oppgaveTjenesteMock).opprettTaskAvsluttOppgave(any(Behandling.class));
        assertThat(behandling.isBehandlingPåVent()).isTrue();
        assertThat(behandling.getÅpneAksjonspunkter()).hasSize(1);
        assertThat(behandling.getÅpneAksjonspunkter().get(0)).isExactlyInstanceOf(Aksjonspunkt.class);
    }

    @Test
    public void skal_bytte_behandlende_enhet() {
        // Arrange
        var enhetNavn = "OSLO";
        var enhetId = "22";
        var årsak = "Test begrunnelse";
        var enhet = new OrganisasjonsEnhet(enhetId, enhetNavn);

        // Act
        behandlingsutredningTjeneste.byttBehandlendeEnhet(behandlingId, enhet, årsak, HistorikkAktør.SAKSBEHANDLER);

        // Assert
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        verify(behandlendeEnhetTjeneste).oppdaterBehandlendeEnhet(behandling, enhet, HistorikkAktør.SAKSBEHANDLER, årsak);
    }
}
