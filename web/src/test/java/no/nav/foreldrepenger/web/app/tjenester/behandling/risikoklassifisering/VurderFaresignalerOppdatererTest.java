package no.nav.foreldrepenger.web.app.tjenester.behandling.risikoklassifisering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltVerdiType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagDel;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagFelt;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.behandlingslager.risikoklassifisering.FaresignalVurdering;
import no.nav.foreldrepenger.behandlingslager.risikoklassifisering.Kontrollresultat;
import no.nav.foreldrepenger.behandlingslager.risikoklassifisering.RisikoklassifiseringEntitet;
import no.nav.foreldrepenger.behandlingslager.risikoklassifisering.RisikoklassifiseringRepository;
import no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.RisikovurderingTjeneste;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

public class VurderFaresignalerOppdatererTest extends EntityManagerAwareTest {

    private RisikoklassifiseringRepository risikoklassifiseringRepository;

    private HistorikkTjenesteAdapter historikkAdapter;

    private Behandling behandling;

    private VurderFaresignalerOppdaterer vurderFaresignalerOppdaterer;

    @BeforeEach
    public void setup() {
        var entityManager = getEntityManager();
        var behandlingRepositoryProvider = new BehandlingRepositoryProvider(entityManager);
        historikkAdapter = new HistorikkTjenesteAdapter(behandlingRepositoryProvider.getHistorikkRepository(), null);
        var behandlingRepository = new BehandlingRepository(entityManager);
        risikoklassifiseringRepository = new RisikoklassifiseringRepository(entityManager);
        var risikovurderingTjeneste = new RisikovurderingTjeneste(risikoklassifiseringRepository, behandlingRepository,
            null, null, null);
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        behandling = scenario.lagre(behandlingRepositoryProvider);
        vurderFaresignalerOppdaterer = new VurderFaresignalerOppdaterer(risikovurderingTjeneste, historikkAdapter);
    }

    @Test
    public void skal_oppdatere_korrekt_ved_ingen_innvirkning() {
        // Arrange
        VurderFaresignalerDto dto = new VurderFaresignalerDto("Dustemikkel", false);
        risikoklassifiseringRepository.lagreRisikoklassifisering(
            lagRisikoklassifisering(Kontrollresultat.HØY, FaresignalVurdering.UDEFINERT), behandling.getId());

        // Act
        vurderFaresignalerOppdaterer.oppdater(dto,
            new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto));
        Optional<RisikoklassifiseringEntitet> oppdatertEntitet = risikoklassifiseringRepository.hentRisikoklassifiseringForBehandling(
            behandling.getId());

