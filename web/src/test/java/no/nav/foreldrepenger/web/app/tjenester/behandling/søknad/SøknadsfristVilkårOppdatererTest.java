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
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_SØKNADSFRISTVILKÅRET,
                BehandlingStegType.VURDER_SØKNADSFRISTVILKÅR);
        scenario.lagre(repositoryProvider);

        var behandling = scenario.getBehandling();
        var historikkinnslag2Repository = repositoryProvider.getHistorikkinnslag2Repository();
        var oppdaterer = new SøknadsfristOppdaterer(historikkinnslag2Repository);

        // Dto
        var dto = new SoknadsfristAksjonspunktDto("begrunnelse", oppdatertSoknadsfristOk);

        // Act
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getAksjonspunktDefinisjon());
        oppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto, aksjonspunkt));
        var historikkinnslag = historikkinnslag2Repository.hent(behandling.getId()).getFirst();

        // Assert
        assertThat(historikkinnslag.getTekstlinjer().getFirst().getTekst()).contains("Søknadsfristvilkåret", "oppfylt");
        assertThat(historikkinnslag.getTekstlinjer().getFirst().getTekst()).doesNotContain("ikke oppfylt");
        assertThat(historikkinnslag.getTekstlinjer().get(1).getTekst()).contains(dto.getBegrunnelse());
    }

}
