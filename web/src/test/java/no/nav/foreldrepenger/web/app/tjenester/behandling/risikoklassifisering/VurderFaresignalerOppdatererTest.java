package no.nav.foreldrepenger.web.app.tjenester.behandling.risikoklassifisering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltVerdiType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.risikoklassifisering.FaresignalVurdering;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.FpriskTjeneste;
import no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.RisikovurderingTjeneste;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.kontrakter.risk.kodeverk.RisikoklasseType;
import no.nav.foreldrepenger.kontrakter.risk.v1.RisikovurderingResultatDto;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@ExtendWith(MockitoExtension.class)
class VurderFaresignalerOppdatererTest extends EntityManagerAwareTest {

    private HistorikkTjenesteAdapter historikkAdapter;

    private Behandling behandling;

    private VurderFaresignalerOppdaterer vurderFaresignalerOppdaterer;

    @Mock
    private FpriskTjeneste fpriskTjeneste;
    @Mock
    private ProsessTaskTjeneste prosessTaskTjeneste;

    @BeforeEach
    public void setup() {
        var entityManager = getEntityManager();
        var behandlingRepositoryProvider = new BehandlingRepositoryProvider(entityManager);
        historikkAdapter = new HistorikkTjenesteAdapter(behandlingRepositoryProvider.getHistorikkRepository(), null,
                behandlingRepositoryProvider.getBehandlingRepository());
        var behandlingRepository = new BehandlingRepository(entityManager);
        var risikovurderingTjeneste = new RisikovurderingTjeneste(fpriskTjeneste, prosessTaskTjeneste);
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        behandling = scenario.lagre(behandlingRepositoryProvider);
        vurderFaresignalerOppdaterer = new VurderFaresignalerOppdaterer(risikovurderingTjeneste, historikkAdapter, behandlingRepository);
    }

    @Test
    void skal_lage_korrekt_historikkinnslag_når_det_ikke_finnes_tidligere_vurdering() {
        // Arrange
        when(fpriskTjeneste.hentFaresignalerForBehandling(any())).thenReturn(Optional.of(lagRespons(null)));
        var dto = new VurderFaresignalerDto("Dustemikkel", FaresignalVurdering.INNVILGET_UENDRET);

        // Act
        vurderFaresignalerOppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto));

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
    void skal_lage_korrekt_historikkinnslag_når_det_finnes_tidligere_vurdering_ingen_innvirkning() {
        // Arrange
        when(fpriskTjeneste.hentFaresignalerForBehandling(any())).thenReturn(Optional.of(lagRespons(no.nav.foreldrepenger.kontrakter.risk.kodeverk.FaresignalVurdering.INGEN_INNVIRKNING)));
        var dto = new VurderFaresignalerDto("Dustemikkel", FaresignalVurdering.INNVILGET_REDUSERT);

        // Act
        vurderFaresignalerOppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto));

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
    void skal_lage_korrekt_historikkinnslag_når_det_finnes_tidligere_vurdering_innvirkning() {
        // Arrange
        when(fpriskTjeneste.hentFaresignalerForBehandling(any())).thenReturn(Optional.of(lagRespons(no.nav.foreldrepenger.kontrakter.risk.kodeverk.FaresignalVurdering.INNVIRKNING)));
        var dto = new VurderFaresignalerDto("Dustemikkel", FaresignalVurdering.INGEN_INNVIRKNING);

        // Act
        vurderFaresignalerOppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto));

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
    void skal_feile_om_det_ikke_finnes_en_risikoklassifisering_for_behandlingen() {
        // Arrange
        when(fpriskTjeneste.hentFaresignalerForBehandling(any())).thenReturn(Optional.empty());
        var dto = new VurderFaresignalerDto("Dustemikkel", FaresignalVurdering.AVSLAG_FARESIGNAL);

        // Act
        var param = new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto);
        assertThrows(IllegalStateException.class, () -> vurderFaresignalerOppdaterer.oppdater(dto, param));
    }

    private RisikovurderingResultatDto lagRespons(no.nav.foreldrepenger.kontrakter.risk.kodeverk.FaresignalVurdering faresignalVurdering) {
        return new RisikovurderingResultatDto(RisikoklasseType.HØY, null, null, faresignalVurdering);
    }

}
