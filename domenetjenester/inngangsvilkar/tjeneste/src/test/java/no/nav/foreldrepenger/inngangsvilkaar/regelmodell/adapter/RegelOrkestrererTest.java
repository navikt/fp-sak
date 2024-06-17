package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.adapter;

import static java.util.Collections.emptyList;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.MANUELL_VURDERING_AV_SØKNADSFRISTVILKÅRET;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType.IKKE_OPPFYLT;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType.IKKE_VURDERT;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType.OPPFYLT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.inngangsvilkaar.InngangsvilkårTjeneste;
import no.nav.foreldrepenger.inngangsvilkaar.RegelOrkestrerer;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårData;
import no.nav.vedtak.exception.TekniskException;

class RegelOrkestrererTest {

    private RegelOrkestrerer orkestrerer;

    private InngangsvilkårTjeneste inngangsvilkårTjeneste;

    @BeforeEach
    public void oppsett() {
        inngangsvilkårTjeneste = Mockito.mock(InngangsvilkårTjeneste.class);
        orkestrerer = new RegelOrkestrerer(inngangsvilkårTjeneste);
    }

    @Test
    void skal_kalle_regeltjeneste_for_fødselsvilkåret_og_oppdatere_vilkårresultat() {
        // Arrange
        var vilkårType = VilkårType.FØDSELSVILKÅRET_MOR;
        var vilkårData = new VilkårData(vilkårType, OPPFYLT, emptyList());
        when(inngangsvilkårTjeneste.finnVilkår(vilkårType, FagsakYtelseType.FORELDREPENGER)).thenReturn(b -> vilkårData);

        var behandling = byggBehandlingMedVilkårresultat(VilkårResultatType.IKKE_FASTSATT, vilkårType);
        when(inngangsvilkårTjeneste.getBehandlingsresultat(behandling.getId())).thenReturn(behandling.getBehandlingsresultat());

        // Act
        var regelResultat = orkestrerer.vurderInngangsvilkår(Set.of(vilkårType), behandling, BehandlingReferanse.fra(behandling));

        // Assert
        assertThat(regelResultat.vilkårResultat().getVilkårene()).hasSize(1);
        assertThat(regelResultat.vilkårResultat().getVilkårene().iterator().next().getVilkårType()).isEqualTo(vilkårType);
    }

    @Test
    void skal_kalle_regeltjeneste_for_adopsjonsvilkåret_og_oppdatere_vilkårresultat() {
        // Arrange
        var vilkårType = VilkårType.ADOPSJONSVILKÅRET_ENGANGSSTØNAD;
        var vilkårData = new VilkårData(vilkårType, OPPFYLT, emptyList());
        when(inngangsvilkårTjeneste.finnVilkår(vilkårType, FagsakYtelseType.FORELDREPENGER)).thenReturn(b -> vilkårData);
        var behandling = byggBehandlingMedVilkårresultat(VilkårResultatType.IKKE_FASTSATT, vilkårType);
        when(inngangsvilkårTjeneste.getBehandlingsresultat(behandling.getId())).thenReturn(behandling.getBehandlingsresultat());

        // Act
        var regelResultat = orkestrerer.vurderInngangsvilkår(Set.of(vilkårType), behandling, BehandlingReferanse.fra(behandling));

        // Assert
        assertThat(regelResultat.vilkårResultat().getVilkårene()).hasSize(1);
        assertThat(regelResultat.vilkårResultat().getVilkårene().iterator().next().getVilkårType()).isEqualTo(vilkårType);
    }

    @Test
    void skal_kalle_regeltjeneste_for_medlemskapvilkåret_og_oppdatere_vilkårresultat() {
        // Arrange
        var vilkårType = VilkårType.MEDLEMSKAPSVILKÅRET;
        var vilkårData = new VilkårData(vilkårType, OPPFYLT, emptyList());
        when(inngangsvilkårTjeneste.finnVilkår(vilkårType, FagsakYtelseType.FORELDREPENGER)).thenReturn(b -> vilkårData);
        var behandling = byggBehandlingMedVilkårresultat(VilkårResultatType.IKKE_FASTSATT, vilkårType);
        when(inngangsvilkårTjeneste.getBehandlingsresultat(behandling.getId())).thenReturn(behandling.getBehandlingsresultat());

        // Act
        var regelResultat = orkestrerer.vurderInngangsvilkår(Set.of(vilkårType), behandling, BehandlingReferanse.fra(behandling));

        // Assert
        assertThat(regelResultat.vilkårResultat().getVilkårene()).hasSize(1);
        assertThat(regelResultat.vilkårResultat().getVilkårene().iterator().next().getVilkårType()).isEqualTo(vilkårType);
    }

