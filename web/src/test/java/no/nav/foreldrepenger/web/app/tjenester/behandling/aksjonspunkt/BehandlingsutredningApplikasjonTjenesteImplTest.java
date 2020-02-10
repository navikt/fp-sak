package no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

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
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.historikk.OppgaveÅrsak;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveBehandlingKobling;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveBehandlingKoblingRepository;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class BehandlingsutredningApplikasjonTjenesteImplTest {

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();

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

    private BehandlingsutredningApplikasjonTjeneste behandlingsutredningApplikasjonTjeneste;

    private Long behandlingId;

    @Before
    public void setUp() {
        behandlingRepository = repositoryProvider.getBehandlingRepository();
        Behandling behandling = ScenarioMorSøkerEngangsstønad
            .forFødsel()
            .lagre(repositoryProvider);
        behandlingId = behandling.getId();

        BehandlingskontrollTjenesteImpl behandlingskontrollTjenesteImpl = new BehandlingskontrollTjenesteImpl(behandlingskontrollServiceProvider);

        when(behandlendeEnhetTjeneste.sjekkEnhetVedNyAvledetBehandling(any(Fagsak.class))).thenReturn(Optional.empty());

        behandlingsutredningApplikasjonTjeneste = new BehandlingsutredningApplikasjonTjeneste(
            Period.parse("P4W"),
            repositoryProvider,
            oppgaveTjenesteMock,
            behandlendeEnhetTjeneste,
            behandlingskontrollTjenesteImpl);
    }

    @Test
    public void skal_sette_behandling_pa_vent() {
        // Act
        behandlingsutredningApplikasjonTjeneste.settBehandlingPaVent(behandlingId, LocalDate.now(), Venteårsak.AVV_DOK);

        // Assert
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        assertThat(behandling.isBehandlingPåVent()).isTrue();
        assertThat(behandling.getÅpneAksjonspunkter()).hasSize(1);
        assertThat(behandling.getÅpneAksjonspunkter().get(0)).isExactlyInstanceOf(Aksjonspunkt.class);
    }

    @Test
    public void skal_oppdatere_ventefrist_og_arsakskode() {
        // Arrange
        LocalDate toUkerFrem = LocalDate.now().plusWeeks(2);

        // Act
        behandlingsutredningApplikasjonTjeneste.settBehandlingPaVent(behandlingId, LocalDate.now(), Venteårsak.AVV_DOK);
        behandlingsutredningApplikasjonTjeneste.endreBehandlingPaVent(behandlingId, toUkerFrem, Venteårsak.AVV_FODSEL);

        // Assert
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        assertThat(behandling.getFristDatoBehandlingPåVent()).isEqualTo(toUkerFrem);
        assertThat(behandling.getVenteårsak()).isEqualTo(Venteårsak.AVV_FODSEL);
    }

    @Test(expected = Exception.class)
    public void skal_kaste_feil_når_oppdatering_av_ventefrist_av_behandling_som_ikke_er_på_vent() {
        // Arrange
        LocalDate toUkerFrem = LocalDate.now().plusWeeks(2);

        // Act
        behandlingsutredningApplikasjonTjeneste.endreBehandlingPaVent(behandlingId, toUkerFrem, Venteårsak.AVV_FODSEL);
    }

    @Test
    public void skal_sette_behandling_med_oppgave_pa_vent_og_opprette_task_avslutt_oppgave() {
        // Arrange
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        OppgaveBehandlingKobling oppgave = new OppgaveBehandlingKobling(OppgaveÅrsak.BEHANDLE_SAK, "1",
            behandling.getFagsak().getSaksnummer(), behandling);
        oppgaveBehandlingKoblingRepository.lagre(oppgave);

        // Act
        behandlingsutredningApplikasjonTjeneste.settBehandlingPaVent(behandlingId, LocalDate.now(), Venteårsak.AVV_DOK);

        // Assert
        verify(oppgaveTjenesteMock).opprettTaskAvsluttOppgave(any(Behandling.class));
        assertThat(behandling.isBehandlingPåVent()).isTrue();
        assertThat(behandling.getÅpneAksjonspunkter()).hasSize(1);
        assertThat(behandling.getÅpneAksjonspunkter().get(0)).isExactlyInstanceOf(Aksjonspunkt.class);
    }

    @Test
    public void skal_bytte_behandlende_enhet() {
        // Arrange
        String enhetNavn = "OSLO";
        String enhetId = "22";
        String årsak = "Test begrunnelse";
        OrganisasjonsEnhet enhet = new OrganisasjonsEnhet(enhetId, enhetNavn);

        // Act
        behandlingsutredningApplikasjonTjeneste.byttBehandlendeEnhet(behandlingId, enhet, årsak, HistorikkAktør.SAKSBEHANDLER);

        // Assert
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        verify(behandlendeEnhetTjeneste).oppdaterBehandlendeEnhet(behandling, enhet, HistorikkAktør.SAKSBEHANDLER, årsak);
    }
}
