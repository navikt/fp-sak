package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltVerdiType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app.FaktaOmsorgRettTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.AvklarAleneomsorgVurderingDto;

class BekreftAleneomsorgOppdatererTest extends EntityManagerAwareTest {
    private static final AksjonspunktDefinisjon AKSJONSPUNKT_DEF = AksjonspunktDefinisjon.MANUELL_KONTROLL_AV_OM_BRUKER_HAR_ALENEOMSORG;

    private final HistorikkInnslagTekstBuilder tekstBuilder = new HistorikkInnslagTekstBuilder();

    private BehandlingRepositoryProvider behandlingRepositoryProvider;
    private YtelseFordelingTjeneste ytelseFordelingTjeneste;

    @BeforeEach
    void setUp() {
        var entityManager = getEntityManager();
        behandlingRepositoryProvider = new BehandlingRepositoryProvider(entityManager);
        ytelseFordelingTjeneste = new YtelseFordelingTjeneste(new YtelsesFordelingRepository(entityManager));
    }

    @Test
    void bekrefter_aleneomsorg_ikke_totrinn() {
        // Arrange
        var oppdatertAleneOmsorg = true;

        // Behandling
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();

        scenario.medSøknad();
        var rettighet = OppgittRettighetEntitet.aleneomsorg();
        scenario.medOppgittRettighet(rettighet);
        scenario.leggTilAksjonspunkt(AKSJONSPUNKT_DEF, BehandlingStegType.VURDER_UTTAK);

        scenario.lagre(behandlingRepositoryProvider);

        var behandling = scenario.getBehandling();
        // Dto
        var dto = new AvklarAleneomsorgVurderingDto("begrunnelse");
        dto.setAleneomsorg(oppdatertAleneOmsorg);
        dto.setAnnenforelderHarRett(true);
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getAksjonspunktDefinisjon());
        // Act
        var oppdateringresultat = new BekreftAleneomsorgOppdaterer(new FaktaOmsorgRettTjeneste(ytelseFordelingTjeneste, lagMockHistory()))
            .oppdater(dto, new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto, aksjonspunkt));
        var historikkinnslag = new Historikkinnslag();
        historikkinnslag.setType(HistorikkinnslagType.FAKTA_ENDRET);
        var historikkinnslagDeler = tekstBuilder.build(historikkinnslag);

        // Assert
        assertThat(oppdateringresultat.kreverTotrinnsKontroll()).isFalse();
        assertThat(historikkinnslagDeler).hasSize(1);
        var del = historikkinnslagDeler.get(0);
        var aleneomsorgOpt = del.getEndretFelt(HistorikkEndretFeltType.ALENEOMSORG);
        assertThat(aleneomsorgOpt).hasValueSatisfying(aleneomsorg -> {
            assertThat(aleneomsorg.getNavn()).isEqualTo(HistorikkEndretFeltType.ALENEOMSORG.getKode());
            assertThat(aleneomsorg.getFraVerdi()).isNull();
            assertThat(aleneomsorg.getTilVerdi()).isEqualTo(HistorikkEndretFeltVerdiType.ALENEOMSORG.getKode());
        });
    }

    @Test
    void skal_generere_historikkinnslag_ved_avklaring_av_aleneomsorg() {
        // Arrange
        var oppdatertAleneOmsorg = false;

        // Behandling
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();

        scenario.medSøknad();
        var rettighet = OppgittRettighetEntitet.aleneomsorg();
        scenario.medOppgittRettighet(rettighet);
        scenario.leggTilAksjonspunkt(AKSJONSPUNKT_DEF, BehandlingStegType.VURDER_UTTAK);

        scenario.lagre(behandlingRepositoryProvider);

        var behandling = scenario.getBehandling();
        // Dto
        var dto = new AvklarAleneomsorgVurderingDto("begrunnelse");
        dto.setAleneomsorg(oppdatertAleneOmsorg);
        dto.setAnnenforelderHarRett(true);
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getAksjonspunktDefinisjon());
        // Act
        var oppdateringresultat = new BekreftAleneomsorgOppdaterer(new FaktaOmsorgRettTjeneste(ytelseFordelingTjeneste, lagMockHistory()))
            .oppdater(dto, new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto, aksjonspunkt));
        var historikkinnslag = new Historikkinnslag();
        historikkinnslag.setType(HistorikkinnslagType.FAKTA_ENDRET);
        var historikkinnslagDeler = tekstBuilder.build(historikkinnslag);

        // Assert
        assertThat(oppdateringresultat.kreverTotrinnsKontroll()).isTrue();
        assertThat(historikkinnslagDeler).hasSize(1);
        var del = historikkinnslagDeler.get(0);
        var aleneomsorgOpt = del.getEndretFelt(HistorikkEndretFeltType.ALENEOMSORG);
        assertThat(aleneomsorgOpt).hasValueSatisfying(aleneomsorg -> {
            assertThat(aleneomsorg.getNavn()).isEqualTo(HistorikkEndretFeltType.ALENEOMSORG.getKode());
            assertThat(aleneomsorg.getFraVerdi()).isNull();
            assertThat(aleneomsorg.getTilVerdi()).isEqualTo(HistorikkEndretFeltVerdiType.IKKE_ALENEOMSORG.getKode());
        });
    }

    @Test
    void skal_generere_dobbelt_historikkinnslag_ved_avklaring_av_aleneomsorg_og_ikke_rett() {
        // Arrange
        var oppdatertAleneOmsorg = false;

        // Behandling
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();

        scenario.medSøknad();
        var rettighet = OppgittRettighetEntitet.aleneomsorg();
        scenario.medOppgittRettighet(rettighet);
        scenario.leggTilAksjonspunkt(AKSJONSPUNKT_DEF, BehandlingStegType.VURDER_UTTAK);

        scenario.lagre(behandlingRepositoryProvider);

        var behandling = scenario.getBehandling();
        // Dto
        var dto = new AvklarAleneomsorgVurderingDto("begrunnelse");
        dto.setAleneomsorg(oppdatertAleneOmsorg);
        dto.setAnnenforelderHarRett(true);
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getAksjonspunktDefinisjon());
        // Act
        var oppdateringresultat = new BekreftAleneomsorgOppdaterer(new FaktaOmsorgRettTjeneste(ytelseFordelingTjeneste, lagMockHistory()))
            .oppdater(dto, new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto, aksjonspunkt));
        var historikkinnslag = new Historikkinnslag();
        historikkinnslag.setType(HistorikkinnslagType.FAKTA_ENDRET);
        var historikkinnslagDeler = tekstBuilder.build(historikkinnslag);

        // Assert
        assertThat(oppdateringresultat.kreverTotrinnsKontroll()).isTrue();
        assertThat(historikkinnslagDeler).hasSize(1);
        assertThat(historikkinnslagDeler.get(0).getEndredeFelt()).hasSize(2);
        var del = historikkinnslagDeler.get(0);
        var aleneomsorgOpt = del.getEndretFelt(HistorikkEndretFeltType.ALENEOMSORG);
        assertThat(aleneomsorgOpt).hasValueSatisfying(aleneomsorg -> {
            assertThat(aleneomsorg.getNavn()).isEqualTo(HistorikkEndretFeltType.ALENEOMSORG.getKode());
            assertThat(aleneomsorg.getFraVerdi()).isNull();
            assertThat(aleneomsorg.getTilVerdi()).isEqualTo(HistorikkEndretFeltVerdiType.IKKE_ALENEOMSORG.getKode());
        });

        var annenforelderRettOpt = del.getEndretFelt(HistorikkEndretFeltType.RETT_TIL_FORELDREPENGER);
        assertThat(annenforelderRettOpt).hasValueSatisfying(annenforelderRett -> {
            assertThat(annenforelderRett.getNavn()).isEqualTo(HistorikkEndretFeltType.RETT_TIL_FORELDREPENGER.getKode());
            assertThat(annenforelderRett.getFraVerdi()).isNull();
            assertThat(annenforelderRett.getTilVerdi()).isEqualTo(HistorikkEndretFeltVerdiType.ANNEN_FORELDER_HAR_RETT.getKode());
        });
    }

    @Test
    void skal_generere_trippelt_historikkinnslag_ved_avklaring_av_aleneomsorg_og_ikke_rett() {
        // Arrange
        var annenpart = AktørId.dummy();
        var oppdatertAleneOmsorg = false;

        // Behandling
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();

        scenario.medSøknad();
        var rettighet = OppgittRettighetEntitet.aleneomsorg();
        scenario.medOppgittRettighet(rettighet);
        scenario.medSøknadAnnenPart().medAktørId(annenpart);
        scenario.leggTilAksjonspunkt(AKSJONSPUNKT_DEF, BehandlingStegType.VURDER_UTTAK);

        scenario.lagre(behandlingRepositoryProvider);

        var behandling = scenario.getBehandling();
        // Dto
        var dto = new AvklarAleneomsorgVurderingDto("begrunnelse");
        dto.setAleneomsorg(oppdatertAleneOmsorg);
        dto.setAnnenforelderHarRett(false);
        dto.setAnnenforelderMottarUføretrygd(true);
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getAksjonspunktDefinisjon());
        // Act
        var oppdateringresultat = new BekreftAleneomsorgOppdaterer(
            new FaktaOmsorgRettTjeneste(ytelseFordelingTjeneste, lagMockHistory()))
            .oppdater(dto, new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto, aksjonspunkt));
        var historikkinnslag = new Historikkinnslag();
        historikkinnslag.setType(HistorikkinnslagType.FAKTA_ENDRET);
        var historikkinnslagDeler = tekstBuilder.build(historikkinnslag);

        // Assert
        assertThat(oppdateringresultat.kreverTotrinnsKontroll()).isTrue();

        assertThat(ytelseFordelingTjeneste.hentAggregat(behandling.getId()).getMorUføretrygdAvklaring()).isTrue();

        assertThat(historikkinnslagDeler).hasSize(1);
        assertThat(historikkinnslagDeler.get(0).getEndredeFelt()).hasSize(3);
        var del = historikkinnslagDeler.get(0);
        var aleneomsorgOpt = del.getEndretFelt(HistorikkEndretFeltType.ALENEOMSORG);
        assertThat(aleneomsorgOpt).hasValueSatisfying(aleneomsorg -> {
            assertThat(aleneomsorg.getNavn()).isEqualTo(HistorikkEndretFeltType.ALENEOMSORG.getKode());
            assertThat(aleneomsorg.getFraVerdi()).isNull();
            assertThat(aleneomsorg.getTilVerdi()).isEqualTo(HistorikkEndretFeltVerdiType.IKKE_ALENEOMSORG.getKode());
        });

        var annenforelderRettOpt = del.getEndretFelt(HistorikkEndretFeltType.RETT_TIL_FORELDREPENGER);
        assertThat(annenforelderRettOpt).hasValueSatisfying(annenforelderRett -> {
            assertThat(annenforelderRett.getNavn()).isEqualTo(HistorikkEndretFeltType.RETT_TIL_FORELDREPENGER.getKode());
            assertThat(annenforelderRett.getFraVerdi()).isNull();
            assertThat(annenforelderRett.getTilVerdi()).isEqualTo(HistorikkEndretFeltVerdiType.ANNEN_FORELDER_HAR_IKKE_RETT.getKode());
        });

        var uføretrygdOpt = del.getEndretFelt(HistorikkEndretFeltType.MOR_MOTTAR_UFØRETRYGD);
        assertThat(uføretrygdOpt).hasValueSatisfying(uføretrygd -> {
            assertThat(uføretrygd.getNavn()).isEqualTo(HistorikkEndretFeltType.MOR_MOTTAR_UFØRETRYGD.getKode());
            assertThat(uføretrygd.getFraVerdi()).isNull();
            assertThat(uføretrygd.getTilVerdi()).isEqualTo(Boolean.TRUE.toString());
        });
    }

    private HistorikkTjenesteAdapter lagMockHistory() {
        var mockHistory = mock(HistorikkTjenesteAdapter.class);
        Mockito.when(mockHistory.tekstBuilder()).thenReturn(tekstBuilder);
        return mockHistory;
    }
}
