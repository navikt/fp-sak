package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.adapter;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.MANUELL_VURDERING_AV_SØKNADSFRISTVILKÅRET;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType.IKKE_OPPFYLT;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType.IKKE_VURDERT;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType.OPPFYLT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.inngangsvilkaar.InngangsvilkårTjeneste;
import no.nav.foreldrepenger.inngangsvilkaar.RegelOrkestrerer;
import no.nav.foreldrepenger.inngangsvilkaar.RegelResultat;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårData;
import no.nav.vedtak.exception.TekniskException;

public class RegelOrkestrererTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private RegelOrkestrerer orkestrerer;

    private InngangsvilkårTjeneste inngangsvilkårTjeneste;

    @Before
    public void oppsett() {
        inngangsvilkårTjeneste = Mockito.mock(InngangsvilkårTjeneste.class);

        orkestrerer = new RegelOrkestrerer(inngangsvilkårTjeneste);
    }

    @Test
    public void skal_kalle_regeltjeneste_for_fødselsvilkåret_og_oppdatere_vilkårresultat() {
        // Arrange
        VilkårType vilkårType = VilkårType.FØDSELSVILKÅRET_MOR;
        VilkårData vilkårData = new VilkårData(vilkårType, OPPFYLT, emptyList());
        when(inngangsvilkårTjeneste.finnVilkår(vilkårType, FagsakYtelseType.FORELDREPENGER)).thenReturn((b) -> vilkårData);

        Behandling behandling = byggBehandlingMedVilkårresultat(VilkårResultatType.IKKE_FASTSATT, vilkårType);
        when(inngangsvilkårTjeneste.getBehandlingsresultat(behandling.getId())).thenReturn(behandling.getBehandlingsresultat());

        // Act
        RegelResultat regelResultat = orkestrerer.vurderInngangsvilkår(Set.of(vilkårType), behandling, BehandlingReferanse.fra(behandling));

        // Assert
        assertThat(regelResultat.getVilkårResultat().getVilkårene()).hasSize(1);
        assertThat(regelResultat.getVilkårResultat().getVilkårene().iterator().next().getVilkårType())
                .isEqualTo(vilkårType);
    }

    @Test
    public void skal_kalle_regeltjeneste_for_adopsjonsvilkåret_og_oppdatere_vilkårresultat() {
        // Arrange
        VilkårType vilkårType = VilkårType.ADOPSJONSVILKÅRET_ENGANGSSTØNAD;
        VilkårData vilkårData = new VilkårData(vilkårType, OPPFYLT, emptyList());
        when(inngangsvilkårTjeneste.finnVilkår(vilkårType, FagsakYtelseType.FORELDREPENGER)).thenReturn((b) -> vilkårData);
        Behandling behandling = byggBehandlingMedVilkårresultat(VilkårResultatType.IKKE_FASTSATT, vilkårType);
        when(inngangsvilkårTjeneste.getBehandlingsresultat(behandling.getId())).thenReturn(behandling.getBehandlingsresultat());

        // Act
        RegelResultat regelResultat = orkestrerer.vurderInngangsvilkår(Set.of(vilkårType), behandling, BehandlingReferanse.fra(behandling));

        // Assert
        assertThat(regelResultat.getVilkårResultat().getVilkårene()).hasSize(1);
        assertThat(regelResultat.getVilkårResultat().getVilkårene().iterator().next().getVilkårType())
                .isEqualTo(vilkårType);
    }

    @Test
    public void skal_kalle_regeltjeneste_for_medlemskapvilkåret_og_oppdatere_vilkårresultat() {
        // Arrange
        VilkårType vilkårType = VilkårType.MEDLEMSKAPSVILKÅRET;
        VilkårData vilkårData = new VilkårData(vilkårType, OPPFYLT, emptyList());
        when(inngangsvilkårTjeneste.finnVilkår(vilkårType, FagsakYtelseType.FORELDREPENGER)).thenReturn((b) -> vilkårData);
        Behandling behandling = byggBehandlingMedVilkårresultat(VilkårResultatType.IKKE_FASTSATT, vilkårType);
        when(inngangsvilkårTjeneste.getBehandlingsresultat(behandling.getId())).thenReturn(behandling.getBehandlingsresultat());

        // Act
        RegelResultat regelResultat = orkestrerer.vurderInngangsvilkår(Set.of(vilkårType), behandling, BehandlingReferanse.fra(behandling));

        // Assert
        assertThat(regelResultat.getVilkårResultat().getVilkårene()).hasSize(1);
        assertThat(regelResultat.getVilkårResultat().getVilkårene().iterator().next().getVilkårType())
                .isEqualTo(vilkårType);
    }

    @Test
    public void skal_kalle_regeltjeneste_for_søknadsfristvilkåret_og_oppdatere_vilkårresultat() {
        // Arrange
        VilkårType vilkårType = VilkårType.SØKNADSFRISTVILKÅRET;
        VilkårData vilkårData = new VilkårData(vilkårType, OPPFYLT, emptyList());
        when(inngangsvilkårTjeneste.finnVilkår(vilkårType, FagsakYtelseType.FORELDREPENGER)).thenReturn((b) -> vilkårData);
        Behandling behandling = byggBehandlingMedVilkårresultat(VilkårResultatType.IKKE_FASTSATT, vilkårType);
        when(inngangsvilkårTjeneste.getBehandlingsresultat(behandling.getId())).thenReturn(behandling.getBehandlingsresultat());

        // Act
        RegelResultat regelResultat = orkestrerer.vurderInngangsvilkår(Set.of(vilkårType), behandling, BehandlingReferanse.fra(behandling));

        // Assert
        assertThat(regelResultat.getVilkårResultat().getVilkårene()).hasSize(1);
        assertThat(regelResultat.getVilkårResultat().getVilkårene().iterator().next().getVilkårType())
                .isEqualTo(vilkårType);
    }

    @Test
    public void skal_kalle_regeltjeneste_for_omsorgsvilkåret_og_oppdatere_vilkårresultat() {
        // Arrange
        VilkårType vilkårType = VilkårType.OMSORGSVILKÅRET;
        VilkårData vilkårData = new VilkårData(vilkårType, OPPFYLT, emptyList());
        when(inngangsvilkårTjeneste.finnVilkår(vilkårType, FagsakYtelseType.FORELDREPENGER)).thenReturn((b) -> vilkårData);
        Behandling behandling = byggBehandlingMedVilkårresultat(VilkårResultatType.IKKE_FASTSATT, vilkårType);
        when(inngangsvilkårTjeneste.getBehandlingsresultat(behandling.getId())).thenReturn(behandling.getBehandlingsresultat());

        // Act
        RegelResultat regelResultat = orkestrerer.vurderInngangsvilkår(Set.of(vilkårType), behandling, BehandlingReferanse.fra(behandling));

        // Assert
        assertThat(regelResultat.getVilkårResultat().getVilkårene()).hasSize(1);
        assertThat(regelResultat.getVilkårResultat().getVilkårene().iterator().next().getVilkårType())
                .isEqualTo(vilkårType);
    }

    @Test
    public void skal_kalle_regeltjeneste_for_foreldreansvarsvilkår1_og_oppdatere_vilkårresultat() {
        // Arrange
        VilkårType vilkårType = VilkårType.FORELDREANSVARSVILKÅRET_2_LEDD;
        VilkårData vilkårData = new VilkårData(vilkårType, OPPFYLT, emptyList());
        when(inngangsvilkårTjeneste.finnVilkår(vilkårType, FagsakYtelseType.FORELDREPENGER)).thenReturn((b) -> vilkårData);
        Behandling behandling = byggBehandlingMedVilkårresultat(VilkårResultatType.IKKE_FASTSATT, vilkårType);
        when(inngangsvilkårTjeneste.getBehandlingsresultat(behandling.getId())).thenReturn(behandling.getBehandlingsresultat());

        // Act
        RegelResultat regelResultat = orkestrerer.vurderInngangsvilkår(Set.of(vilkårType), behandling, BehandlingReferanse.fra(behandling));

        // Assert
        assertThat(regelResultat.getVilkårResultat().getVilkårene()).hasSize(1);
        assertThat(regelResultat.getVilkårResultat().getVilkårene().iterator().next().getVilkårType())
                .isEqualTo(vilkårType);
    }

    @Test
    public void skal_kalle_regeltjeneste_for_foreldreansvarsvilkår2_og_oppdatere_vilkårresultat() {
        // Arrange
        VilkårType vilkårType = VilkårType.FORELDREANSVARSVILKÅRET_4_LEDD;
        VilkårData vilkårData = new VilkårData(vilkårType, OPPFYLT, emptyList());
        when(inngangsvilkårTjeneste.finnVilkår(vilkårType, FagsakYtelseType.FORELDREPENGER)).thenReturn((b) -> vilkårData);
        Behandling behandling = byggBehandlingMedVilkårresultat(VilkårResultatType.IKKE_FASTSATT, vilkårType);
        when(inngangsvilkårTjeneste.getBehandlingsresultat(behandling.getId())).thenReturn(behandling.getBehandlingsresultat());

        // Act
        RegelResultat regelResultat = orkestrerer.vurderInngangsvilkår(Set.of(vilkårType), behandling, BehandlingReferanse.fra(behandling));

        // Assert
        assertThat(regelResultat.getVilkårResultat().getVilkårene()).hasSize(1);
        assertThat(regelResultat.getVilkårResultat().getVilkårene().iterator().next().getVilkårType())
                .isEqualTo(vilkårType);
    }

    @Test
    public void skal_ikke_returnere_aksjonspunkter_fra_regelmotor_dersom_allerede_overstyrt() {
        // Arrange
        Behandling behandling = lagBehandling();

        VilkårType vilkårType = VilkårType.FØDSELSVILKÅRET_MOR;
        boolean erOverstyrt = true;
        VilkårResultat.builder()
                .leggTilVilkårResultat(vilkårType, OPPFYLT, null, null, null, false, erOverstyrt, null, null)
                .buildFor(behandling);

        VilkårData vilkårData = new VilkårData(vilkårType, OPPFYLT, List.of(MANUELL_VURDERING_AV_SØKNADSFRISTVILKÅRET));
        when(inngangsvilkårTjeneste.finnVilkår(vilkårType, FagsakYtelseType.FORELDREPENGER)).thenReturn((b) -> vilkårData);
        when(inngangsvilkårTjeneste.getBehandlingsresultat(behandling.getId())).thenReturn(behandling.getBehandlingsresultat());

        // Act
        RegelResultat regelResultat = orkestrerer.vurderInngangsvilkår(Set.of(vilkårType), behandling, BehandlingReferanse.fra(behandling));

        // Assert
        assertThat(regelResultat.getAksjonspunktDefinisjoner()).isEmpty();
    }

    @Test
    public void skal_returnere_aksjonspunkter_fra_regelmotor_dersom_allerede_manuelt_vurdert() {
        // Arrange
        Behandling behandling = lagBehandling();
        VilkårType vilkårType = VilkårType.FØDSELSVILKÅRET_MOR;

        boolean manueltVurdert = true;
        VilkårResultat.builder()
            .leggTilVilkårResultat(vilkårType, OPPFYLT, null, null, null, manueltVurdert, false, null, null)
            .buildFor(behandling);

        VilkårData vilkårData = new VilkårData(vilkårType, OPPFYLT, List.of(MANUELL_VURDERING_AV_SØKNADSFRISTVILKÅRET));
        when(inngangsvilkårTjeneste.finnVilkår(vilkårType, FagsakYtelseType.FORELDREPENGER)).thenReturn((b) -> vilkårData);
        when(inngangsvilkårTjeneste.getBehandlingsresultat(behandling.getId())).thenReturn(behandling.getBehandlingsresultat());

        // Act
        RegelResultat regelResultat = orkestrerer.vurderInngangsvilkår(Set.of(vilkårType), behandling, BehandlingReferanse.fra(behandling));

        // Assert
        assertThat(regelResultat.getAksjonspunktDefinisjoner()).containsExactly(MANUELL_VURDERING_AV_SØKNADSFRISTVILKÅRET);
    }

    @Test
    public void skal_bare_vurdere_vilkår_som_er_støttet_og_finnes_på_behandlingen() {
        // Arrange
        Behandling behandling = lagBehandling();

        VilkårType adopsjonsvilkårType = VilkårType.ADOPSJONSVILKÅRET_ENGANGSSTØNAD;
        VilkårType fødselsvilkårType = VilkårType.FØDSELSVILKÅRET_MOR;
        VilkårType søknadsfristvilkårType = VilkårType.SØKNADSFRISTVILKÅRET;
        Set<VilkårType> vilkårStøttetAvSteg = Set.of(adopsjonsvilkårType, fødselsvilkårType);

        // Legg til vilkårne som automatiske, dvs hverken manuelt vurdert eller overstyrt
        VilkårResultat.builder()
                .leggTilVilkårResultat(søknadsfristvilkårType, IKKE_VURDERT, null, null, null, false, false, null, null)
                .leggTilVilkårResultat(adopsjonsvilkårType, IKKE_VURDERT, null, null, null, false, false, null, null)
                .buildFor(behandling);

        VilkårData vilkårData = new VilkårData(adopsjonsvilkårType, OPPFYLT, emptyList());
        when(inngangsvilkårTjeneste.finnVilkår(adopsjonsvilkårType, FagsakYtelseType.FORELDREPENGER)).thenReturn((b) -> vilkårData);
        when(inngangsvilkårTjeneste.getBehandlingsresultat(behandling.getId())).thenReturn(behandling.getBehandlingsresultat());

        // Act
        RegelResultat regelResultat = orkestrerer.vurderInngangsvilkår(vilkårStøttetAvSteg, behandling, BehandlingReferanse.fra(behandling));

        // Assert
        verify(inngangsvilkårTjeneste).finnVilkår(adopsjonsvilkårType, FagsakYtelseType.FORELDREPENGER);
        verify(inngangsvilkårTjeneste, never()).finnVilkår(fødselsvilkårType, FagsakYtelseType.FORELDREPENGER);
        verify(inngangsvilkårTjeneste, never()).finnVilkår(søknadsfristvilkårType, FagsakYtelseType.FORELDREPENGER);

        Vilkår søknadsfristvilkår = regelResultat.getVilkårResultat().getVilkårene().stream()
                .filter(vilkår -> vilkår.getVilkårType().equals(søknadsfristvilkårType))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Skal ikke kunne komme her"));
        Vilkår adopsjonsvilkår = regelResultat.getVilkårResultat().getVilkårene().stream()
                .filter(vilkår -> vilkår.getVilkårType().equals(adopsjonsvilkårType))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Skal ikke kunne komme her"));
        assertThat(søknadsfristvilkår.getGjeldendeVilkårUtfall()).isEqualTo(VilkårUtfallType.IKKE_VURDERT);
        assertThat(adopsjonsvilkår.getGjeldendeVilkårUtfall()).isEqualTo(VilkårUtfallType.OPPFYLT);
        assertThat(regelResultat.getVilkårResultat().getVilkårResultatType()).isEqualTo(VilkårResultatType.IKKE_FASTSATT);
    }

    @Test
    public void skal_sammenstille_individuelle_vilkårsutfall_til_ett_samlet_vilkårresultat() {
        // Enkelt vilkårutfall
        assertThat(orkestrerer.utledInngangsvilkårUtfall(Set.of(IKKE_OPPFYLT))).isEqualTo(VilkårResultatType.AVSLÅTT);
        assertThat(orkestrerer.utledInngangsvilkårUtfall(Set.of(IKKE_VURDERT))).isEqualTo(VilkårResultatType.IKKE_FASTSATT);
        assertThat(orkestrerer.utledInngangsvilkårUtfall(Set.of(OPPFYLT))).isEqualTo(VilkårResultatType.INNVILGET);

        // Sammensatt vilkårutfall
        assertThat(orkestrerer.utledInngangsvilkårUtfall(asList(IKKE_OPPFYLT, IKKE_VURDERT))).isEqualTo(VilkårResultatType.AVSLÅTT);
        assertThat(orkestrerer.utledInngangsvilkårUtfall(asList(IKKE_OPPFYLT, OPPFYLT))).isEqualTo(VilkårResultatType.AVSLÅTT);

        assertThat(orkestrerer.utledInngangsvilkårUtfall(asList(IKKE_VURDERT, OPPFYLT))).isEqualTo(VilkårResultatType.IKKE_FASTSATT);
    }

    @Test
    public void skal_kaste_feil_dersom_vilkårsresultat_ikke_kan_utledes() {
        expectedException.expect(TekniskException.class);

        orkestrerer.utledInngangsvilkårUtfall(emptyList());
    }

    private Behandling byggBehandlingMedVilkårresultat(VilkårResultatType vilkårResultatType, VilkårType vilkårType) {
        Behandling behandling = lagBehandling();
        VilkårResultat.builder().medVilkårResultatType(vilkårResultatType)
            .leggTilVilkår(vilkårType, IKKE_VURDERT).buildFor(behandling);
        return behandling;
    }

    private Behandling lagBehandling() {
        return ScenarioMorSøkerForeldrepenger.forFødsel().lagMocked();
    }

}
