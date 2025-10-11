package no.nav.foreldrepenger.inngangsvilkaar.utleder;

import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.BEREGNINGSGRUNNLAGVILKÅR;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.FØDSELSVILKÅRET_FAR_MEDMOR;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.MEDLEMSKAPSVILKÅRET;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.OMSORGSOVERTAKELSEVILKÅR;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.OPPTJENINGSPERIODEVILKÅR;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.OPPTJENINGSVILKÅRET;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.SØKERSOPPLYSNINGSPLIKT;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;

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
        assertThat(utledeteVilkår).contains(OMSORGSOVERTAKELSEVILKÅR, MEDLEMSKAPSVILKÅRET, SØKERSOPPLYSNINGSPLIKT, OPPTJENINGSPERIODEVILKÅR, OPPTJENINGSVILKÅRET, BEREGNINGSGRUNNLAGVILKÅR);
    }

}
