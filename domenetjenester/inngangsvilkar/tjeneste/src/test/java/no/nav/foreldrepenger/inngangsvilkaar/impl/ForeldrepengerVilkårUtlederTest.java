package no.nav.foreldrepenger.inngangsvilkaar.impl;

import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.ADOPSJONSVILKARET_FORELDREPENGER;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.BEREGNINGSGRUNNLAGVILKÅR;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.FØDSELSVILKÅRET_FAR_MEDMOR;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.MEDLEMSKAPSVILKÅRET;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.OPPTJENINGSPERIODEVILKÅR;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.OPPTJENINGSVILKÅRET;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.SØKERSOPPLYSNINGSPLIKT;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.inngangsvilkaar.impl.ForeldrepengerVilkårUtleder;
import no.nav.foreldrepenger.inngangsvilkaar.impl.UtledeteVilkår;

public class ForeldrepengerVilkårUtlederTest {

    @Test
    public void skal_opprette_vilkår_for_far_som_søker_stønad_foreldrepenger_fødsel() {
        // Arrange
        ScenarioFarSøkerForeldrepenger scenario = ScenarioFarSøkerForeldrepenger.forFødsel();
        scenario.medSøknadHendelse().medFødselsDato(LocalDate.now());
        Behandling behandling = scenario.lagMocked();
        final BehandlingRepositoryProvider repositoryProvider = scenario.mockBehandlingRepositoryProvider();

        // Act
        final Optional<FamilieHendelseType> familieHendelseType = repositoryProvider.getFamilieHendelseRepository().hentAggregatHvisEksisterer(behandling.getId())
            .map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon).map(FamilieHendelseEntitet::getType);
        UtledeteVilkår utledeteVilkår = new ForeldrepengerVilkårUtleder().utledVilkår(behandling, familieHendelseType);

        // Assert
        assertThat(utledeteVilkår.getPotensielleBetingedeVilkårtyper()).containsExactly(FØDSELSVILKÅRET_FAR_MEDMOR);
        assertThat(utledeteVilkår.getBetinget()).isEqualTo(Optional.of(FØDSELSVILKÅRET_FAR_MEDMOR));
        assertThat(utledeteVilkår.getAlleAvklarte()).containsExactly(FØDSELSVILKÅRET_FAR_MEDMOR, MEDLEMSKAPSVILKÅRET, SØKERSOPPLYSNINGSPLIKT, OPPTJENINGSPERIODEVILKÅR, OPPTJENINGSVILKÅRET, BEREGNINGSGRUNNLAGVILKÅR);
    }

    @Test
    public void skal_opprette_vilkår_for_mor_som_søker_stønad_foreldrepenger_adopsjon() {
        // Arrange
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forAdopsjon();
        scenario.medSøknadHendelse().medAdopsjon(scenario.medSøknadHendelse().getAdopsjonBuilder().medOmsorgsovertakelseDato(LocalDate.now()));
        Behandling behandling = scenario.lagMocked();
        final BehandlingRepositoryProvider repositoryProvider = scenario.mockBehandlingRepositoryProvider();

        // Act
        final Optional<FamilieHendelseType> familieHendelseType = repositoryProvider.getFamilieHendelseRepository().hentAggregatHvisEksisterer(behandling.getId())
            .map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon).map(FamilieHendelseEntitet::getType);
        UtledeteVilkår utledeteVilkår = new ForeldrepengerVilkårUtleder().utledVilkår(behandling, familieHendelseType);

        // Assert
        assertThat(utledeteVilkår.getPotensielleBetingedeVilkårtyper()).containsExactly(ADOPSJONSVILKARET_FORELDREPENGER);
        assertThat(utledeteVilkår.getBetinget()).isEqualTo(Optional.of(ADOPSJONSVILKARET_FORELDREPENGER));
        assertThat(utledeteVilkår.getAlleAvklarte()).containsExactly(ADOPSJONSVILKARET_FORELDREPENGER, MEDLEMSKAPSVILKÅRET, SØKERSOPPLYSNINGSPLIKT, OPPTJENINGSPERIODEVILKÅR, OPPTJENINGSVILKÅRET, BEREGNINGSGRUNNLAGVILKÅR);
    }

}