        // Assert
        assertThat(oppdatertEntitet).isPresent();
        assertThat(oppdatertEntitet.get().getFaresignalVurdering()).isEqualTo(FaresignalVurdering.INGEN_INNVIRKNING);
        assertThat(oppdatertEntitet.get().getKontrollresultat()).isEqualTo(Kontrollresultat.HØY);
    }

    @Test
    public void skal_oppdatere_korrekt_ved_har_hatt_innvirkning() {
        // Arrange
        VurderFaresignalerDto dto = new VurderFaresignalerDto("Dustemikkel", true);
        risikoklassifiseringRepository.lagreRisikoklassifisering(
            lagRisikoklassifisering(Kontrollresultat.HØY, FaresignalVurdering.UDEFINERT), behandling.getId());

        // Act
        vurderFaresignalerOppdaterer.oppdater(dto,
            new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto));
        Optional<RisikoklassifiseringEntitet> oppdatertEntitet = risikoklassifiseringRepository.hentRisikoklassifiseringForBehandling(
            behandling.getId());

        // Assert
        assertThat(oppdatertEntitet).isPresent();
        assertThat(oppdatertEntitet.get().getFaresignalVurdering()).isEqualTo(FaresignalVurdering.INNVIRKNING);
        assertThat(oppdatertEntitet.get().getKontrollresultat()).isEqualTo(Kontrollresultat.HØY);
    }

    @Test
    public void skal_lage_korrekt_historikkinnslag_når_det_ikke_finnes_tidligere_vurdering() {
        // Arrange
        VurderFaresignalerDto dto = new VurderFaresignalerDto("Dustemikkel", true);
        risikoklassifiseringRepository.lagreRisikoklassifisering(
            lagRisikoklassifisering(Kontrollresultat.HØY, FaresignalVurdering.UDEFINERT), behandling.getId());

        // Act
        vurderFaresignalerOppdaterer.oppdater(dto,
            new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto));

        // Assert
        HistorikkInnslagTekstBuilder tekstBuilder = historikkAdapter.tekstBuilder().ferdigstillHistorikkinnslagDel();
        List<HistorikkinnslagDel> deler = tekstBuilder.getHistorikkinnslagDeler();
        List<HistorikkinnslagFelt> historikkinnslagFelt = deler.get(0).getHistorikkinnslagFelt();

        assertThat(deler).hasSize(1);
        assertThat(historikkinnslagFelt).hasSize(3);

        Optional<HistorikkinnslagFelt> faresignaler = historikkinnslagFelt.stream()
            .filter(he -> he.getNavn().equals(HistorikkEndretFeltType.FARESIGNALER.getKode()))
            .findFirst();

        assertThat(faresignaler).isPresent();
        assertThat(faresignaler.get().getTilVerdi()).isEqualTo(HistorikkEndretFeltVerdiType.INNVIRKNING.getKode());
        assertThat(faresignaler.get().getFraVerdi()).isNull();
    }

    @Test
    public void skal_lage_korrekt_historikkinnslag_når_det_finnes_tidligere_vurdering_ingen_innvirkning() {
        // Arrange
        VurderFaresignalerDto dto = new VurderFaresignalerDto("Dustemikkel", true);
        risikoklassifiseringRepository.lagreRisikoklassifisering(
            lagRisikoklassifisering(Kontrollresultat.HØY, FaresignalVurdering.INGEN_INNVIRKNING), behandling.getId());

        // Act
        vurderFaresignalerOppdaterer.oppdater(dto,
            new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto));

        // Assert
        HistorikkInnslagTekstBuilder tekstBuilder = historikkAdapter.tekstBuilder().ferdigstillHistorikkinnslagDel();
        List<HistorikkinnslagDel> deler = tekstBuilder.getHistorikkinnslagDeler();
        List<HistorikkinnslagFelt> historikkinnslagFelt = deler.get(0).getHistorikkinnslagFelt();

        assertThat(deler).hasSize(1);
        assertThat(historikkinnslagFelt).hasSize(3);

        Optional<HistorikkinnslagFelt> faresignaler = historikkinnslagFelt.stream()
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
        VurderFaresignalerDto dto = new VurderFaresignalerDto("Dustemikkel", false);
        risikoklassifiseringRepository.lagreRisikoklassifisering(
            lagRisikoklassifisering(Kontrollresultat.HØY, FaresignalVurdering.INNVIRKNING), behandling.getId());

        // Act
        vurderFaresignalerOppdaterer.oppdater(dto,
            new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto));

        // Assert
        HistorikkInnslagTekstBuilder tekstBuilder = historikkAdapter.tekstBuilder().ferdigstillHistorikkinnslagDel();
        List<HistorikkinnslagDel> deler = tekstBuilder.getHistorikkinnslagDeler();
        List<HistorikkinnslagFelt> historikkinnslagFelt = deler.get(0).getHistorikkinnslagFelt();

        assertThat(deler).hasSize(1);
        assertThat(historikkinnslagFelt).hasSize(3);

        Optional<HistorikkinnslagFelt> faresignaler = historikkinnslagFelt.stream()
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
        VurderFaresignalerDto dto = new VurderFaresignalerDto("Dustemikkel", true);

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