    @Test
    void skal_kalle_regeltjeneste_for_søknadsfristvilkåret_og_oppdatere_vilkårresultat() {
        // Arrange
        var vilkårType = VilkårType.SØKNADSFRISTVILKÅRET;
        var vilkårData = new VilkårData(vilkårType, OPPFYLT, emptyList());
        when(inngangsvilkårTjeneste.finnVilkår(vilkårType, FagsakYtelseType.FORELDREPENGER)).thenReturn(b -> vilkårData);
        var behandling = byggBehandlingMedVilkårresultat(VilkårResultatType.IKKE_FASTSATT, vilkårType);
        when(inngangsvilkårTjeneste.getBehandlingsresultat(behandling.getId())).thenReturn(behandling.getBehandlingsresultat());

        // Act
        var regelResultat = orkestrerer.vurderInngangsvilkår(Set.of(vilkårType), behandling, BehandlingReferanse.fra(behandling));

        // Assert
        assertThat(regelResultat.vilkårResultat().getVilkårene()).hasSize(1);
        assertThat(regelResultat.vilkårResultat().getVilkårene().iterator().next().getVilkårType()).isEqualTo(vilkårType);
    }

    @Test
    void skal_kalle_regeltjeneste_for_omsorgsvilkåret_og_oppdatere_vilkårresultat() {
        // Arrange
        var vilkårType = VilkårType.OMSORGSVILKÅRET;
        var vilkårData = new VilkårData(vilkårType, OPPFYLT, emptyList());
        when(inngangsvilkårTjeneste.finnVilkår(vilkårType, FagsakYtelseType.FORELDREPENGER)).thenReturn(b -> vilkårData);
        var behandling = byggBehandlingMedVilkårresultat(VilkårResultatType.IKKE_FASTSATT, vilkårType);
        when(inngangsvilkårTjeneste.getBehandlingsresultat(behandling.getId())).thenReturn(behandling.getBehandlingsresultat());

        // Act
        var regelResultat = orkestrerer.vurderInngangsvilkår(Set.of(vilkårType), behandling, BehandlingReferanse.fra(behandling));

        // Assert
        assertThat(regelResultat.vilkårResultat().getVilkårene()).hasSize(1);
        assertThat(regelResultat.vilkårResultat().getVilkårene().iterator().next().getVilkårType()).isEqualTo(vilkårType);
    }

    @Test
    void skal_kalle_regeltjeneste_for_foreldreansvarsvilkår1_og_oppdatere_vilkårresultat() {
        // Arrange
        var vilkårType = VilkårType.FORELDREANSVARSVILKÅRET_2_LEDD;
        var vilkårData = new VilkårData(vilkårType, OPPFYLT, emptyList());
        when(inngangsvilkårTjeneste.finnVilkår(vilkårType, FagsakYtelseType.FORELDREPENGER)).thenReturn(b -> vilkårData);
        var behandling = byggBehandlingMedVilkårresultat(VilkårResultatType.IKKE_FASTSATT, vilkårType);
        when(inngangsvilkårTjeneste.getBehandlingsresultat(behandling.getId())).thenReturn(behandling.getBehandlingsresultat());

        // Act
        var regelResultat = orkestrerer.vurderInngangsvilkår(Set.of(vilkårType), behandling, BehandlingReferanse.fra(behandling));

        // Assert
        assertThat(regelResultat.vilkårResultat().getVilkårene()).hasSize(1);
        assertThat(regelResultat.vilkårResultat().getVilkårene().iterator().next().getVilkårType()).isEqualTo(vilkårType);
    }

