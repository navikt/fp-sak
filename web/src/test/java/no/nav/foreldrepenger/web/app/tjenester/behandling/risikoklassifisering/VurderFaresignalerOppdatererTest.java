package no.nav.foreldrepenger.web.app.tjenester.behandling.risikoklassifisering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;

import java.util.Optional;

import no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.FpriskTjeneste;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltVerdiType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.risikoklassifisering.FaresignalVurdering;
import no.nav.foreldrepenger.behandlingslager.risikoklassifisering.Kontrollresultat;
import no.nav.foreldrepenger.behandlingslager.risikoklassifisering.RisikoklassifiseringEntitet;
import no.nav.foreldrepenger.behandlingslager.risikoklassifisering.RisikoklassifiseringRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.RisikovurderingTjeneste;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class VurderFaresignalerOppdatererTest extends EntityManagerAwareTest {

    private RisikoklassifiseringRepository risikoklassifiseringRepository;

    private HistorikkTjenesteAdapter historikkAdapter;

    private Behandling behandling;

    private VurderFaresignalerOppdaterer vurderFaresignalerOppdaterer;

    @Mock
    private FpriskTjeneste fpriskTjeneste;

    @BeforeEach
    public void setup() {
        var entityManager = getEntityManager();
        var behandlingRepositoryProvider = new BehandlingRepositoryProvider(entityManager);
        historikkAdapter = new HistorikkTjenesteAdapter(behandlingRepositoryProvider.getHistorikkRepository(), null,
                behandlingRepositoryProvider.getBehandlingRepository());
        var behandlingRepository = new BehandlingRepository(entityManager);
        risikoklassifiseringRepository = new RisikoklassifiseringRepository(entityManager);
        var risikovurderingTjeneste = new RisikovurderingTjeneste(risikoklassifiseringRepository,
            behandlingRepository, fpriskTjeneste, null);
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        behandling = scenario.lagre(behandlingRepositoryProvider);
        vurderFaresignalerOppdaterer = new VurderFaresignalerOppdaterer(risikovurderingTjeneste, historikkAdapter, behandlingRepository);
    }

    @Test
    public void skal_oppdatere_korrekt_ved_ingen_innvirkning() {
        // Arrange
        var dto = new VurderFaresignalerDto("Dustemikkel", FaresignalVurdering.INGEN_INNVIRKNING);
        risikoklassifiseringRepository.lagreRisikoklassifisering(
            lagRisikoklassifisering(Kontrollresultat.HØY, FaresignalVurdering.UDEFINERT), behandling.getId());

        // Act
        vurderFaresignalerOppdaterer.oppdater(dto,
            new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto));
        var oppdatertEntitet = risikoklassifiseringRepository.hentRisikoklassifiseringForBehandling(
            behandling.getId());

        // Assert
        assertThat(oppdatertEntitet).isPresent();
        assertThat(oppdatertEntitet.get().getFaresignalVurdering()).isEqualTo(FaresignalVurdering.INGEN_INNVIRKNING);
        assertThat(oppdatertEntitet.get().getKontrollresultat()).isEqualTo(Kontrollresultat.HØY);
    }

    @Test
    public void skal_oppdatere_korrekt_ved_har_hatt_innvirkning() {
        // Arrange
        var dto = new VurderFaresignalerDto("Dustemikkel", FaresignalVurdering.AVSLAG_FARESIGNAL);
        risikoklassifiseringRepository.lagreRisikoklassifisering(
            lagRisikoklassifisering(Kontrollresultat.HØY, FaresignalVurdering.UDEFINERT), behandling.getId());

        // Act
        vurderFaresignalerOppdaterer.oppdater(dto,
            new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto));
        var oppdatertEntitet = risikoklassifiseringRepository.hentRisikoklassifiseringForBehandling(
            behandling.getId());

        // Assert
        assertThat(oppdatertEntitet).isPresent();
        assertThat(oppdatertEntitet.get().getFaresignalVurdering()).isEqualTo(FaresignalVurdering.AVSLAG_FARESIGNAL);
        assertThat(oppdatertEntitet.get().getKontrollresultat()).isEqualTo(Kontrollresultat.HØY);
    }

    @Test
    public void skal_lage_korrekt_historikkinnslag_når_det_ikke_finnes_tidligere_vurdering() {
        // Arrange
        var dto = new VurderFaresignalerDto("Dustemikkel", FaresignalVurdering.INNVILGET_UENDRET);
        risikoklassifiseringRepository.lagreRisikoklassifisering(
            lagRisikoklassifisering(Kontrollresultat.HØY, FaresignalVurdering.UDEFINERT), behandling.getId());

        // Act
        vurderFaresignalerOppdaterer.oppdater(dto,
            new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto));

        // Assert
        var tekstBuilder = historikkAdapter.tekstBuilder().ferdigstillHistorikkinnslagDel();
        var deler = tekstBuilder.getHistorikkinnslagDeler();
        var historikkinnslagFelt = deler.get(0).getHistorikkinnslagFelt();

        assertThat(deler).hasSize(1);
        assertThat(historikkinnslagFelt).hasSize(3);

        var faresignaler = historikkinnslagFelt.stream()
            .filter(he -> he.getNavn().equals(HistorikkEndretFeltType.FARESIGNALER.getKode()))
            .findFirst();

        assertThat(faresignaler).isPresent();
        assertThat(faresignaler.get().getTilVerdi()).isEqualTo(HistorikkEndretFeltVerdiType.INNVIRKNING.getKode());
        assertThat(faresignaler.get().getFraVerdi()).isNull();
    }

    @Test
    public void skal_lage_korrekt_historikkinnslag_når_det_finnes_tidligere_vurdering_ingen_innvirkning() {
        // Arrange
        var dto = new VurderFaresignalerDto("Dustemikkel", FaresignalVurdering.INNVILGET_REDUSERT);
        risikoklassifiseringRepository.lagreRisikoklassifisering(
            lagRisikoklassifisering(Kontrollresultat.HØY, FaresignalVurdering.INGEN_INNVIRKNING), behandling.getId());

        // Act
        vurderFaresignalerOppdaterer.oppdater(dto,
            new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto));

        // Assert
        var tekstBuilder = historikkAdapter.tekstBuilder().ferdigstillHistorikkinnslagDel();
        var deler = tekstBuilder.getHistorikkinnslagDeler();
        var historikkinnslagFelt = deler.get(0).getHistorikkinnslagFelt();

        assertThat(deler).hasSize(1);
        assertThat(historikkinnslagFelt).hasSize(3);

        var faresignaler = historikkinnslagFelt.stream()
            .filter(he -> he.getNavn().equals(HistorikkEndretFeltType.FARESIGNALER.getKode()))
            .findFirst();

        assertThat(faresignaler).isPresent();
        assertThat(faresignaler.get().getTilVerdi()).isEqualTo(HistorikkEndretFeltVerdiType.INNVIRKNING.getKode());
        assertThat(faresignaler.get().getFraVerdi()).isEqualTo(
            HistorikkEndretFeltVerdiType.INGEN_INNVIRKNING.getKode());
    }

    @Test
    public void skal_lage_korrekt_historikkinnslag_når_det_finnes_tidligere_vurdering_innvirkning() {
        // Arrange
        var dto = new VurderFaresignalerDto("Dustemikkel", FaresignalVurdering.INGEN_INNVIRKNING);
        risikoklassifiseringRepository.lagreRisikoklassifisering(
            lagRisikoklassifisering(Kontrollresultat.HØY, FaresignalVurdering.INNVIRKNING), behandling.getId());

        // Act
        vurderFaresignalerOppdaterer.oppdater(dto,
            new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto));

        // Assert
        var tekstBuilder = historikkAdapter.tekstBuilder().ferdigstillHistorikkinnslagDel();
        var deler = tekstBuilder.getHistorikkinnslagDeler();
        var historikkinnslagFelt = deler.get(0).getHistorikkinnslagFelt();

        assertThat(deler).hasSize(1);
        assertThat(historikkinnslagFelt).hasSize(3);

        var faresignaler = historikkinnslagFelt.stream()
            .filter(he -> he.getNavn().equals(HistorikkEndretFeltType.FARESIGNALER.getKode()))
            .findFirst();

        assertThat(faresignaler).isPresent();
        assertThat(faresignaler.get().getTilVerdi()).isEqualTo(
            HistorikkEndretFeltVerdiType.INGEN_INNVIRKNING.getKode());
        assertThat(faresignaler.get().getFraVerdi()).isEqualTo(HistorikkEndretFeltVerdiType.INNVIRKNING.getKode());
    }

    @Test
    public void skal_feile_om_det_ikke_finnes_en_risikoklassifisering_for_behandlingen() {
        // Arrange
        var dto = new VurderFaresignalerDto("Dustemikkel", FaresignalVurdering.AVSLAG_FARESIGNAL);

        // Act
        assertThrows(IllegalStateException.class, () -> vurderFaresignalerOppdaterer.oppdater(dto,
            new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto)));
    }

    private RisikoklassifiseringEntitet lagRisikoklassifisering(Kontrollresultat kontrollresultat,
                                                                FaresignalVurdering faresignalVurdering) {
        return RisikoklassifiseringEntitet.builder()
            .medKontrollresultat(kontrollresultat)
            .medFaresignalVurdering(faresignalVurdering)
            .buildFor(behandling.getId());
    }

}
