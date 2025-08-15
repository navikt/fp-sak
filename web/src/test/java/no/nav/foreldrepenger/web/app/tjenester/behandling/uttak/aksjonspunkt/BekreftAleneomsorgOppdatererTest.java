package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakEgenskapRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app.FaktaOmsorgRettTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.AvklarAleneomsorgVurderingDto;

class BekreftAleneomsorgOppdatererTest extends EntityManagerAwareTest {
    private static final AksjonspunktDefinisjon AKSJONSPUNKT_DEF = AksjonspunktDefinisjon.MANUELL_KONTROLL_AV_OM_BRUKER_HAR_ALENEOMSORG;

    private BehandlingRepositoryProvider repositoryProvider;
    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private HistorikkinnslagRepository historikkRepository;

    @BeforeEach
    void setUp() {
        var entityManager = getEntityManager();
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        ytelseFordelingTjeneste = new YtelseFordelingTjeneste(new YtelsesFordelingRepository(entityManager));
        historikkRepository = repositoryProvider.getHistorikkinnslagRepository();
    }

    @Test
    void bekrefter_aleneomsorg_ikke_totrinn() {
        // Arrange

        // Behandling
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();

        scenario.medSøknad();
        var rettighet = OppgittRettighetEntitet.aleneomsorg();
        scenario.medOppgittRettighet(rettighet);
        scenario.leggTilAksjonspunkt(AKSJONSPUNKT_DEF, BehandlingStegType.VURDER_UTTAK);

        scenario.lagre(repositoryProvider);

        var behandling = scenario.getBehandling();
        // Dto
        var dto = new AvklarAleneomsorgVurderingDto("begrunnelse.");
        dto.setAleneomsorg(true);
        dto.setAnnenforelderHarRett(true);
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getAksjonspunktDefinisjon());
        // Act
        var oppdateringresultat = new BekreftAleneomsorgOppdaterer(
            new FaktaOmsorgRettTjeneste(ytelseFordelingTjeneste, mock(FagsakEgenskapRepository.class)), historikkRepository).oppdater(dto,
            new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto, aksjonspunkt));

        // Assert
        assertThat(oppdateringresultat.kreverTotrinnsKontroll()).isFalse();
        var historikkinnslag = historikkRepository.hent(behandling.getSaksnummer()).getFirst();
        assertThat(historikkinnslag.getLinjer()).hasSize(2);
        assertThat(historikkinnslag.getLinjer().get(0).getTekst()).contains("Søker har aleneomsorg for barnet");
        assertThat(historikkinnslag.getLinjer().get(1).getTekst()).isEqualTo(dto.getBegrunnelse());
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

        scenario.lagre(repositoryProvider);

        var behandling = scenario.getBehandling();
        // Dto
        var dto = new AvklarAleneomsorgVurderingDto("begrunnelse.");
        dto.setAleneomsorg(oppdatertAleneOmsorg);
        dto.setAnnenforelderHarRett(true);
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getAksjonspunktDefinisjon());
        // Act
        var oppdateringresultat = new BekreftAleneomsorgOppdaterer(
            new FaktaOmsorgRettTjeneste(ytelseFordelingTjeneste, mock(FagsakEgenskapRepository.class)),
            repositoryProvider.getHistorikkinnslagRepository()).oppdater(dto,
            new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto, aksjonspunkt));

        // Assert
        assertThat(oppdateringresultat.kreverTotrinnsKontroll()).isTrue();
        var historikk = repositoryProvider.getHistorikkinnslagRepository().hent(behandling.getSaksnummer()).getFirst();
        assertThat(historikk.getLinjer()).hasSize(3);
        assertThat(historikk.getLinjer().getFirst().getTekst()).contains("Søker har ikke aleneomsorg for barnet");
        assertThat(historikk.getLinjer().get(1).getTekst()).contains("Annen forelder har rett");
        assertThat(historikk.getLinjer().get(2).getTekst()).isEqualTo(dto.getBegrunnelse());
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

        scenario.lagre(repositoryProvider);

        var behandling = scenario.getBehandling();
        // Dto
        var dto = new AvklarAleneomsorgVurderingDto("begrunnelse.");
        dto.setAleneomsorg(oppdatertAleneOmsorg);
        dto.setAnnenforelderHarRett(true);
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getAksjonspunktDefinisjon());
        // Act
        var oppdateringresultat = new BekreftAleneomsorgOppdaterer(
            new FaktaOmsorgRettTjeneste(ytelseFordelingTjeneste, mock(FagsakEgenskapRepository.class)),
            repositoryProvider.getHistorikkinnslagRepository()).oppdater(dto,
            new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto, aksjonspunkt));

        // Assert
        assertThat(oppdateringresultat.kreverTotrinnsKontroll()).isTrue();

        var historikk = historikkRepository.hent(behandling.getSaksnummer()).getFirst();
        assertThat(historikk.getLinjer()).hasSize(3);
        assertThat(historikk.getLinjer().get(0).getTekst()).contains("Søker har ikke aleneomsorg for barnet");
        assertThat(historikk.getLinjer().get(1).getTekst()).contains("Annen forelder har rett");
        assertThat(historikk.getLinjer().get(2).getTekst()).isEqualTo(dto.getBegrunnelse());
    }

    @Test
    void skal_generere_historikkinnslag_ved_avklaring_av_aleneomsorg_og_ikke_rett() {
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

        scenario.lagre(repositoryProvider);

        var behandling = scenario.getBehandling();
        // Dto
        var dto = new AvklarAleneomsorgVurderingDto("begrunnelse.");
        dto.setAleneomsorg(oppdatertAleneOmsorg);
        dto.setAnnenforelderHarRett(false);
        dto.setAnnenforelderMottarUføretrygd(true);
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getAksjonspunktDefinisjon());
        // Act
        var oppdateringresultat = new BekreftAleneomsorgOppdaterer(
            new FaktaOmsorgRettTjeneste(ytelseFordelingTjeneste, mock(FagsakEgenskapRepository.class)),
            repositoryProvider.getHistorikkinnslagRepository()).oppdater(dto,
            new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto, aksjonspunkt));

        var historikkinnslag = historikkRepository.hent(behandling.getSaksnummer());

        // Assert
        assertThat(oppdateringresultat.kreverTotrinnsKontroll()).isTrue();

        assertThat(ytelseFordelingTjeneste.hentAggregat(behandling.getId()).getMorUføretrygdAvklaring()).isTrue();

        assertThat(historikkinnslag.getFirst().getLinjer()).hasSize(5);
        assertThat(historikkinnslag.getFirst().getLinjer().get(0).getTekst()).contains("Søker har ikke aleneomsorg for barnet");
        assertThat(historikkinnslag.getFirst().getLinjer().get(1).getTekst()).contains("Annen forelder har ikke rett");
        assertThat(historikkinnslag.getFirst().getLinjer().get(2).getTekst()).contains("Annen forelder har mottatt pengestøtte tilsvarende foreldrepenger fra land i EØS");
        assertThat(historikkinnslag.getFirst().getLinjer().get(3).getTekst()).contains("Mor mottar uføretrygd");
        assertThat(historikkinnslag.getFirst().getLinjer().get(4).getTekst()).isEqualTo(dto.getBegrunnelse());
    }
}
