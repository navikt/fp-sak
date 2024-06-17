package no.nav.foreldrepenger.web.app.tjenester.behandling.vilkår.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltVerdiType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.FarSøkerType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.FordelingPeriodeKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.AksjonspunktTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vilkår.aksjonspunkt.dto.OverstyringFødselvilkåretFarMedmorDto;

@CdiDbAwareTest
class FødselsvilkåretFarMedmorOverstyringshåndtererTest {

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    @Inject
    private AksjonspunktTjeneste aksjonspunktTjeneste;

    @Test
    void skal_generere_historikkinnslag_om_resultat_fødsel_far_medmor_er_overstyrt() {
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
        scenario.medSøknad().medFarSøkerType(FarSøkerType.OVERTATT_OMSORG);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.OVERSTYRING_AV_FØDSELSVILKÅRET_FAR_MEDMOR, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);
        scenario.lagre(repositoryProvider);

        var behandling = scenario.getBehandling();
        // Dto
        var overstyringDto = new OverstyringFødselvilkåretFarMedmorDto(false, "test overstyring av inngangsvilkår far/medmor",
            Avslagsårsak.INGEN_BARN_DOKUMENTERT_PÅ_FAR_MEDMOR.getKode());
        assertThat(behandling.getAksjonspunkter()).hasSize(1);

        // Act
        aksjonspunktTjeneste.overstyrAksjonspunkter(Set.of(overstyringDto), behandling.getId());

        // Assert
        var historikkinnslagene = repositoryProvider.getHistorikkRepository().hentHistorikk(behandling.getId());
        assertThat(historikkinnslagene.get(0).getHistorikkinnslagDeler()).hasSize(1);
        var feltList = historikkinnslagene.get(0).getHistorikkinnslagDeler().get(0).getEndredeFelt();
        assertThat(feltList).hasSize(1);
        var felt = feltList.get(0);
        assertThat(felt.getNavn()).as("navn").isEqualTo(HistorikkEndretFeltType.OVERSTYRT_VURDERING.getKode());
        assertThat(felt.getFraVerdi()).as("fraVerdi").isEqualTo(HistorikkEndretFeltVerdiType.VILKAR_OPPFYLT.getKode());
        assertThat(felt.getTilVerdi()).as("tilVerdi").isEqualTo(HistorikkEndretFeltVerdiType.VILKAR_IKKE_OPPFYLT.getKode());

        var aksjonspunktSet = behandling.getAksjonspunkter();

        assertThat(aksjonspunktSet).extracting("aksjonspunktDefinisjon").contains(AksjonspunktDefinisjon.OVERSTYRING_AV_FØDSELSVILKÅRET_FAR_MEDMOR);

        assertThat(aksjonspunktSet.stream()
            .filter(ap -> ap.getAksjonspunktDefinisjon().equals(AksjonspunktDefinisjon.OVERSTYRING_AV_FØDSELSVILKÅRET_FAR_MEDMOR))).anySatisfy(
            ap -> assertThat(ap.getStatus()).isEqualTo(AksjonspunktStatus.UTFØRT));

        assertThat(aksjonspunktSet).hasSize(1);
    }
}
