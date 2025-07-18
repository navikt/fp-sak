package no.nav.foreldrepenger.behandling.steg.inngangsvilkår;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.StegTransisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.inngangsvilkaar.InngangsvilkårTjeneste;
import no.nav.foreldrepenger.inngangsvilkaar.RegelOrkestrerer;
import no.nav.foreldrepenger.inngangsvilkaar.RegelResultat;

@ExtendWith(MockitoExtension.class)
class SamletInngangsvilkårStegImplTest {

    private BehandlingskontrollKontekst kontekst;

    @Mock
    private RegelOrkestrerer regelOrkestrerer;
    @Mock
    private InngangsvilkårTjeneste ivTjeneste;

    private InngangsvilkårFellesTjeneste inngangsvilkårFellesTjeneste;

    @BeforeEach
    void before() {
        inngangsvilkårFellesTjeneste = new InngangsvilkårFellesTjeneste(regelOrkestrerer);
        when(ivTjeneste.erInngangsvilkår(any())).thenReturn(true);
    }

    @Test
    void skal_gjenskape_overstyring_mellomliggende_vilkår() {
        // Arrange
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medBehandlingType(BehandlingType.REVURDERING)
                .leggTilVilkår(VilkårType.SØKERSOPPLYSNINGSPLIKT, VilkårUtfallType.IKKE_OPPFYLT)
                .leggTilVilkår(VilkårType.FØDSELSVILKÅRET_MOR, VilkårUtfallType.IKKE_VURDERT)
                .leggTilVilkår(VilkårType.MEDLEMSKAPSVILKÅRET, VilkårUtfallType.IKKE_VURDERT)
                .leggTilVilkår(VilkårType.OPPTJENINGSVILKÅRET, VilkårUtfallType.OPPFYLT);
        var behandling = scenario.lagMocked();

        var repositoryProvider = scenario.mockBehandlingRepositoryProvider();
        Behandlingsresultat.builderEndreEksisterende(behandling.getBehandlingsresultat())
                .medBehandlingResultatType(BehandlingResultatType.INNVILGET);
        kontekst = new BehandlingskontrollKontekst(behandling,
                repositoryProvider.getBehandlingRepository().taSkriveLås(behandling));

        var val = new RegelResultat(behandling.getBehandlingsresultat().getVilkårResultat(), emptyList(), emptyMap());
        when(regelOrkestrerer.vurderInngangsvilkår(eq(Set.of()), any(), any())).thenReturn(val);

        // Act
        var stegResultat = new SamletInngangsvilkårStegImpl(repositoryProvider, inngangsvilkårFellesTjeneste, ivTjeneste)
                .utførSteg(kontekst);

        // Assert
        assertThat(stegResultat.getTransisjon().stegTransisjon()).isEqualTo(StegTransisjon.HOPPOVER);
        assertThat(stegResultat.getTransisjon().målSteg()).isEqualTo(BehandlingStegType.INNGANG_UTTAK);

        var vilkårResultat = behandling.getBehandlingsresultat().getVilkårResultat();
        assertThat(vilkårResultat.getVilkårene().stream().map(Vilkår::getGjeldendeVilkårUtfall).anyMatch(VilkårUtfallType.IKKE_OPPFYLT::equals)).isTrue();
    }

    @Test
    void alle_vilkår_oppfylt() {
        // Arrange
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medBehandlingType(BehandlingType.REVURDERING)
                .leggTilVilkår(VilkårType.SØKERSOPPLYSNINGSPLIKT, VilkårUtfallType.OPPFYLT)
                .leggTilVilkår(VilkårType.FØDSELSVILKÅRET_MOR, VilkårUtfallType.OPPFYLT)
                .leggTilVilkår(VilkårType.MEDLEMSKAPSVILKÅRET, VilkårUtfallType.OPPFYLT)
                .leggTilVilkår(VilkårType.OPPTJENINGSVILKÅRET, VilkårUtfallType.OPPFYLT);
        var behandling = scenario.lagMocked();

        var repositoryProvider = scenario.mockBehandlingRepositoryProvider();
        Behandlingsresultat.builderEndreEksisterende(behandling.getBehandlingsresultat())
                .medBehandlingResultatType(BehandlingResultatType.INNVILGET);
        kontekst = new BehandlingskontrollKontekst(behandling,
                repositoryProvider.getBehandlingRepository().taSkriveLås(behandling));

        var val = new RegelResultat(behandling.getBehandlingsresultat().getVilkårResultat(), emptyList(), emptyMap());
        when(regelOrkestrerer.vurderInngangsvilkår(eq(Set.of()), any(), any())).thenReturn(val);

        // Act
        var stegResultat = new SamletInngangsvilkårStegImpl(repositoryProvider, inngangsvilkårFellesTjeneste, ivTjeneste)
                .utførSteg(kontekst);

        // Assert
        assertThat(stegResultat.getTransisjon().stegTransisjon()).isEqualTo(StegTransisjon.UTFØRT);

    }

}
