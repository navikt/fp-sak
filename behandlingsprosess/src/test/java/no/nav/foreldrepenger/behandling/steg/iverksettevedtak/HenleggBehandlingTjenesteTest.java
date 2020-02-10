package no.nav.foreldrepenger.behandling.steg.iverksettevedtak;

import static no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType.IVERKSETT_VEDTAK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingModell;
import no.nav.foreldrepenger.behandlingskontroll.impl.BehandlingModellRepository;
import no.nav.foreldrepenger.behandlingskontroll.impl.BehandlingskontrollTjenesteImpl;
import no.nav.foreldrepenger.behandlingskontroll.spi.BehandlingskontrollServiceProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.InternalManipulerBehandling;
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
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestillerApplikasjonTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.dto.BestillBrevDto;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class HenleggBehandlingTjenesteTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    @Inject
    private InternalManipulerBehandling manipulerInternBehandling;

    private BehandlingRepositoryProvider repositoryProvider;

    @Mock
    private HistorikkRepository historikkRepositoryMock;

    @Mock
    private DokumentBestillerApplikasjonTjeneste dokumentBestillerApplikasjonTjenesteMock;

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

    @Before
    public void setUp() {
        System.setProperty("systembruker.username", "brukerident");

        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        behandling = scenario.lagMocked();
        repositoryProvider = scenario.mockBehandlingRepositoryProvider();

        manipulerInternBehandling.forceOppdaterBehandlingSteg(behandling, BehandlingStegType.INNHENT_SØKNADOPP);

        when(repositoryProvider.getHistorikkRepository()).thenReturn(historikkRepositoryMock);

        var serviceProvider = new BehandlingskontrollServiceProvider(
            repositoryProvider.getFagsakRepository(),
            repositoryProvider.getBehandlingRepository(),
            repositoryProvider.getFagsakLåsRepository(),
            repositoryProvider.getBehandlingLåsRepository(),
            behandlingModellRepository,
            aksjonspunktKontrollRepository);

        BehandlingskontrollTjenesteImpl behandlingskontrollTjenesteImpl = new BehandlingskontrollTjenesteImpl(serviceProvider);
        when(modell.erStegAFørStegB(any(), any())).thenReturn(true);

        henleggBehandlingTjeneste = new HenleggBehandlingTjeneste(repositoryProvider,
            behandlingskontrollTjenesteImpl,
            dokumentBestillerApplikasjonTjenesteMock, prosessTaskRepositoryMock);
    }

    @Test
    public void skal_henlegge_behandling_med_brev() throws Exception {
        // Arrange
        BehandlingResultatType behandlingsresultat = BehandlingResultatType.HENLAGT_SØKNAD_TRUKKET;

        // Act
        henleggBehandlingTjeneste.henleggBehandling(behandling.getId(), behandlingsresultat, "begrunnelse");

        // Assert
        verify(historikkRepositoryMock).lagre(any(Historikkinnslag.class));
        verify(repositoryProvider.getBehandlingRepository(), atLeast(2)).lagre(eq(behandling), any(BehandlingLås.class));
        verify(dokumentBestillerApplikasjonTjenesteMock).bestillDokument(any(BestillBrevDto.class), eq(HistorikkAktør.VEDTAKSLØSNINGEN), eq(false));
    }

    @Test
    public void skal_henlegge_behandling_uten_brev() throws Exception {
        // Arrange
        BehandlingResultatType behandlingsresultat = BehandlingResultatType.HENLAGT_FEILOPPRETTET;

        // Act
        henleggBehandlingTjeneste.henleggBehandling(behandling.getId(), behandlingsresultat, "begrunnelse");

        // Assert
        verify(historikkRepositoryMock).lagre(any(Historikkinnslag.class));
        verify(repositoryProvider.getBehandlingRepository(), atLeast(2)).lagre(eq(behandling), any(BehandlingLås.class));
        verify(dokumentBestillerApplikasjonTjenesteMock, never()).bestillDokument(any(), any(), anyBoolean());
    }

    @Test
    public void skal_henlegge_behandling_med_aksjonspunkt() throws Exception {
        // Arrange
        BehandlingResultatType behandlingsresultat = BehandlingResultatType.HENLAGT_FEILOPPRETTET;
        Aksjonspunkt aksjonspunkt = new AksjonspunktTestSupport().leggTilAksjonspunkt(behandling,
            AksjonspunktDefinisjon.SJEKK_MANGLENDE_FØDSEL);
        assertThat(aksjonspunkt.getStatus()).isEqualTo(AksjonspunktStatus.OPPRETTET);

        // Act
        henleggBehandlingTjeneste.henleggBehandling(behandling.getId(), behandlingsresultat, "begrunnelse");

        // Assert
        verify(historikkRepositoryMock).lagre(any(Historikkinnslag.class));
        verify(repositoryProvider.getBehandlingRepository(), atLeastOnce()).lagre(eq(behandling), any(BehandlingLås.class));
        verify(dokumentBestillerApplikasjonTjenesteMock, never()).bestillDokument(any(), any(), anyBoolean());
        assertThat(aksjonspunkt.getStatus()).isEqualTo(AksjonspunktStatus.AVBRUTT);
    }

    @Test
    public void skal_henlegge_behandling_ved_dødsfall() throws Exception {
        // Arrange
        BehandlingResultatType behandlingsresultat = BehandlingResultatType.HENLAGT_BRUKER_DØD;

        // Act
        henleggBehandlingTjeneste.henleggBehandling(behandling.getId(), behandlingsresultat, "begrunnelse");

        // Assert
        verify(historikkRepositoryMock).lagre(any(Historikkinnslag.class));
        verify(repositoryProvider.getBehandlingRepository(), atLeast(2)).lagre(eq(behandling), any(BehandlingLås.class));
        verify(dokumentBestillerApplikasjonTjenesteMock, never()).bestillDokument(any(), any(), anyBoolean());
    }

    @Test
    public void kan_henlegge_behandling_som_er_satt_på_vent() throws Exception {
        // Arrange
        AksjonspunktDefinisjon def = AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT;
        Aksjonspunkt aksjonspunkt = new AksjonspunktTestSupport().leggTilAksjonspunkt(behandling, def);
        new AksjonspunktTestSupport().setFrist(aksjonspunkt, LocalDateTime.now(), null);

        manipulerInternBehandling.forceOppdaterBehandlingSteg(behandling, BehandlingStegType.INNHENT_SØKNADOPP);

        BehandlingResultatType behandlingsresultat = BehandlingResultatType.HENLAGT_SØKNAD_TRUKKET;

        // Act
        henleggBehandlingTjeneste.henleggBehandling(behandling.getId(), behandlingsresultat, "begrunnelse");
    }

    @Test
    public void kan_henlegge_behandling_der_vedtak_er_foreslått() throws Exception {
        // Arrange
        Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET).buildFor(behandling);
        BehandlingResultatType behandlingsresultat = BehandlingResultatType.HENLAGT_SØKNAD_TRUKKET;

        // Act
        henleggBehandlingTjeneste.henleggBehandling(behandling.getId(), behandlingsresultat, "begrunnelse");
    }

    @Test
    public void kan_ikke_henlegge_behandling_der_vedtak_er_fattet() throws Exception {
        // Arrange
        Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET).buildFor(behandling);
        BehandlingResultatType behandlingsresultat = BehandlingResultatType.HENLAGT_SØKNAD_TRUKKET;
        manipulerInternBehandling.forceOppdaterBehandlingSteg(behandling, IVERKSETT_VEDTAK);

        // Act
        try {
            henleggBehandlingTjeneste.henleggBehandling(behandling.getId(), behandlingsresultat, "begrunnelse");
            Assert.fail("Forventet exception");
        } catch (Exception ex) {
            assertThat(ex.getMessage()).contains("FP-143308");
        }
    }

    @Test
    public void kan_ikke_henlegge_behandling_som_allerede_er_henlagt() throws Exception {
        // Arrange
        Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.HENLAGT_FEILOPPRETTET).buildFor(behandling);
        BehandlingResultatType behandlingsresultat = BehandlingResultatType.HENLAGT_SØKNAD_TRUKKET;

        // Act
        try {
            henleggBehandlingTjeneste.henleggBehandling(behandling.getId(), behandlingsresultat, "begrunnelse");
            Assert.fail("Forventet exception");
        } catch (Exception ex) {
            assertThat(ex.getMessage()).contains("FP-143308");
        }
    }

}
