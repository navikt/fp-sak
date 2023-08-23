package no.nav.foreldrepenger.inngangsvilkaar.utleder;

import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.*;
import static org.assertj.core.api.Assertions.assertThat;

class ForeldrepengerVilkårUtlederTest {

    @Test
    void skal_opprette_vilkår_for_far_som_søker_stønad_foreldrepenger_fødsel() {
        // Arrange
        var scenario = ScenarioFarSøkerForeldrepenger.forFødsel();
        scenario.medSøknadHendelse().medFødselsDato(LocalDate.now());
        var behandling = scenario.lagMocked();
        var repositoryProvider = scenario.mockBehandlingRepositoryProvider();

        // Act
        var familieHendelseType = repositoryProvider.getFamilieHendelseRepository()
            .hentAggregatHvisEksisterer(behandling.getId())
            .map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
            .map(FamilieHendelseEntitet::getType);
        var utledeteVilkår = ForeldrepengerVilkårUtleder.utledVilkårFor(behandling, familieHendelseType);

        // Assert
        assertThat(utledeteVilkår).contains(FØDSELSVILKÅRET_FAR_MEDMOR, MEDLEMSKAPSVILKÅRET, SØKERSOPPLYSNINGSPLIKT, OPPTJENINGSPERIODEVILKÅR, OPPTJENINGSVILKÅRET, BEREGNINGSGRUNNLAGVILKÅR);
    }

    @Test
    void skal_opprette_vilkår_for_mor_som_søker_stønad_foreldrepenger_adopsjon() {
        // Arrange
        var scenario = ScenarioMorSøkerForeldrepenger.forAdopsjon();
        scenario.medSøknadHendelse().medAdopsjon(scenario.medSøknadHendelse().getAdopsjonBuilder().medOmsorgsovertakelseDato(LocalDate.now()));
        var behandling = scenario.lagMocked();
        var repositoryProvider = scenario.mockBehandlingRepositoryProvider();

        // Act
        var familieHendelseType = repositoryProvider.getFamilieHendelseRepository()
            .hentAggregatHvisEksisterer(behandling.getId())
            .map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
            .map(FamilieHendelseEntitet::getType);
        var utledeteVilkår = ForeldrepengerVilkårUtleder.utledVilkårFor(behandling, familieHendelseType);

        // Assert
        assertThat(utledeteVilkår).contains(ADOPSJONSVILKARET_FORELDREPENGER, MEDLEMSKAPSVILKÅRET, SØKERSOPPLYSNINGSPLIKT, OPPTJENINGSPERIODEVILKÅR, OPPTJENINGSVILKÅRET, BEREGNINGSGRUNNLAGVILKÅR);
    }

}
