package no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere;

import static org.assertj.core.api.Assertions.assertThat;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallMerknad;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Inngangsvilkår;

@ExtendWith(FPsakEntityManagerAwareExtension.class)
public class InngangsvilkårGrunnlagByggerTest {

    private UttakRepositoryProvider repositoryProvider;
    private BehandlingRepository behandlingRepository;

    @BeforeEach
    void setUp(EntityManager entityManager) {
        repositoryProvider = new UttakRepositoryProvider(entityManager);
        behandlingRepository = new BehandlingRepository(entityManager);
    }

    @Test
    public void setterInngangsvilkåreneErOppfylt() {
        InngangsvilkårGrunnlagBygger bygger = new InngangsvilkårGrunnlagBygger(repositoryProvider);

        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        Behandling behandling = scenario.lagre(repositoryProvider);

        VilkårResultat.Builder vilkårBuilder = VilkårResultat.builder();
        vilkårBuilder.leggTilVilkårResultat(VilkårType.FØDSELSVILKÅRET_MOR, VilkårUtfallType.OPPFYLT, VilkårUtfallMerknad.UDEFINERT,
                null, Avslagsårsak.UDEFINERT, false, false, null, null);
        vilkårBuilder.leggTilVilkårResultat(VilkårType.OPPTJENINGSVILKÅRET, VilkårUtfallType.OPPFYLT, VilkårUtfallMerknad.UDEFINERT,
                null, Avslagsårsak.UDEFINERT, false, false, null, null);
        vilkårBuilder.leggTilVilkårResultat(VilkårType.FORELDREANSVARSVILKÅRET_2_LEDD, VilkårUtfallType.OPPFYLT, VilkårUtfallMerknad.UDEFINERT,
                null, Avslagsårsak.UDEFINERT, false, false, null, null);
        vilkårBuilder.leggTilVilkårResultat(VilkårType.ADOPSJONSVILKARET_FORELDREPENGER, VilkårUtfallType.OPPFYLT, VilkårUtfallMerknad.UDEFINERT,
                null, Avslagsårsak.UDEFINERT, false, false, null, null);
        lagreVilkår(behandling, vilkårBuilder);

        Inngangsvilkår grunnlag = bygger.byggGrunnlag(input(behandling)).build();

        assertThat(grunnlag.erFødselsvilkåretOppfylt()).isTrue();
        assertThat(grunnlag.erAdopsjonOppfylt()).isTrue();
        assertThat(grunnlag.erForeldreansvarOppfylt()).isTrue();
        assertThat(grunnlag.erOpptjeningOppfylt()).isTrue();
    }

    private void lagreVilkår(Behandling behandling, VilkårResultat.Builder vilkårBuilder) {
        behandlingRepository.lagre(vilkårBuilder.buildFor(behandling), behandlingRepository.taSkriveLås(behandling.getId()));
    }

    private UttakInput input(Behandling behandling) {
        return new UttakInput(BehandlingReferanse.fra(behandling), null, null);
    }

    @Test
    public void setterInngangsvilkåreneErAvslått() {
        InngangsvilkårGrunnlagBygger bygger = new InngangsvilkårGrunnlagBygger(repositoryProvider);

        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        Behandling behandling = scenario.lagre(repositoryProvider);

        VilkårResultat.Builder vilkårBuilder = VilkårResultat.builder();
        vilkårBuilder.leggTilVilkårResultat(VilkårType.FØDSELSVILKÅRET_MOR, VilkårUtfallType.IKKE_OPPFYLT, VilkårUtfallMerknad.UDEFINERT,
                null, Avslagsårsak.UDEFINERT, false, false, null, null);
        vilkårBuilder.leggTilVilkårResultat(VilkårType.OPPTJENINGSVILKÅRET, VilkårUtfallType.IKKE_OPPFYLT, VilkårUtfallMerknad.UDEFINERT,
                null, Avslagsårsak.UDEFINERT, false, false, null, null);
        vilkårBuilder.leggTilVilkårResultat(VilkårType.FORELDREANSVARSVILKÅRET_2_LEDD, VilkårUtfallType.IKKE_OPPFYLT, VilkårUtfallMerknad.UDEFINERT,
                null, Avslagsårsak.UDEFINERT, false, false, null, null);
        vilkårBuilder.leggTilVilkårResultat(VilkårType.ADOPSJONSVILKARET_FORELDREPENGER, VilkårUtfallType.IKKE_OPPFYLT, VilkårUtfallMerknad.UDEFINERT,
                null, Avslagsårsak.UDEFINERT, false, false, null, null);
        lagreVilkår(behandling, vilkårBuilder);

        Inngangsvilkår grunnlag = bygger.byggGrunnlag(input(behandling)).build();

        assertThat(grunnlag.erFødselsvilkåretOppfylt()).isFalse();
        assertThat(grunnlag.erAdopsjonOppfylt()).isFalse();
        assertThat(grunnlag.erForeldreansvarOppfylt()).isFalse();
        assertThat(grunnlag.erOpptjeningOppfylt()).isFalse();
    }
}
