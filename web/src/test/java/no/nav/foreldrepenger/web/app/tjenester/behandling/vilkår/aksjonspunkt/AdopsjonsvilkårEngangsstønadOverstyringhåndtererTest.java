package no.nav.foreldrepenger.web.app.tjenester.behandling.vilkår.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Set;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.FarSøkerType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.AksjonspunktTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vilkår.aksjonspunkt.es.OverstyringAdopsjonsvilkåretDto;

@CdiDbAwareTest
class AdopsjonsvilkårEngangsstønadOverstyringhåndtererTest {

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    @Inject
    private AksjonspunktTjeneste aksjonspunktTjeneste;

    @Test
    void skal_opprette_aksjonspunkt_for_overstyring() {
        // Arrange
        // Behandling
        var scenario = ScenarioFarSøkerEngangsstønad.forAdopsjon();
        scenario.medSøknad().medFarSøkerType(FarSøkerType.OVERTATT_OMSORG);
        scenario.medSøknadHendelse().medAdopsjon(scenario.medSøknadHendelse().getAdopsjonBuilder()
            .medOmsorgsovertakelseDato(LocalDate.now().plusWeeks(1)));
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_OM_ADOPSJON_GJELDER_EKTEFELLES_BARN,
                BehandlingStegType.SØKERS_RELASJON_TIL_BARN);
        scenario.leggTilVilkår(VilkårType.ADOPSJONSVILKÅRET_ENGANGSSTØNAD, VilkårUtfallType.OPPFYLT);
        scenario.lagre(repositoryProvider);

        var behandling = scenario.getBehandling();

        // Dto
        var overstyringspunktDto = new OverstyringAdopsjonsvilkåretDto(false,
                "test av overstyring", "1005");
        assertThat(behandling.getAksjonspunkter()).hasSize(1);

        // Act
        aksjonspunktTjeneste.overstyrAksjonspunkter(Set.of(overstyringspunktDto), behandling);

        // Assert
        var aksjonspunktSet = behandling.getAksjonspunkter();
        assertThat(aksjonspunktSet).hasSize(2);
        assertThat(aksjonspunktSet).extracting("aksjonspunktDefinisjon")
                .contains(AksjonspunktDefinisjon.AVKLAR_OM_ADOPSJON_GJELDER_EKTEFELLES_BARN);
        assertThat(aksjonspunktSet.stream()
                .filter(ap -> ap.getAksjonspunktDefinisjon().equals(AksjonspunktDefinisjon.OVERSTYRING_AV_ADOPSJONSVILKÅRET)))
                        .anySatisfy(ap -> assertThat(ap.getStatus()).isEqualTo(AksjonspunktStatus.UTFØRT));
    }

}
