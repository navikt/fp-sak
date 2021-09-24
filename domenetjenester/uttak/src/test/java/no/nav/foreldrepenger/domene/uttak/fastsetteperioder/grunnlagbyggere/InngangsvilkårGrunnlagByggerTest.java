package no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.UttakRepositoryStubProvider;

public class InngangsvilkårGrunnlagByggerTest {

    private final UttakRepositoryProvider repositoryProvider = new UttakRepositoryStubProvider();

    @Test
    public void setterInngangsvilkåreneErOppfylt() {
        var bygger = new InngangsvilkårGrunnlagBygger(repositoryProvider);

        var scenario = ScenarioMorSøkerForeldrepenger
            .forFødsel();
        var behandling = scenario.lagre(repositoryProvider);

        var vilkårBuilder = VilkårResultat.builder();
        vilkårBuilder.leggTilVilkår(VilkårType.FØDSELSVILKÅRET_MOR, VilkårUtfallType.OPPFYLT);
        vilkårBuilder.leggTilVilkår(VilkårType.OPPTJENINGSVILKÅRET, VilkårUtfallType.OPPFYLT);
        vilkårBuilder.leggTilVilkår(VilkårType.FORELDREANSVARSVILKÅRET_2_LEDD, VilkårUtfallType.OPPFYLT);
        vilkårBuilder.leggTilVilkår(VilkårType.ADOPSJONSVILKARET_FORELDREPENGER, VilkårUtfallType.OPPFYLT);
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
    public void setterInngangsvilkåreneErAvslått() {
        var bygger = new InngangsvilkårGrunnlagBygger(repositoryProvider);

        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var behandling = scenario.lagre(repositoryProvider);

        var vilkårBuilder = VilkårResultat.builder();
        vilkårBuilder.leggTilVilkår(VilkårType.FØDSELSVILKÅRET_MOR, VilkårUtfallType.IKKE_OPPFYLT, Avslagsårsak.FØDSELSDATO_IKKE_OPPGITT_ELLER_REGISTRERT);
        vilkårBuilder.leggTilVilkår(VilkårType.OPPTJENINGSVILKÅRET, VilkårUtfallType.IKKE_OPPFYLT, Avslagsårsak.IKKE_TILSTREKKELIG_OPPTJENING);
        vilkårBuilder.leggTilVilkår(VilkårType.FORELDREANSVARSVILKÅRET_2_LEDD, VilkårUtfallType.IKKE_OPPFYLT, Avslagsårsak.SØKER_HAR_IKKE_FORELDREANSVAR);
        vilkårBuilder.leggTilVilkår(VilkårType.ADOPSJONSVILKARET_FORELDREPENGER, VilkårUtfallType.IKKE_OPPFYLT, Avslagsårsak.BARN_OVER_15_ÅR);
        lagreVilkår(behandling, vilkårBuilder);

        var grunnlag = bygger.byggGrunnlag(input(behandling)).build();

        assertThat(grunnlag.erFødselsvilkåretOppfylt()).isFalse();
        assertThat(grunnlag.erAdopsjonOppfylt()).isFalse();
        assertThat(grunnlag.erForeldreansvarOppfylt()).isFalse();
        assertThat(grunnlag.erOpptjeningOppfylt()).isFalse();
    }
}
