package no.nav.foreldrepenger.behandling.steg.inngangsvilkår;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner.FREMHOPP_TIL_UTTAKSPLAN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.TransisjonIdentifikator;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.inngangsvilkaar.InngangsvilkårTjeneste;
import no.nav.foreldrepenger.inngangsvilkaar.RegelOrkestrerer;
import no.nav.foreldrepenger.inngangsvilkaar.RegelResultat;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

public class SamletInngangsvilkårStegImplTest {

    @Rule
    public MockitoRule mockito = MockitoJUnit.rule().silent();

    private BehandlingskontrollKontekst kontekst;

    @Mock
    private RegelOrkestrerer regelOrkestrerer;
    @Mock
    private InngangsvilkårTjeneste ivTjeneste;

    private InngangsvilkårFellesTjeneste inngangsvilkårFellesTjeneste;

    @Before
    public void before() {
        initMocks(this);
        inngangsvilkårFellesTjeneste = new InngangsvilkårFellesTjeneste(regelOrkestrerer, mock(SkjæringstidspunktTjeneste.class));
        when(ivTjeneste.erInngangsvilkår(any())).thenReturn(true);
    }

    @Test
    public void skal_gjenskape_overstyring_mellomliggende_vilkår() {
        // Arrange
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.REVURDERING)
            .medVilkårResultatType(VilkårResultatType.IKKE_FASTSATT)
            .leggTilVilkår(VilkårType.SØKERSOPPLYSNINGSPLIKT, VilkårUtfallType.IKKE_OPPFYLT)
            .leggTilVilkår(VilkårType.FØDSELSVILKÅRET_MOR, VilkårUtfallType.IKKE_VURDERT)
            .leggTilVilkår(VilkårType.MEDLEMSKAPSVILKÅRET, VilkårUtfallType.IKKE_VURDERT)
            .leggTilVilkår(VilkårType.OPPTJENINGSVILKÅRET, VilkårUtfallType.OPPFYLT);
        Behandling behandling = scenario.lagMocked();

        BehandlingRepositoryProvider repositoryProvider = scenario.mockBehandlingRepositoryProvider();
        Behandlingsresultat.builderEndreEksisterende(behandling.getBehandlingsresultat())
            .medBehandlingResultatType(BehandlingResultatType.INNVILGET);
        kontekst = new BehandlingskontrollKontekst(behandling.getFagsakId(), behandling.getAktørId(), repositoryProvider.getBehandlingRepository().taSkriveLås(behandling));

        RegelResultat val = new RegelResultat(behandling.getBehandlingsresultat().getVilkårResultat(), emptyList(), emptyMap());
        when(regelOrkestrerer.vurderInngangsvilkår(eq(Set.of()), any(), any())).thenReturn(val);

        // Act
        BehandleStegResultat stegResultat = new SamletInngangsvilkårStegImpl(repositoryProvider, inngangsvilkårFellesTjeneste, ivTjeneste).utførSteg(kontekst);

        // Assert
        assertThat(stegResultat.getTransisjon()).isEqualTo(TransisjonIdentifikator.forId(FREMHOPP_TIL_UTTAKSPLAN.getId()));

        VilkårResultat vilkårResultat = behandling.getBehandlingsresultat().getVilkårResultat();
        assertThat(vilkårResultat.getVilkårResultatType()).isEqualTo(VilkårResultatType.IKKE_FASTSATT);
    }

    @Test
    public void alle_vilkår_oppfylt() {
        // Arrange
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.REVURDERING)
            .medVilkårResultatType(VilkårResultatType.IKKE_FASTSATT)
            .leggTilVilkår(VilkårType.SØKERSOPPLYSNINGSPLIKT, VilkårUtfallType.OPPFYLT)
            .leggTilVilkår(VilkårType.FØDSELSVILKÅRET_MOR, VilkårUtfallType.OPPFYLT)
            .leggTilVilkår(VilkårType.MEDLEMSKAPSVILKÅRET, VilkårUtfallType.OPPFYLT)
            .leggTilVilkår(VilkårType.OPPTJENINGSVILKÅRET, VilkårUtfallType.OPPFYLT);
        Behandling behandling = scenario.lagMocked();

        BehandlingRepositoryProvider repositoryProvider = scenario.mockBehandlingRepositoryProvider();
        Behandlingsresultat.builderEndreEksisterende(behandling.getBehandlingsresultat())
            .medBehandlingResultatType(BehandlingResultatType.INNVILGET);
        kontekst = new BehandlingskontrollKontekst(behandling.getFagsakId(), behandling.getAktørId(), repositoryProvider.getBehandlingRepository().taSkriveLås(behandling));

        RegelResultat val = new RegelResultat(behandling.getBehandlingsresultat().getVilkårResultat(), emptyList(), emptyMap());
        when(regelOrkestrerer.vurderInngangsvilkår(eq(Set.of()), any(), any())).thenReturn(val);

        // Act
        BehandleStegResultat stegResultat = new SamletInngangsvilkårStegImpl(repositoryProvider, inngangsvilkårFellesTjeneste, ivTjeneste).utførSteg(kontekst);

        // Assert
        assertThat(stegResultat.getTransisjon()).isEqualTo(TransisjonIdentifikator.forId(FellesTransisjoner.UTFØRT.getId()));

    }

}
