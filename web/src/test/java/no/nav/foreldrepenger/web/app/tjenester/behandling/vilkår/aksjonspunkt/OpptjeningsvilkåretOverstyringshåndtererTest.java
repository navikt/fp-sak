package no.nav.foreldrepenger.web.app.tjenester.behandling.vilkår.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.aksjonspunkt.OverstyringAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.FordelingPeriodeKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.AksjonspunktTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vilkår.aksjonspunkt.dto.OverstyringOpptjeningsvilkåretDto;
import no.nav.vedtak.exception.FunksjonellException;

@CdiDbAwareTest
class OpptjeningsvilkåretOverstyringshåndtererTest {

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    @Inject
    private AksjonspunktTjeneste aksjonspunktTjeneste;

    @Test
    void skal_opprette_aksjonspunkt_for_overstyring() {
        // Arrange
        // Behandling
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medDefaultFordeling(LocalDate.now())
            .medFødselAdopsjonsdato(List.of(LocalDate.now()));
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.VURDER_PERIODER_MED_OPPTJENING,
                BehandlingStegType.VURDER_OPPTJENINGSVILKÅR);
        scenario.leggTilVilkår(VilkårType.OPPTJENINGSVILKÅRET, VilkårUtfallType.OPPFYLT);
        scenario.lagre(repositoryProvider);

        var behandling = scenario.getBehandling();
        // Dto
        var overstyringspunktDto = new OverstyringOpptjeningsvilkåretDto(false,
                "test av overstyring opptjeningsvilkåret", "1035");
        assertThat(behandling.getAksjonspunkter()).hasSize(1);

        // Act
        aksjonspunktTjeneste.overstyrAksjonspunkter(Set.of(overstyringspunktDto), behandling.getId());

        // Assert
        var aksjonspunktSet = behandling.getAksjonspunkter();
        assertThat(aksjonspunktSet).hasSize(2);
        assertThat(aksjonspunktSet).extracting("aksjonspunktDefinisjon")
                .contains(AksjonspunktDefinisjon.VURDER_PERIODER_MED_OPPTJENING);
        assertThat(aksjonspunktSet.stream()
                .filter(ap -> ap.getAksjonspunktDefinisjon().equals(AksjonspunktDefinisjon.OVERSTYRING_AV_OPPTJENINGSVILKÅRET)))
                        .anySatisfy(ap -> assertThat(ap.getStatus()).isEqualTo(AksjonspunktStatus.UTFØRT));
    }

    @Test
    void skal_få_historikkinnslag_når_opptjening_er_overstyrt() {
        // Arrange
        // Behandling
        var oppgittPeriode = OppgittPeriodeBuilder.ny()
            .medPeriode(LocalDate.now(), LocalDate.now().plusWeeks(6))
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .build();
        var scenario = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medFordeling(new OppgittFordelingEntitet(List.of(oppgittPeriode), true))
            .medFødselAdopsjonsdato(List.of(LocalDate.now()));
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.VURDER_PERIODER_MED_OPPTJENING,
                BehandlingStegType.VURDER_OPPTJENINGSVILKÅR);
        scenario.leggTilVilkår(VilkårType.OPPTJENINGSVILKÅRET, VilkårUtfallType.OPPFYLT);
        scenario.lagre(repositoryProvider);

        var behandling = scenario.getBehandling();
        // Dto
        var overstyringspunktDto = new OverstyringOpptjeningsvilkåretDto(false,
                "test av overstyring opptjeningsvilkåret", "1035");

        // Act
        aksjonspunktTjeneste.overstyrAksjonspunkter(Set.of(overstyringspunktDto), behandling.getId());

        // Assert
        var historikkinnslagene = repositoryProvider.getHistorikkinnslagRepository().hent(behandling.getSaksnummer());
        var linjer = historikkinnslagene.getFirst().getLinjer();
        assertThat(linjer).hasSize(2);
        assertThat(linjer.getFirst().getTekst()).contains("Overstyrt vurdering", "ikke oppfylt");
        assertThat(linjer.get(1).getTekst()).contains(overstyringspunktDto.getBegrunnelse());
    }

    @Test
    void skal_feile_hvis_det_forsøkes_å_overstyre_uten_aktiviteter_i_opptjening() {
        // Arrange
        // Behandling
        var scenario = ScenarioFarSøkerForeldrepenger.forFødsel();
        scenario.medSøknadHendelse().medFødselsDato(LocalDate.now().minusWeeks(2), 1);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.VURDER_PERIODER_MED_OPPTJENING,
                BehandlingStegType.VURDER_OPPTJENINGSVILKÅR);
        scenario.leggTilVilkår(VilkårType.OPPTJENINGSVILKÅRET, VilkårUtfallType.OPPFYLT);
        scenario.lagre(repositoryProvider);

        var behandling = scenario.getBehandling();
        var behandlingId = behandling.getId();
        // Dto
        var overstyringspunktDto = new OverstyringOpptjeningsvilkåretDto(true,
                "test av overstyring opptjeningsvilkåret", "1035");
        Collection<OverstyringAksjonspunktDto> dto = Set.of(overstyringspunktDto);

        //Act
        assertThatThrownBy(() -> aksjonspunktTjeneste.overstyrAksjonspunkter(dto, behandlingId))
            .isInstanceOf(FunksjonellException.class)
            .hasMessage("FP-093923:Kan ikke overstyre vilkår. Det må være minst en aktivitet for at opptjeningsvilkåret skal kunne overstyres.");
    }
}
