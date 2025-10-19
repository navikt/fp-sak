package no.nav.foreldrepenger.familiehendelse.aksjonspunkt.sammebarn;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning.Personopplysning;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.HistorikkSammeBarnTjeneste;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.sammebarn.dto.VurdereYtelseSammeBarnSøkerAksjonspunktDto;

class VurdereYtelseSammeBarnSøkerOppdatererTest {

    private final VilkårResultat.Builder vilkårBuilder = VilkårResultat.builder();

    @Test
    void skal_oppdatere_vilkår_for_adopsjon() {
        // Arrange
        var scenario = scenario();
        var behandling = scenario.lagMocked();

        var dto = new VurdereYtelseSammeBarnSøkerAksjonspunktDto();
        dto.setErVilkarOk(true);

        utførAksjonspunktOppdaterer(behandling, dto, scenario.mockBehandlingRepositoryProvider());

        // Assert
        var behandlingsresultat = behandling.getBehandlingsresultat();
        assertThat(behandlingsresultat.getVilkårResultat().getVilkårene()).hasSize(1);
        assertThat(behandlingsresultat.getVilkårResultat().getVilkårene().get(0).getGjeldendeVilkårUtfall()).isEqualTo(VilkårUtfallType.OPPFYLT);
    }

    @Test
    void skal_sette_adopsjonsvilkåret_til_ikke_oppfylt_med_avslagskode() {
        var scenario = scenario();
        var behandling = scenario.lagMocked();

        var dto = new VurdereYtelseSammeBarnSøkerAksjonspunktDto();
        dto.setErVilkarOk(false);
        dto.setAvslagskode("1006");

        utførAksjonspunktOppdaterer(behandling, dto, scenario.mockBehandlingRepositoryProvider());

        // Assert
        var vilkårResultat = behandling.getBehandlingsresultat().getVilkårResultat();
        assertThat(vilkårResultat.getVilkårene()).hasSize(1);
        var vilkår = vilkårResultat.getVilkårene().get(0);
        assertThat(vilkår.getGjeldendeVilkårUtfall()).isEqualTo(VilkårUtfallType.IKKE_OPPFYLT);
        assertThat(vilkår.getAvslagsårsak()).isEqualTo(Avslagsårsak.MANN_ADOPTERER_IKKE_ALENE);
    }

    private ScenarioFarSøkerEngangsstønad scenario() {
        var scenario = ScenarioFarSøkerEngangsstønad.forAdopsjon();

        var forelder = scenario.opprettBuilderForRegisteropplysninger()
            .leggTilPersonopplysninger(
                Personopplysning.builder()
                    .aktørId(AktørId.dummy())
                    .navn("Forelder")
                    .fødselsdato(LocalDate.now().minusYears(29))
            )
            .build();

        scenario.medRegisterOpplysninger(forelder);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_OM_SØKER_HAR_MOTTATT_STØTTE, BehandlingStegType.KONTROLLER_FAKTA);
        scenario.leggTilVilkår(VilkårType.ADOPSJONSVILKÅRET_ENGANGSSTØNAD, VilkårUtfallType.IKKE_VURDERT);
        return scenario;
    }

    private void utførAksjonspunktOppdaterer(Behandling behandling,
                                             VurdereYtelseSammeBarnSøkerAksjonspunktDto dto,
                                             BehandlingRepositoryProvider repositoryProvider) {
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getAksjonspunktDefinisjon());
        // Act
        var resultat = new VurdereYtelseSammeBarnOppdaterer.VurdereYtelseSammeBarnSøkerOppdaterer(
            new HistorikkSammeBarnTjeneste(repositoryProvider.getHistorikkinnslagRepository()), repositoryProvider.getBehandlingsresultatRepository())
            .oppdater(dto, new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto, aksjonspunkt));
        byggVilkårResultat(vilkårBuilder, resultat);
        vilkårBuilder.buildFor(behandling);
    }

    private void byggVilkårResultat(VilkårResultat.Builder vilkårBuilder, OppdateringResultat delresultat) {
        delresultat.getVilkårUtfallSomSkalLeggesTil()
            .forEach(v -> vilkårBuilder.manueltVilkår(v.getVilkårType(), v.getVilkårUtfallType(), v.getAvslagsårsak()));
        delresultat.getVilkårTyperSomSkalFjernes().forEach(vilkårBuilder::fjernVilkår); // TODO: Vilkår burde ryddes på ein annen måte enn dette
    }

}
