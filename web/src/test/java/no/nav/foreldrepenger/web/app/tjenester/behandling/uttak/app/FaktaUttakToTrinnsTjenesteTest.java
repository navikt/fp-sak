package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.FaktaUttakDto;

public class FaktaUttakToTrinnsTjenesteTest {
    @Rule
    public UnittestRepositoryRule repositoryRule = new UnittestRepositoryRule();

    private BehandlingRepositoryProvider behandlingRepositoryProvider = new BehandlingRepositoryProvider(repositoryRule.getEntityManager());
    private YtelseFordelingTjeneste ytelseFordelingTjeneste = new YtelseFordelingTjeneste(new YtelsesFordelingRepository(repositoryRule.getEntityManager()));

    @Test
    public void skal_sette_totrinns_ved_endring_fakta_uttak() {
        //Scenario med avklar fakta uttak
        ScenarioMorSøkerForeldrepenger scenario = AvklarFaktaTestUtil.opprettScenarioMorSøkerForeldrepenger();
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_FAKTA_UTTAK_KONTROLLER_SØKNADSPERIODER,
            BehandlingStegType.VURDER_UTTAK);
        scenario.lagre(behandlingRepositoryProvider);
        // Behandling
        Behandling behandling = AvklarFaktaTestUtil.opprettBehandling(scenario);

        // dto
        FaktaUttakDto dto = AvklarFaktaTestUtil.opprettDtoAvklarFaktaUttakDto();

        boolean totrinn = new FaktaUttakToTrinnsTjeneste(ytelseFordelingTjeneste).oppdaterTotrinnskontrollVedEndringerFaktaUttak(dto);
        //assert
        assertThat(behandling.harAksjonspunktMedType(AksjonspunktDefinisjon.AVKLAR_FAKTA_UTTAK_KONTROLLER_SØKNADSPERIODER)).isTrue();
        assertThat(totrinn).isTrue();

    }

}
