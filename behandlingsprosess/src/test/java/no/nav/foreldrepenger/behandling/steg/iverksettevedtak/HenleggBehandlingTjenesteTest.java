package no.nav.foreldrepenger.behandling.steg.iverksettevedtak;

import static no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType.IVERKSETT_VEDTAK;
import static no.nav.foreldrepenger.behandlingslager.behandling.InternalManipulerBehandling.forceOppdaterBehandlingSteg;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import no.nav.foreldrepenger.behandling.BehandlingEventPubliserer;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingModell;
import no.nav.foreldrepenger.behandlingskontroll.impl.BehandlingModellRepository;
import no.nav.foreldrepenger.behandlingskontroll.impl.BehandlingskontrollTjenesteImpl;
import no.nav.foreldrepenger.behandlingskontroll.spi.BehandlingskontrollServiceProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKontrollRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.foreldrepenger.behandlingslager.behandling.events.BehandlingHenlagtEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;

@CdiDbAwareTest
class HenleggBehandlingTjenesteTest {

    private BehandlingRepositoryProvider repositoryProvider;

    @Mock
    private HistorikkinnslagRepository historikkRepositoryMock;

    @Mock
    private BehandlingEventPubliserer eventPubliserer;

    @Inject
    private BehandlingModellRepository behandlingModellRepository;

    @Mock
    private BehandlingModell modell;

    @Inject
    private AksjonspunktKontrollRepository aksjonspunktKontrollRepository;

    private HenleggBehandlingTjeneste henleggBehandlingTjeneste;

    private Behandling behandling;

    @BeforeEach
    void setUp() {
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        behandling = scenario.lagMocked();
        repositoryProvider = scenario.mockBehandlingRepositoryProvider();

        forceOppdaterBehandlingSteg(behandling, BehandlingStegType.INNHENT_SØKNADOPP);

        when(repositoryProvider.getHistorikkinnslagRepository()).thenReturn(historikkRepositoryMock);

        var serviceProvider = new BehandlingskontrollServiceProvider(
                repositoryProvider.getFagsakRepository(),
                repositoryProvider.getBehandlingRepository(),
                repositoryProvider.getFagsakLåsRepository(),
                repositoryProvider.getBehandlingLåsRepository(),
                aksjonspunktKontrollRepository);

        var behandlingskontrollTjenesteImpl = new BehandlingskontrollTjenesteImpl(serviceProvider);
        lenient().when(modell.erStegAFørStegB(any(), any())).thenReturn(true);

        henleggBehandlingTjeneste = new HenleggBehandlingTjeneste(repositoryProvider, behandlingskontrollTjenesteImpl, eventPubliserer);
    }

    @Test
    void skal_henlegge_behandling_med_brev() {
        // Arrange
        var behandlingsresultat = BehandlingResultatType.HENLAGT_SØKNAD_TRUKKET;

        // Act
        henleggBehandlingTjeneste.henleggBehandlingManuell(behandling.getId(), behandlingsresultat, "begrunnelse");

        // Assert
        verify(historikkRepositoryMock).lagre(any(Historikkinnslag.class));
        verify(repositoryProvider.getBehandlingRepository(), atLeast(2)).lagre(eq(behandling), any(BehandlingLås.class));
        verify(eventPubliserer).publiserBehandlingEvent(any(BehandlingHenlagtEvent.class));
    }

    @Test
    void skal_henlegge_behandling_uten_brev() {
        // Arrange
        var behandlingsresultat = BehandlingResultatType.HENLAGT_FEILOPPRETTET;

        // Act
        henleggBehandlingTjeneste.henleggBehandlingManuell(behandling.getId(), behandlingsresultat, "begrunnelse");

        // Assert
        verify(historikkRepositoryMock).lagre(any(Historikkinnslag.class));
        verify(repositoryProvider.getBehandlingRepository(), atLeast(2)).lagre(eq(behandling), any(BehandlingLås.class));
    }

