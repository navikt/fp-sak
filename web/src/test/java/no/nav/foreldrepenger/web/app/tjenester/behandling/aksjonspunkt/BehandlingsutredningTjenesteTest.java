package no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.Period;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.vedtak.exception.FunksjonellException;

@CdiDbAwareTest
class BehandlingsutredningTjenesteTest {

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;

    @Inject
    private BehandlingRepository behandlingRepository;

    @Inject
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;

    @Mock
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;

    private BehandlingsutredningTjeneste behandlingsutredningTjeneste;

    private Long behandlingId;

    @BeforeEach
    void setUp() {
        behandlingRepository = repositoryProvider.getBehandlingRepository();
        var behandling = ScenarioMorSøkerEngangsstønad
                .forFødsel()
                .lagre(repositoryProvider);
        behandlingId = behandling.getId();

        lenient().when(behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(any(Fagsak.class)))
                .thenReturn(new OrganisasjonsEnhet("1234", "Testlokasjon"));

        behandlingsutredningTjeneste = new BehandlingsutredningTjeneste(
                Period.parse("P4W"),
                repositoryProvider,
                behandlendeEnhetTjeneste,
                behandlingProsesseringTjeneste);
    }

    @Test
    void skal_sette_behandling_pa_vent() {
        // Act
        behandlingsutredningTjeneste.settBehandlingPaVent(behandlingId, LocalDate.now(), Venteårsak.AVV_DOK);

        // Assert
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        assertThat(behandling.isBehandlingPåVent()).isTrue();
        assertThat(behandling.getÅpneAksjonspunkter()).hasSize(1);
        assertThat(behandling.getÅpneAksjonspunkter().get(0)).isExactlyInstanceOf(Aksjonspunkt.class);
    }

    @Test
    void skal_oppdatere_ventefrist_og_arsakskode() {
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
    void skal_kaste_feil_når_oppdatering_av_ventefrist_av_behandling_som_ikke_er_på_vent() {
        // Arrange
        var toUkerFrem = LocalDate.now().plusWeeks(2);

        // Act
        assertThrows(FunksjonellException.class,
                () -> behandlingsutredningTjeneste.endreBehandlingPaVent(behandlingId, toUkerFrem, Venteårsak.AVV_FODSEL));
    }

    @Test
    void skal_bytte_behandlende_enhet() {
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
