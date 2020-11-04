package no.nav.foreldrepenger.behandling.steg.iverksettevedtak;

import static no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType.IVERKSETT_VEDTAK;
import static no.nav.foreldrepenger.behandlingslager.behandling.InternalManipulerBehandling.forceOppdaterBehandlingSteg;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingModell;
import no.nav.foreldrepenger.behandlingskontroll.impl.BehandlingModellRepository;
import no.nav.foreldrepenger.behandlingskontroll.impl.BehandlingskontrollTjenesteImpl;
import no.nav.foreldrepenger.behandlingskontroll.spi.BehandlingskontrollServiceProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKontrollRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestillerTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.dto.BestillBrevDto;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

@CdiDbAwareTest
public class HenleggBehandlingTjenesteTest {

    private BehandlingRepositoryProvider repositoryProvider;

    @Mock
    private HistorikkRepository historikkRepositoryMock;

    @Mock
    private DokumentBestillerTjeneste dokumentBestillerTjenesteMock;

    @Inject
    private BehandlingModellRepository behandlingModellRepository;

    @Mock
    private BehandlingModell modell;

    @Inject
    private AksjonspunktKontrollRepository aksjonspunktKontrollRepository;
    @Mock
    private ProsessTaskRepository prosessTaskRepositoryMock;

    private HenleggBehandlingTjeneste henleggBehandlingTjeneste;

    private Behandling behandling;

    @BeforeEach
    public void setUp() {
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        behandling = scenario.lagMocked();
        repositoryProvider = scenario.mockBehandlingRepositoryProvider();

        forceOppdaterBehandlingSteg(behandling, BehandlingStegType.INNHENT_SØKNADOPP);

        when(repositoryProvider.getHistorikkRepository()).thenReturn(historikkRepositoryMock);

        var serviceProvider = new BehandlingskontrollServiceProvider(
                repositoryProvider.getFagsakRepository(),
                repositoryProvider.getBehandlingRepository(),
                repositoryProvider.getFagsakLåsRepository(),
                repositoryProvider.getBehandlingLåsRepository(),
                behandlingModellRepository,
                aksjonspunktKontrollRepository);

        BehandlingskontrollTjenesteImpl behandlingskontrollTjenesteImpl = new BehandlingskontrollTjenesteImpl(serviceProvider);
        lenient().when(modell.erStegAFørStegB(any(), any())).thenReturn(true);

        henleggBehandlingTjeneste = new HenleggBehandlingTjeneste(repositoryProvider,
                behandlingskontrollTjenesteImpl, dokumentBestillerTjenesteMock, prosessTaskRepositoryMock);
    }

    @Test
    public void skal_henlegge_behandling_med_brev() {
        // Arrange
        BehandlingResultatType behandlingsresultat = BehandlingResultatType.HENLAGT_SØKNAD_TRUKKET;

        // Act
        henleggBehandlingTjeneste.henleggBehandling(behandling.getId(), behandlingsresultat, "begrunnelse");

        // Assert
        verify(historikkRepositoryMock).lagre(any(Historikkinnslag.class));
        verify(repositoryProvider.getBehandlingRepository(), atLeast(2)).lagre(eq(behandling), any(BehandlingLås.class));
        verify(dokumentBestillerTjenesteMock).bestillDokument(any(BestillBrevDto.class), eq(HistorikkAktør.VEDTAKSLØSNINGEN), eq(false));
    }

    @Test
    public void skal_henlegge_behandling_uten_brev() {
        // Arrange
        BehandlingResultatType behandlingsresultat = BehandlingResultatType.HENLAGT_FEILOPPRETTET;

        // Act
        henleggBehandlingTjeneste.henleggBehandling(behandling.getId(), behandlingsresultat, "begrunnelse");

        // Assert
        verify(historikkRepositoryMock).lagre(any(Historikkinnslag.class));
        verify(repositoryProvider.getBehandlingRepository(), atLeast(2)).lagre(eq(behandling), any(BehandlingLås.class));
        verify(dokumentBestillerTjenesteMock, never()).bestillDokument(any(), any(), anyBoolean());
    }