    @Test
    void skal_kalle_regeltjeneste_for_foreldreansvarsvilkår2_og_oppdatere_vilkårresultat() {
        // Arrange
        var vilkårType = VilkårType.FORELDREANSVARSVILKÅRET_4_LEDD;
        var vilkårData = new VilkårData(vilkårType, OPPFYLT, emptyList());
        when(inngangsvilkårTjeneste.finnVilkår(vilkårType, FagsakYtelseType.FORELDREPENGER)).thenReturn(b -> vilkårData);
        var behandling = byggBehandlingMedVilkårresultat(VilkårResultatType.IKKE_FASTSATT, vilkårType);
        when(inngangsvilkårTjeneste.getBehandlingsresultat(behandling.getId())).thenReturn(behandling.getBehandlingsresultat());

        // Act
        var regelResultat = orkestrerer.vurderInngangsvilkår(Set.of(vilkårType), behandling, BehandlingReferanse.fra(behandling));

        // Assert
        assertThat(regelResultat.vilkårResultat().getVilkårene()).hasSize(1);
        assertThat(regelResultat.vilkårResultat().getVilkårene().iterator().next().getVilkårType()).isEqualTo(vilkårType);
    }

    @Test
    void skal_ikke_returnere_aksjonspunkter_fra_regelmotor_dersom_allerede_overstyrt() {
        // Arrange
        var behandling = lagBehandling();

        var vilkårType = VilkårType.FØDSELSVILKÅRET_MOR;
        VilkårResultat.builder().overstyrVilkår(vilkårType, OPPFYLT, Avslagsårsak.UDEFINERT).buildFor(behandling);

        var vilkårData = new VilkårData(vilkårType, OPPFYLT, List.of(MANUELL_VURDERING_AV_SØKNADSFRISTVILKÅRET));
        when(inngangsvilkårTjeneste.finnVilkår(vilkårType, FagsakYtelseType.FORELDREPENGER)).thenReturn(b -> vilkårData);
        when(inngangsvilkårTjeneste.getBehandlingsresultat(behandling.getId())).thenReturn(behandling.getBehandlingsresultat());

        // Act
        var regelResultat = orkestrerer.vurderInngangsvilkår(Set.of(vilkårType), behandling, BehandlingReferanse.fra(behandling));

        // Assert
        assertThat(regelResultat.aksjonspunktDefinisjoner()).isEmpty();
    }

    @Test
    void skal_returnere_aksjonspunkter_fra_regelmotor_dersom_allerede_manuelt_vurdert() {
        // Arrange
        var behandling = lagBehandling();
        var vilkårType = VilkårType.FØDSELSVILKÅRET_MOR;

        VilkårResultat.builder().manueltVilkår(vilkårType, OPPFYLT, Avslagsårsak.UDEFINERT).buildFor(behandling);

        var vilkårData = new VilkårData(vilkårType, OPPFYLT, List.of(MANUELL_VURDERING_AV_SØKNADSFRISTVILKÅRET));
        when(inngangsvilkårTjeneste.finnVilkår(vilkårType, FagsakYtelseType.FORELDREPENGER)).thenReturn(b -> vilkårData);
        when(inngangsvilkårTjeneste.getBehandlingsresultat(behandling.getId())).thenReturn(behandling.getBehandlingsresultat());

        // Act
        var regelResultat = orkestrerer.vurderInngangsvilkår(Set.of(vilkårType), behandling, BehandlingReferanse.fra(behandling));

        // Assert
        assertThat(regelResultat.aksjonspunktDefinisjoner()).containsExactly(MANUELL_VURDERING_AV_SØKNADSFRISTVILKÅRET);
    }

