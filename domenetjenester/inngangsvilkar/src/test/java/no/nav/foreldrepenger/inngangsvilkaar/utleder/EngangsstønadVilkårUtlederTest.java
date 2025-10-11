package no.nav.foreldrepenger.inngangsvilkaar.utleder;

import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.FØDSELSVILKÅRET_MOR;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.MEDLEMSKAPSVILKÅRET_FORUTGÅENDE;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.OMSORGSOVERTAKELSEVILKÅR;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.SØKERSOPPLYSNINGSPLIKT;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.SØKNADSFRISTVILKÅRET;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.OmsorgsovertakelseVilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.FarSøkerType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;

class EngangsstønadVilkårUtlederTest {

    @Test
    void skal_opprette_vilkår_for_mor_som_søker_stønad_fødsel() {
        // Arrange
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse().medFødselsDato(LocalDate.now());
        var behandling = scenario.lagMocked();
        var repositoryProvider = scenario.mockBehandlingRepositoryProvider();

        // Act
        var familieHendelseType = repositoryProvider.getFamilieHendelseRepository()
            .hentAggregatHvisEksisterer(behandling.getId())
            .map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
            .map(FamilieHendelseEntitet::getType);
        var utledeteVilkår = EngangsstønadVilkårUtleder.utledVilkårFor(behandling, familieHendelseType);

        // Assert
        assertThat(utledeteVilkår).contains(FØDSELSVILKÅRET_MOR, MEDLEMSKAPSVILKÅRET_FORUTGÅENDE, SØKNADSFRISTVILKÅRET, SØKERSOPPLYSNINGSPLIKT);
    }

    @Test
    void skal_opprette_vilkår_for_mor_som_søker_stønad_adopsjon() {
        // Arrange
        var scenario = ScenarioMorSøkerEngangsstønad.forAdopsjon();
        scenario.medSøknadHendelse().medAdopsjon(scenario.medSøknadHendelse().getAdopsjonBuilder().medOmsorgsovertakelseDato(LocalDate.now()));
        var behandling = scenario.lagMocked();
        var repositoryProvider = scenario.mockBehandlingRepositoryProvider();

        // Act
        var familieHendelseType = repositoryProvider.getFamilieHendelseRepository()
            .hentAggregatHvisEksisterer(behandling.getId())
            .map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
            .map(FamilieHendelseEntitet::getType);
        var utledeteVilkår = EngangsstønadVilkårUtleder.utledVilkårFor(behandling, familieHendelseType);

        // Assert
        assertThat(utledeteVilkår).contains(OMSORGSOVERTAKELSEVILKÅR, MEDLEMSKAPSVILKÅRET_FORUTGÅENDE, SØKNADSFRISTVILKÅRET, SØKERSOPPLYSNINGSPLIKT);
    }

    @Test
    void skal_opprette_vilkår_for_far_som_søker_stønad_adopsjon() {
        // Arrange
        var scenario = ScenarioFarSøkerEngangsstønad.forAdopsjon();
        scenario.medSøknadHendelse().medAdopsjon(scenario.medSøknadHendelse().getAdopsjonBuilder().medAdoptererAlene(true)
            .medOmsorgsovertakelseDato(LocalDate.now()));
        scenario.medSøknad().medFarSøkerType(FarSøkerType.ADOPTERER_ALENE);
        var behandling = scenario.lagMocked();
        var repositoryProvider = scenario.mockBehandlingRepositoryProvider();

        // Act
        var familieHendelseType = repositoryProvider.getFamilieHendelseRepository()
            .hentAggregatHvisEksisterer(behandling.getId())
            .map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
            .map(FamilieHendelseEntitet::getType);
        var utledeteVilkår = EngangsstønadVilkårUtleder.utledVilkårFor(behandling, familieHendelseType);

        // Assert
        assertThat(utledeteVilkår).contains(OMSORGSOVERTAKELSEVILKÅR, MEDLEMSKAPSVILKÅRET_FORUTGÅENDE, SØKNADSFRISTVILKÅRET, SØKERSOPPLYSNINGSPLIKT);
    }

    @Test
    void skal_opprette_vilkår_for_far_som_søker_stønad_adopsjon_ikke_alene() {
        // Arrange
        var scenario = ScenarioFarSøkerEngangsstønad.forAdopsjon();
        scenario.medSøknadHendelse().medAdopsjon(scenario.medSøknadHendelse().getAdopsjonBuilder().medOmsorgsovertakelseDato(LocalDate.now())
            .medOmsorgovertalseVilkårType(OmsorgsovertakelseVilkårType.ES_OMSORGSVILKÅRET));
        scenario.medSøknad().medFarSøkerType(FarSøkerType.ANDRE_FORELDER_DØD);
        var behandling = scenario.lagMocked();
        var repositoryProvider = scenario.mockBehandlingRepositoryProvider();

        // Act
        var familieHendelseType = repositoryProvider.getFamilieHendelseRepository()
            .hentAggregatHvisEksisterer(behandling.getId())
            .map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
            .map(FamilieHendelseEntitet::getType);
        var utledeteVilkår = EngangsstønadVilkårUtleder.utledVilkårFor(behandling, familieHendelseType);

        // Assert
        assertThat(utledeteVilkår).contains(MEDLEMSKAPSVILKÅRET_FORUTGÅENDE, SØKNADSFRISTVILKÅRET, SØKERSOPPLYSNINGSPLIKT);
    }

    @Test
    void skal_opprette_vilkår_når_far_søker_om_omsorgsovertakelse_ved_fødsel() {
        // Arrange
        var scenario = ScenarioFarSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse().medAdopsjon(scenario.medSøknadHendelse().getAdopsjonBuilder()
            .medOmsorgovertalseVilkårType(OmsorgsovertakelseVilkårType.ES_OMSORGSVILKÅRET).medOmsorgsovertakelseDato(LocalDate.now()));
        scenario.medSøknad().medFarSøkerType(FarSøkerType.OVERTATT_OMSORG_F);
        var behandling = scenario.lagMocked();
        var repositoryProvider = scenario.mockBehandlingRepositoryProvider();

        // Act
        var familieHendelseType = repositoryProvider.getFamilieHendelseRepository()
            .hentAggregatHvisEksisterer(behandling.getId())
            .map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
            .map(FamilieHendelseEntitet::getType);
        var utledeteVilkår = EngangsstønadVilkårUtleder.utledVilkårFor(behandling, familieHendelseType);

        // Assert
        assertThat(utledeteVilkår).contains(MEDLEMSKAPSVILKÅRET_FORUTGÅENDE, SØKNADSFRISTVILKÅRET, SØKERSOPPLYSNINGSPLIKT);
    }
}
