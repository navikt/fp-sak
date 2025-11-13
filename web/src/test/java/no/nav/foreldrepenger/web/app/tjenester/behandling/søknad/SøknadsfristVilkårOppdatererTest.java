package no.nav.foreldrepenger.web.app.tjenester.behandling.søknad;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.FarSøkerType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.web.app.tjenester.behandling.søknad.aksjonspunkt.SoknadsfristAksjonspunktDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.søknad.aksjonspunkt.SøknadsfristOppdaterer;

@CdiDbAwareTest
class SøknadsfristVilkårOppdatererTest {

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;

    @Test
    void skal_generere_historikkinnslag_ved_avklaring_av_soknadsfrist() {
        // Arrange
        var oppdatertSoknadsfristOk = true;

        // Behandling
        var scenario = ScenarioFarSøkerEngangsstønad.forAdopsjon();
        scenario.medSøknad().medFarSøkerType(FarSøkerType.ADOPTERER_ALENE);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_SØKNADSFRISTVILKÅRET, BehandlingStegType.VURDER_SØKNADSFRISTVILKÅR);
        scenario.lagre(repositoryProvider);

        var behandling = scenario.getBehandling();
        var historikkinnslagRepository = repositoryProvider.getHistorikkinnslagRepository();
        var oppdaterer = new SøknadsfristOppdaterer(historikkinnslagRepository);

        // Dto
        var dto = new SoknadsfristAksjonspunktDto(oppdatertSoknadsfristOk, "begrunnelse.", null);

        // Act
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getAksjonspunktDefinisjon());
        oppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto, aksjonspunkt));
        var historikkinnslag = historikkinnslagRepository.hent(behandling.getSaksnummer()).getFirst();

        // Assert
        assertThat(historikkinnslag.getTekstLinjer()).contains("__Søknadsfristvilkåret__ er satt til __Oppfylt__.", dto.getBegrunnelse());
    }

}
