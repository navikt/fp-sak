package no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.*;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.UttakRepositoryStubProvider;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InngangsvilkårGrunnlagByggerTest {

    private final UttakRepositoryProvider repositoryProvider = new UttakRepositoryStubProvider();

    @Test
    void setterInngangsvilkåreneErOppfylt() {
        var bygger = new InngangsvilkårGrunnlagBygger(repositoryProvider);

        var scenario = ScenarioMorSøkerForeldrepenger
            .forFødsel();
        var behandling = scenario.lagre(repositoryProvider);

        var vilkårBuilder = VilkårResultat.builder();
        vilkårBuilder.leggTilVilkårOppfylt(VilkårType.FØDSELSVILKÅRET_MOR);
        vilkårBuilder.leggTilVilkårOppfylt(VilkårType.OPPTJENINGSVILKÅRET);
        vilkårBuilder.leggTilVilkårOppfylt(VilkårType.FORELDREANSVARSVILKÅRET_2_LEDD);
        vilkårBuilder.leggTilVilkårOppfylt(VilkårType.ADOPSJONSVILKARET_FORELDREPENGER);
        lagreVilkår(behandling, vilkårBuilder);

        var grunnlag = bygger.byggGrunnlag(input(behandling)).build();

        assertThat(grunnlag.erFødselsvilkåretOppfylt()).isTrue();
        assertThat(grunnlag.erAdopsjonOppfylt()).isTrue();
        assertThat(grunnlag.erForeldreansvarOppfylt()).isTrue();
        assertThat(grunnlag.erOpptjeningOppfylt()).isTrue();
    }

    private void lagreVilkår(Behandling behandling, VilkårResultat.Builder vilkårBuilder) {
        var behandlingsresultat =  Behandlingsresultat.builderForInngangsvilkår().build();
        behandlingsresultat.medOppdatertVilkårResultat(vilkårBuilder.build());
        repositoryProvider.getBehandlingsresultatRepository().lagre(behandling.getId(),behandlingsresultat);
    }

    private UttakInput input(Behandling behandling) {
        return new UttakInput(BehandlingReferanse.fra(behandling), null, null);
    }

    @Test
    void setterInngangsvilkåreneErAvslått() {
        var bygger = new InngangsvilkårGrunnlagBygger(repositoryProvider);

        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var behandling = scenario.lagre(repositoryProvider);

        var vilkårBuilder = VilkårResultat.builder();
        vilkårBuilder.leggTilVilkårAvslått(VilkårType.FØDSELSVILKÅRET_MOR, VilkårUtfallMerknad.VM_1026);
        vilkårBuilder.leggTilVilkårAvslått(VilkårType.OPPTJENINGSVILKÅRET, VilkårUtfallMerknad.VM_1035);
        vilkårBuilder.manueltVilkår(VilkårType.FORELDREANSVARSVILKÅRET_2_LEDD, VilkårUtfallType.IKKE_OPPFYLT, Avslagsårsak.SØKER_HAR_IKKE_FORELDREANSVAR);
        vilkårBuilder.leggTilVilkårAvslått(VilkårType.ADOPSJONSVILKARET_FORELDREPENGER, VilkårUtfallMerknad.VM_1004);
        lagreVilkår(behandling, vilkårBuilder);

        var grunnlag = bygger.byggGrunnlag(input(behandling)).build();

        assertThat(grunnlag.erFødselsvilkåretOppfylt()).isFalse();
        assertThat(grunnlag.erAdopsjonOppfylt()).isFalse();
        assertThat(grunnlag.erForeldreansvarOppfylt()).isFalse();
        assertThat(grunnlag.erOpptjeningOppfylt()).isFalse();
    }
}