    @Test
    public void skal_henlegge_behandling_med_aksjonspunkt() {
        // Arrange
        BehandlingResultatType behandlingsresultat = BehandlingResultatType.HENLAGT_FEILOPPRETTET;
        Aksjonspunkt aksjonspunkt = AksjonspunktTestSupport.leggTilAksjonspunkt(behandling,
                AksjonspunktDefinisjon.SJEKK_MANGLENDE_FØDSEL);
        assertThat(aksjonspunkt.getStatus()).isEqualTo(AksjonspunktStatus.OPPRETTET);

        // Act
        henleggBehandlingTjeneste.henleggBehandling(behandling.getId(), behandlingsresultat, "begrunnelse");

        // Assert
        verify(historikkRepositoryMock).lagre(any(Historikkinnslag.class));
        verify(repositoryProvider.getBehandlingRepository(), atLeastOnce()).lagre(eq(behandling), any(BehandlingLås.class));
        verify(dokumentBestillerTjenesteMock, never()).bestillDokument(any(), any(), anyBoolean());
        assertThat(aksjonspunkt.getStatus()).isEqualTo(AksjonspunktStatus.AVBRUTT);
    }

    @Test
    public void skal_henlegge_behandling_ved_dødsfall() {
        // Arrange
        BehandlingResultatType behandlingsresultat = BehandlingResultatType.HENLAGT_BRUKER_DØD;

        // Act
        henleggBehandlingTjeneste.henleggBehandling(behandling.getId(), behandlingsresultat, "begrunnelse");

        // Assert
        verify(historikkRepositoryMock).lagre(any(Historikkinnslag.class));
        verify(repositoryProvider.getBehandlingRepository(), atLeast(2)).lagre(eq(behandling), any(BehandlingLås.class));
        verify(dokumentBestillerTjenesteMock, never()).bestillDokument(any(), any(), anyBoolean());
    }

    @Test
    public void kan_henlegge_behandling_som_er_satt_på_vent() {
        // Arrange
        AksjonspunktDefinisjon def = AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT;
        Aksjonspunkt aksjonspunkt = AksjonspunktTestSupport.leggTilAksjonspunkt(behandling, def);
        AksjonspunktTestSupport.setFrist(aksjonspunkt, LocalDateTime.now(), null);

        forceOppdaterBehandlingSteg(behandling, BehandlingStegType.INNHENT_SØKNADOPP);

        BehandlingResultatType behandlingsresultat = BehandlingResultatType.HENLAGT_SØKNAD_TRUKKET;

        // Act
        henleggBehandlingTjeneste.henleggBehandling(behandling.getId(), behandlingsresultat, "begrunnelse");
    }

    @Test
    public void kan_henlegge_behandling_der_vedtak_er_foreslått() {
        // Arrange
        Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET).buildFor(behandling);
        BehandlingResultatType behandlingsresultat = BehandlingResultatType.HENLAGT_SØKNAD_TRUKKET;

        // Act
        henleggBehandlingTjeneste.henleggBehandling(behandling.getId(), behandlingsresultat, "begrunnelse");
    }

    @Test
    public void kan_ikke_henlegge_behandling_der_vedtak_er_fattet() {
        // Arrange
        Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET).buildFor(behandling);
        BehandlingResultatType behandlingsresultat = BehandlingResultatType.HENLAGT_SØKNAD_TRUKKET;
        forceOppdaterBehandlingSteg(behandling, IVERKSETT_VEDTAK);

        // Act
        assertThatThrownBy(() -> henleggBehandlingTjeneste.henleggBehandling(behandling.getId(), behandlingsresultat, "begrunnelse"))
                .hasMessageContaining("FP-143308");
    }

    @Test
    public void kan_ikke_henlegge_behandling_som_allerede_er_henlagt() {
        // Arrange
        Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.HENLAGT_FEILOPPRETTET).buildFor(behandling);
        BehandlingResultatType behandlingsresultat = BehandlingResultatType.HENLAGT_SØKNAD_TRUKKET;

        // Act
        assertThatThrownBy(() -> henleggBehandlingTjeneste.henleggBehandling(behandling.getId(), behandlingsresultat, "begrunnelse"))
                .hasMessageContaining("FP-143308");
    }

}
