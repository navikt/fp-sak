package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2Repository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakEgenskapRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app.FaktaOmsorgRettTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.AvklarAleneomsorgVurderingDto;

class BekreftAleneomsorgOppdatererTest extends EntityManagerAwareTest {
    private static final AksjonspunktDefinisjon AKSJONSPUNKT_DEF = AksjonspunktDefinisjon.MANUELL_KONTROLL_AV_OM_BRUKER_HAR_ALENEOMSORG;

    private final HistorikkInnslagTekstBuilder tekstBuilder = new HistorikkInnslagTekstBuilder();

    private BehandlingRepositoryProvider repositoryProvider;
    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private Historikkinnslag2Repository historikkRepository;

    @BeforeEach
    void setUp() {
        var entityManager = getEntityManager();
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        ytelseFordelingTjeneste = new YtelseFordelingTjeneste(new YtelsesFordelingRepository(entityManager));
        historikkRepository = repositoryProvider.getHistorikkinnslag2Repository();
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

        scenario.lagre(repositoryProvider);

        var behandling = scenario.getBehandling();
        // Dto
        var dto = new AvklarAleneomsorgVurderingDto("begrunnelse.");
        dto.setAleneomsorg(oppdatertAleneOmsorg);
        dto.setAnnenforelderHarRett(true);
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getAksjonspunktDefinisjon());
        // Act
        var oppdateringresultat = new BekreftAleneomsorgOppdaterer(new FaktaOmsorgRettTjeneste(ytelseFordelingTjeneste, mock(FagsakEgenskapRepository.class)), historikkRepository).oppdater(dto,
            new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto, aksjonspunkt));

        // Assert
        assertThat(oppdateringresultat.kreverTotrinnsKontroll()).isFalse();
        var historikkinnslag = historikkRepository.hent(behandling.getId()).getFirst();
        assertThat(historikkinnslag.getTekstlinjer()).hasSize(2);
        assertThat(historikkinnslag.getTekstlinjer().get(0).getTekst()).contains("Søker har aleneomsorg for barnet");
        assertThat(historikkinnslag.getTekstlinjer().get(1).getTekst()).isEqualTo(dto.getBegrunnelse());
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
        var oppdateringresultat = new BekreftAleneomsorgOppdaterer(new FaktaOmsorgRettTjeneste(ytelseFordelingTjeneste, mock(FagsakEgenskapRepository.class)),
            repositoryProvider.getHistorikkinnslag2Repository()).oppdater(dto,
            new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto, aksjonspunkt));

        // Assert
        assertThat(oppdateringresultat.kreverTotrinnsKontroll()).isTrue();
        var historikk = repositoryProvider.getHistorikkinnslag2Repository().hent(behandling.getId()).getFirst();
        assertThat(historikk.getTekstlinjer()).hasSize(3);
        assertThat(historikk.getTekstlinjer().getFirst().getTekst()).contains("Søker har ikke aleneomsorg for barnet");
        assertThat(historikk.getTekstlinjer().get(1).getTekst()).contains("Annen forelder har rett");
        assertThat(historikk.getTekstlinjer().get(2).getTekst()).isEqualTo(dto.getBegrunnelse());
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
        var oppdateringresultat = new BekreftAleneomsorgOppdaterer(new FaktaOmsorgRettTjeneste(ytelseFordelingTjeneste, mock(FagsakEgenskapRepository.class)),
            repositoryProvider.getHistorikkinnslag2Repository()).oppdater(dto,
            new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto, aksjonspunkt));

        // Assert
        assertThat(oppdateringresultat.kreverTotrinnsKontroll()).isTrue();

        var historikk = historikkRepository.hent(behandling.getId()).getFirst();
        assertThat(historikk.getTekstlinjer()).hasSize(3);
        assertThat(historikk.getTekstlinjer().get(0).getTekst()).contains("Søker har ikke aleneomsorg for barnet");
        assertThat(historikk.getTekstlinjer().get(1).getTekst()).contains("Annen forelder har rett");
        assertThat(historikk.getTekstlinjer().get(2).getTekst()).isEqualTo(dto.getBegrunnelse());
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

        scenario.lagre(repositoryProvider);

        var behandling = scenario.getBehandling();
        // Dto
        var dto = new AvklarAleneomsorgVurderingDto("begrunnelse.");
        dto.setAleneomsorg(oppdatertAleneOmsorg);
        dto.setAnnenforelderHarRett(false);
        dto.setAnnenforelderMottarUføretrygd(true);
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getAksjonspunktDefinisjon());
        // Act
        var oppdateringresultat = new BekreftAleneomsorgOppdaterer(new FaktaOmsorgRettTjeneste(ytelseFordelingTjeneste, mock(FagsakEgenskapRepository.class)),
            repositoryProvider.getHistorikkinnslag2Repository()).oppdater(dto,
            new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto, aksjonspunkt));

        var historikkinnslag = historikkRepository.hent(behandling.getId());

        // Assert
        assertThat(oppdateringresultat.kreverTotrinnsKontroll()).isTrue();

        assertThat(ytelseFordelingTjeneste.hentAggregat(behandling.getId()).getMorUføretrygdAvklaring()).isTrue();

        assertThat(historikkinnslag.getFirst().getTekstlinjer()).hasSize(4);
        assertThat(historikkinnslag.getFirst().getTekstlinjer().get(0).getTekst()).contains("Søker har ikke aleneomsorg for barnet");
        assertThat(historikkinnslag.getFirst().getTekstlinjer().get(1).getTekst()).contains("Annen forelder har ikke rett");
        assertThat(historikkinnslag.getFirst().getTekstlinjer().get(2).getTekst()).contains("Mor mottar uføretrygd");
        assertThat(historikkinnslag.getFirst().getTekstlinjer().get(3).getTekst()).isEqualTo(dto.getBegrunnelse());
    }
}
