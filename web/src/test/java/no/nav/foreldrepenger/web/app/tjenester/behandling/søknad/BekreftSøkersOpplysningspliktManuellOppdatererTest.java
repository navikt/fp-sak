package no.nav.foreldrepenger.web.app.tjenester.behandling.søknad;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringAksjonspunktResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.FarSøkerType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.web.app.tjenester.behandling.søknad.aksjonspunkt.BekreftSokersOpplysningspliktManuDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.søknad.aksjonspunkt.BekreftSøkersOpplysningspliktManuellOppdaterer;

@CdiDbAwareTest
class BekreftSøkersOpplysningspliktManuellOppdatererTest {

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;

    @Test
    void skal_generere_historikkinnslag_ved_avklaring_av_søkers_opplysningsplikt_manu() {
        // Arrange
        // Behandling
        var scenario = ScenarioFarSøkerEngangsstønad.forAdopsjon();
        scenario.medSøknad().medFarSøkerType(FarSøkerType.OVERTATT_OMSORG);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.SØKERS_OPPLYSNINGSPLIKT_MANU, BehandlingStegType.KONTROLLERER_SØKERS_OPPLYSNINGSPLIKT);
        scenario.lagre(repositoryProvider);

        var behandling = scenario.getBehandling();

        var historikkinnslagRepository = repositoryProvider.getHistorikkinnslagRepository();
        var oppdaterer = new BekreftSøkersOpplysningspliktManuellOppdaterer(historikkinnslagRepository,
            repositoryProvider.getBehandlingRepository());

        // Dto
        var bekreftSokersOpplysningspliktManuDto = new BekreftSokersOpplysningspliktManuDto("test av manu", true, Collections.emptyList());
        assertThat(behandling.getAksjonspunkter()).hasSize(1);

        // Act
        var aksjonspunkt = behandling.getAksjonspunktFor(bekreftSokersOpplysningspliktManuDto.getAksjonspunktDefinisjon());
        var resultat = oppdaterer.oppdater(bekreftSokersOpplysningspliktManuDto,
            new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), bekreftSokersOpplysningspliktManuDto, aksjonspunkt));
        var historikkInnslagForBehandling = repositoryProvider.getHistorikkinnslagRepository().hent(behandling.getSaksnummer());

        // Assert
        assertThat(historikkInnslagForBehandling).hasSize(1);
        var historikkInnslag = historikkInnslagForBehandling.getFirst();
        var linjer = historikkInnslag.getLinjer();
        assertThat(linjer).hasSize(2);
        assertThat(linjer.getFirst().getTekst()).isEqualTo("__Søkers opplysningsplikt__ er satt til __Vilkåret er oppfylt__.");
        assertThat(linjer.get(1).getTekst()).contains(bekreftSokersOpplysningspliktManuDto.getBegrunnelse());

        var aksjonspunktSet = resultat.getEkstraAksjonspunktResultat()
            .stream()
            .map(OppdateringAksjonspunktResultat::aksjonspunktDefinisjon)
            .collect(Collectors.toSet());

        assertThat(aksjonspunktSet).isEmpty();
    }
}