    @Test
    void skal_henlegge_behandling_med_aksjonspunkt() {
        // Arrange
        var behandlingsresultat = BehandlingResultatType.HENLAGT_FEILOPPRETTET;
        var aksjonspunkt = AksjonspunktTestSupport.leggTilAksjonspunkt(behandling,
                AksjonspunktDefinisjon.SJEKK_MANGLENDE_FØDSEL);
        assertThat(aksjonspunkt.getStatus()).isEqualTo(AksjonspunktStatus.OPPRETTET);

        // Act
        henleggBehandlingTjeneste.henleggBehandlingManuell(behandling.getId(), behandlingsresultat, "begrunnelse");

        // Assert
        verify(historikkRepositoryMock).lagre(any(Historikkinnslag.class));
        verify(repositoryProvider.getBehandlingRepository(), atLeastOnce()).lagre(eq(behandling), any(BehandlingLås.class));
        assertThat(aksjonspunkt.getStatus()).isEqualTo(AksjonspunktStatus.AVBRUTT);
    }

    @Test
    void skal_henlegge_behandling_ved_dødsfall() {
        // Arrange
        var behandlingsresultat = BehandlingResultatType.HENLAGT_BRUKER_DØD;

        // Act
        henleggBehandlingTjeneste.henleggBehandlingManuell(behandling.getId(), behandlingsresultat, "begrunnelse");

        // Assert
        verify(historikkRepositoryMock).lagre(any(Historikkinnslag.class));
        verify(repositoryProvider.getBehandlingRepository(), atLeast(2)).lagre(eq(behandling), any(BehandlingLås.class));
    }

    @Test
    void kan_henlegge_behandling_som_er_satt_på_vent() {
        // Arrange
        var def = AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT;
        var aksjonspunkt = AksjonspunktTestSupport.leggTilAksjonspunkt(behandling, def);
        AksjonspunktTestSupport.setFrist(aksjonspunkt, LocalDateTime.now(), null);

        forceOppdaterBehandlingSteg(behandling, BehandlingStegType.INNHENT_SØKNADOPP);

        var behandlingsresultat = BehandlingResultatType.HENLAGT_SØKNAD_TRUKKET;

        // Act
        henleggBehandlingTjeneste.henleggBehandlingManuell(behandling.getId(), behandlingsresultat, "begrunnelse");
        assertThat(behandling.getStatus()).isEqualTo(BehandlingStatus.AVSLUTTET);
    }

    @Test
    void kan_henlegge_behandling_der_vedtak_er_foreslått() {
        // Arrange
        Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET).buildFor(behandling);
        var behandlingsresultat = BehandlingResultatType.HENLAGT_SØKNAD_TRUKKET;

        // Act
        henleggBehandlingTjeneste.henleggBehandlingManuell(behandling.getId(), behandlingsresultat, "begrunnelse");
        assertThat(behandling.getStatus()).isEqualTo(BehandlingStatus.AVSLUTTET);
    }

    @Test
    void kan_ikke_henlegge_behandling_der_vedtak_er_fattet() {
        // Arrange
        Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET).buildFor(behandling);
        var behandlingsresultat = BehandlingResultatType.HENLAGT_SØKNAD_TRUKKET;
        forceOppdaterBehandlingSteg(behandling, IVERKSETT_VEDTAK);

        // Act
        assertThatThrownBy(() -> henleggBehandlingTjeneste.henleggBehandlingManuell(behandling.getId(), behandlingsresultat, "begrunnelse"))
                .hasMessageContaining("FP-143308");
    }

    @Test
    void kan_ikke_henlegge_behandling_som_allerede_er_henlagt() {
        // Arrange
        Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.HENLAGT_FEILOPPRETTET).buildFor(behandling);
        var behandlingsresultat = BehandlingResultatType.HENLAGT_SØKNAD_TRUKKET;

        // Act
        assertThatThrownBy(() -> henleggBehandlingTjeneste.henleggBehandlingManuell(behandling.getId(), behandlingsresultat, "begrunnelse"))
                .hasMessageContaining("FP-143308");
    }

}