    @Test
    void skal_bare_vurdere_vilkår_som_er_støttet_og_finnes_på_behandlingen() {
        // Arrange
        var behandling = lagBehandling();

        var adopsjonsvilkårType = VilkårType.ADOPSJONSVILKÅRET_ENGANGSSTØNAD;
        var fødselsvilkårType = VilkårType.FØDSELSVILKÅRET_MOR;
        var søknadsfristvilkårType = VilkårType.SØKNADSFRISTVILKÅRET;
        var vilkårStøttetAvSteg = Set.of(adopsjonsvilkårType, fødselsvilkårType);

        // Legg til vilkårne som automatiske, dvs hverken manuelt vurdert eller overstyrt
        VilkårResultat.builder().leggTilVilkårIkkeVurdert(søknadsfristvilkårType).leggTilVilkårIkkeVurdert(adopsjonsvilkårType).buildFor(behandling);

        var vilkårData = new VilkårData(adopsjonsvilkårType, OPPFYLT, emptyList());
        when(inngangsvilkårTjeneste.finnVilkår(adopsjonsvilkårType, FagsakYtelseType.FORELDREPENGER)).thenReturn(b -> vilkårData);
        when(inngangsvilkårTjeneste.getBehandlingsresultat(behandling.getId())).thenReturn(behandling.getBehandlingsresultat());

        // Act
        var regelResultat = orkestrerer.vurderInngangsvilkår(vilkårStøttetAvSteg, behandling, BehandlingReferanse.fra(behandling));

        // Assert
        verify(inngangsvilkårTjeneste).finnVilkår(adopsjonsvilkårType, FagsakYtelseType.FORELDREPENGER);
        verify(inngangsvilkårTjeneste, never()).finnVilkår(fødselsvilkårType, FagsakYtelseType.FORELDREPENGER);
        verify(inngangsvilkårTjeneste, never()).finnVilkår(søknadsfristvilkårType, FagsakYtelseType.FORELDREPENGER);

        var søknadsfristvilkår = regelResultat.vilkårResultat()
            .getVilkårene()
            .stream()
            .filter(vilkår -> vilkår.getVilkårType().equals(søknadsfristvilkårType))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Skal ikke kunne komme her"));
        var adopsjonsvilkår = regelResultat.vilkårResultat()
            .getVilkårene()
            .stream()
            .filter(vilkår -> vilkår.getVilkårType().equals(adopsjonsvilkårType))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Skal ikke kunne komme her"));
        assertThat(søknadsfristvilkår.getGjeldendeVilkårUtfall()).isEqualTo(VilkårUtfallType.IKKE_VURDERT);
        assertThat(adopsjonsvilkår.getGjeldendeVilkårUtfall()).isEqualTo(VilkårUtfallType.OPPFYLT);
        assertThat(regelResultat.vilkårResultat().getVilkårResultatType()).isEqualTo(VilkårResultatType.IKKE_FASTSATT);
    }

    @Test
    void skal_sammenstille_individuelle_vilkårsutfall_til_ett_samlet_vilkårresultat() {
        // Enkelt vilkårutfall
        assertThat(VilkårResultatType.utledInngangsvilkårUtfall(Set.of(IKKE_OPPFYLT))).isEqualTo(VilkårResultatType.AVSLÅTT);
        assertThat(VilkårResultatType.utledInngangsvilkårUtfall(Set.of(IKKE_VURDERT))).isEqualTo(VilkårResultatType.IKKE_FASTSATT);
        assertThat(VilkårResultatType.utledInngangsvilkårUtfall(Set.of(OPPFYLT))).isEqualTo(VilkårResultatType.INNVILGET);

        // Sammensatt vilkårutfall
        assertThat(VilkårResultatType.utledInngangsvilkårUtfall(Set.of(IKKE_OPPFYLT, IKKE_VURDERT))).isEqualTo(VilkårResultatType.AVSLÅTT);
        assertThat(VilkårResultatType.utledInngangsvilkårUtfall(Set.of(IKKE_OPPFYLT, OPPFYLT))).isEqualTo(VilkårResultatType.AVSLÅTT);

        assertThat(VilkårResultatType.utledInngangsvilkårUtfall(Set.of(IKKE_VURDERT, OPPFYLT))).isEqualTo(VilkårResultatType.IKKE_FASTSATT);
    }

    @Test
    void skal_kaste_feil_dersom_vilkårsresultat_ikke_kan_utledes() {
        Set<VilkårUtfallType> utfall = Set.of();
        assertThrows(TekniskException.class, () -> VilkårResultatType.utledInngangsvilkårUtfall(utfall));
    }

    private Behandling byggBehandlingMedVilkårresultat(VilkårResultatType vilkårResultatType, VilkårType vilkårType) {
        var behandling = lagBehandling();
        VilkårResultat.builder().medVilkårResultatType(vilkårResultatType).leggTilVilkårIkkeVurdert(vilkårType).buildFor(behandling);
        return behandling;
    }

    private Behandling lagBehandling() {
        return ScenarioMorSøkerForeldrepenger.forFødsel().lagMocked();
    }

}
