package no.nav.foreldrepenger.web.app.tjenester.behandling.ytelsefordeling;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.familiehendelse.rest.BekreftFaktaForOmsorgVurderingDto;

class BekreftOmsorgOppdatererTest extends EntityManagerAwareTest {

    private static final AksjonspunktDefinisjon AKSJONSPUNKT_DEF = AksjonspunktDefinisjon.AVKLAR_LØPENDE_OMSORG;

    private BehandlingRepositoryProvider behandlingRepositoryProvider;
    private YtelseFordelingTjeneste ytelseFordelingTjeneste;

    @BeforeEach
    void setUp() {
        var entityManager = getEntityManager();
        behandlingRepositoryProvider = new BehandlingRepositoryProvider(entityManager);
        ytelseFordelingTjeneste = new YtelseFordelingTjeneste(new YtelsesFordelingRepository(entityManager));
    }

    @Test
    void skal_generere_historikkinnslag_ved_avklaring_av_omsorg() {
        // Arrange
        var oppdatertOmsorg = true;

        // Behandling
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();

        scenario.medSøknad();
        var rettighet = OppgittRettighetEntitet.aleneomsorg();
        scenario.medOppgittRettighet(rettighet);
        scenario.leggTilAksjonspunkt(AKSJONSPUNKT_DEF, BehandlingStegType.VURDER_UTTAK);

        var behandling = scenario.lagre(behandlingRepositoryProvider);

        // Dto
        var dto = new BekreftFaktaForOmsorgVurderingDto("begrunnelse");
        dto.setOmsorg(oppdatertOmsorg);
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getAksjonspunktDefinisjon());
        // Act
        new BekreftOmsorgOppdaterer(ytelseFordelingTjeneste, behandlingRepositoryProvider.getHistorikkinnslagRepository())
            .oppdater(dto, new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto, aksjonspunkt));

        var historikkinnslag = behandlingRepositoryProvider.getHistorikkinnslagRepository().hent(behandling.getSaksnummer()).getFirst();
        assertThat(historikkinnslag.getLinjer()).hasSize(2);
        assertThat(historikkinnslag.getLinjer().getFirst().getTekst()).contains("Omsorg", "Søker har omsorg for barnet");
        assertThat(historikkinnslag.getLinjer().get(1).getTekst()).contains(dto.getBegrunnelse());
    }
}
