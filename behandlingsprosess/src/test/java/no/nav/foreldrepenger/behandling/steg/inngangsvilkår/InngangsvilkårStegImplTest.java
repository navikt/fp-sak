package no.nav.foreldrepenger.behandling.steg.inngangsvilkår;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.List.of;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.steg.inngangsvilkår.opptjening.fp.VurderOpptjeningsvilkårSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.StegTransisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.inngangsvilkaar.RegelOrkestrerer;
import no.nav.foreldrepenger.inngangsvilkaar.RegelResultat;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.OpptjeningsvilkårResultat;

@ExtendWith(MockitoExtension.class)
class InngangsvilkårStegImplTest {

    private final VilkårType medlVilkårType = VilkårType.MEDLEMSKAPSVILKÅRET;
    private final VilkårType oppVilkårType = VilkårType.OPPTJENINGSVILKÅRET;

    @Mock
    private BehandlingStegModell modell;

    @Mock
    private RegelOrkestrerer regelOrkestrerer;

    @Test
    void skal_hoppe_til_uttak_ved_avslag_for_foreldrepenger_ved_revurdering() {
        // Arrange
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medBehandlingType(BehandlingType.REVURDERING)
                .leggTilVilkår(medlVilkårType, VilkårUtfallType.IKKE_OPPFYLT);
        var behandling = scenario.lagMocked();

        var repositoryProvider = scenario.mockBehandlingRepositoryProvider();
        Behandlingsresultat.builderEndreEksisterende(behandling.getBehandlingsresultat())
                .medBehandlingResultatType(BehandlingResultatType.INNVILGET);
        var kontekst = new BehandlingskontrollKontekst(behandling.getSaksnummer(), behandling.getFagsakId(),
                repositoryProvider.getBehandlingRepository().taSkriveLås(behandling));

        var val = new RegelResultat(behandling.getBehandlingsresultat().getVilkårResultat(), emptyList(), emptyMap());
        when(regelOrkestrerer.vurderInngangsvilkår(eq(Set.of(medlVilkårType)), any(), any())).thenReturn(val);
        var inngangsvilkårFellesTjeneste = new InngangsvilkårFellesTjeneste(regelOrkestrerer);
        // Act
        var stegResultat = new SutMedlemskapsvilkårSteg(repositoryProvider, inngangsvilkårFellesTjeneste).utførSteg(kontekst);

        // Assert
        assertThat(stegResultat.getTransisjon().stegTransisjon()).isEqualTo(StegTransisjon.HOPPOVER);
        assertThat(stegResultat.getTransisjon().målSteg()).isEqualTo(BehandlingStegType.INNGANG_UTTAK);

        var vilkårResultat = behandling.getBehandlingsresultat().getVilkårResultat();
        assertThat(vilkårResultat.getVilkårene().stream().map(Vilkår::getGjeldendeVilkårUtfall).collect(toList()))
                .containsExactly(VilkårUtfallType.IKKE_OPPFYLT);
    }

    @Test
    void skal_gi_aksjonspunkt_ved_avslag_på_opptjening_for_foreldrepenger_ved_førstegang() {
        // Arrange
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD)
                .leggTilVilkår(oppVilkårType, VilkårUtfallType.IKKE_OPPFYLT);
        var behandling = scenario.lagMocked();

        var repositoryProvider = scenario.mockBehandlingRepositoryProvider();
        Behandlingsresultat.builderEndreEksisterende(behandling.getBehandlingsresultat())
                .medBehandlingResultatType(BehandlingResultatType.INNVILGET);
        var kontekst = new BehandlingskontrollKontekst(behandling.getSaksnummer(), behandling.getFagsakId(),
                repositoryProvider.getBehandlingRepository().taSkriveLås(behandling));

        var val = lagRegelResultatOpptjening(behandling);
        when(regelOrkestrerer.vurderInngangsvilkår(eq(Set.of(oppVilkårType)), any(), any())).thenReturn(val);
        var inngangsvilkårFellesTjeneste = new InngangsvilkårFellesTjeneste(regelOrkestrerer);
        // Act
        var stegResultat = new SutOpptjeningSteg(repositoryProvider, inngangsvilkårFellesTjeneste)
                .utførSteg(kontekst);

        // Assert
        assertThat(stegResultat.getAksjonspunktListe()).contains(AksjonspunktDefinisjon.VURDER_OPPTJENINGSVILKÅRET);

        var vilkårResultat = behandling.getBehandlingsresultat().getVilkårResultat();
        assertThat(vilkårResultat.getVilkårene().stream().map(Vilkår::getGjeldendeVilkårUtfall).collect(toList()))
                .containsExactly(VilkårUtfallType.IKKE_OPPFYLT);
    }

    @Test
    void skal_gi_aksjonspunkt_ved_avslag_på_opptjening_for_foreldrepenger_ved_revurdering() {
        // Arrange
        var førstegangsscenario = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD)
                .leggTilVilkår(oppVilkårType, VilkårUtfallType.OPPFYLT)
                .medBehandlingsresultat(Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET));
        var førstegangsbehandling = førstegangsscenario.lagMocked();
        førstegangsbehandling.avsluttBehandling();

        var revurderingsscenario = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medBehandlingType(BehandlingType.REVURDERING)
                .leggTilVilkår(oppVilkårType, VilkårUtfallType.IKKE_OPPFYLT)
                .medOriginalBehandling(førstegangsbehandling, BehandlingÅrsakType.RE_HENDELSE_FØDSEL);
        var revurdering = revurderingsscenario.lagMocked();

        var repositoryProvider = revurderingsscenario.mockBehandlingRepositoryProvider();
        Behandlingsresultat.builderEndreEksisterende(revurdering.getBehandlingsresultat())
                .medBehandlingResultatType(BehandlingResultatType.AVSLÅTT);
        var kontekst = new BehandlingskontrollKontekst(revurdering.getSaksnummer(), revurdering.getFagsakId(),
                repositoryProvider.getBehandlingRepository().taSkriveLås(revurdering));

        var val = lagRegelResultatOpptjening(revurdering);
        when(regelOrkestrerer.vurderInngangsvilkår(eq(Set.of(oppVilkårType)), any(), any())).thenReturn(val);
        var inngangsvilkårFellesTjeneste = new InngangsvilkårFellesTjeneste(regelOrkestrerer);
        // Act
        var stegResultat = new SutOpptjeningSteg(repositoryProvider, inngangsvilkårFellesTjeneste)
                .utførSteg(kontekst);

        // Assert
        assertThat(stegResultat.getAksjonspunktListe()).contains(AksjonspunktDefinisjon.VURDER_OPPTJENINGSVILKÅRET);
    }

    private RegelResultat lagRegelResultatOpptjening(Behandling behandling) {
        var ekstra = new OpptjeningsvilkårResultat();
        ekstra.setUnderkjentePerioder(emptyMap());
        ekstra.setBekreftetGodkjentAktivitet(emptyMap());
        ekstra.setAntattGodkjentePerioder(emptyMap());
        ekstra.setAkseptertMellomliggendePerioder(emptyMap());
        return new RegelResultat(behandling.getBehandlingsresultat().getVilkårResultat(), emptyList(),
                Map.of(VilkårType.OPPTJENINGSVILKÅRET, ekstra));
    }

    @Test
    void skal_hoppe_til_uttak_når_forrige_behandling_ikke_er_avslått_og_opptjeningsvilkåret_er_oppfylt_for_foreldrepenger_ved_revurdering() {
        // Arrange
        var førstegangsscenario = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD)
                .leggTilVilkår(oppVilkårType, VilkårUtfallType.OPPFYLT)
                .medBehandlingsresultat(Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET));
        var førstegangsbehandling = førstegangsscenario.lagMocked();
        førstegangsbehandling.avsluttBehandling();

        var revurderingsscenario = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medBehandlingType(BehandlingType.REVURDERING)
                .leggTilVilkår(medlVilkårType, VilkårUtfallType.IKKE_OPPFYLT)
                .leggTilVilkår(oppVilkårType, VilkårUtfallType.OPPFYLT)
                .medOriginalBehandling(førstegangsbehandling, BehandlingÅrsakType.RE_HENDELSE_FØDSEL);
        var revurdering = revurderingsscenario.lagMocked();

        var repositoryProvider = revurderingsscenario.mockBehandlingRepositoryProvider();
        Behandlingsresultat.builderEndreEksisterende(revurdering.getBehandlingsresultat())
                .medBehandlingResultatType(BehandlingResultatType.INNVILGET);
        var kontekst = new BehandlingskontrollKontekst(revurdering.getSaksnummer(), revurdering.getFagsakId(),
                repositoryProvider.getBehandlingRepository().taSkriveLås(revurdering));

        var val = new RegelResultat(revurdering.getBehandlingsresultat().getVilkårResultat(), emptyList(), emptyMap());
        when(regelOrkestrerer.vurderInngangsvilkår(eq(Set.of(oppVilkårType, medlVilkårType)), any(), any())).thenReturn(val);
        var inngangsvilkårFellesTjeneste = new InngangsvilkårFellesTjeneste(regelOrkestrerer);
        // Act
        var stegResultat = new SutOpptjeningOgMedlVilkårSteg(repositoryProvider, inngangsvilkårFellesTjeneste).utførSteg(kontekst);

        // Assert
        assertThat(stegResultat.getTransisjon().stegTransisjon()).isEqualTo(StegTransisjon.HOPPOVER);
        assertThat(stegResultat.getTransisjon().målSteg()).isEqualTo(BehandlingStegType.INNGANG_UTTAK);
    }

    @Test
    void skal_ikke_hoppe_til_uttak_når_forrige_behandling_er_avslått_for_foreldrepenger_ved_revurdering() {
        // Arrange
        var førstegangsscenario = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD)
                .leggTilVilkår(oppVilkårType, VilkårUtfallType.IKKE_OPPFYLT)
                .medBehandlingsresultat(Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.AVSLÅTT));
        var førstegangsbehandling = førstegangsscenario.lagMocked();
        førstegangsbehandling.avsluttBehandling();

        var revurderingsscenario = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medBehandlingType(BehandlingType.REVURDERING)
                .leggTilVilkår(medlVilkårType, VilkårUtfallType.IKKE_OPPFYLT)
                .leggTilVilkår(oppVilkårType, VilkårUtfallType.OPPFYLT)
                .medOriginalBehandling(førstegangsbehandling, BehandlingÅrsakType.RE_HENDELSE_FØDSEL);
        var revurdering = revurderingsscenario.lagMocked();

        var repositoryProvider = revurderingsscenario.mockBehandlingRepositoryProvider();
        Behandlingsresultat.builderEndreEksisterende(revurdering.getBehandlingsresultat())
                .medBehandlingResultatType(BehandlingResultatType.INNVILGET);
        var kontekst = new BehandlingskontrollKontekst(revurdering.getSaksnummer(), revurdering.getFagsakId(),
                repositoryProvider.getBehandlingRepository().taSkriveLås(revurdering));

        var val = new RegelResultat(revurdering.getBehandlingsresultat().getVilkårResultat(), emptyList(), emptyMap());
        when(regelOrkestrerer.vurderInngangsvilkår(eq(Set.of(oppVilkårType, medlVilkårType)), any(), any())).thenReturn(val);
        var behrepo = revurderingsscenario.mockBehandlingRepository();
        when(behrepo.hentBehandling(førstegangsbehandling.getId())).thenReturn(førstegangsbehandling);
        var inngangsvilkårFellesTjeneste = new InngangsvilkårFellesTjeneste(regelOrkestrerer);
        // Act
        var stegResultat = new SutOpptjeningOgMedlVilkårSteg(repositoryProvider, inngangsvilkårFellesTjeneste).utførSteg(kontekst);

        // Assert
        assertThat(stegResultat.getTransisjon().stegTransisjon()).isEqualTo(StegTransisjon.HOPPOVER);
        assertThat(stegResultat.getTransisjon().målSteg()).isEqualTo(BehandlingStegType.FORESLÅ_BEHANDLINGSRESULTAT);
    }

    @Test
    void skal_ikke_hoppe_til_uttak_når_en_tidligere_behandling_er_avslått_selv_om_det_finnes_et_beslutningsvedtak_imellom() {
        // Arrange
        var førstegangsscenario = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD)
                .leggTilVilkår(oppVilkårType, VilkårUtfallType.IKKE_OPPFYLT)
                .medBehandlingsresultat(Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.AVSLÅTT));
        var førstegangsbehandling = førstegangsscenario.lagMocked();
        førstegangsbehandling.avsluttBehandling();

        var førsteRevurderingsscenario = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medBehandlingType(BehandlingType.REVURDERING)
                .leggTilVilkår(oppVilkårType, VilkårUtfallType.IKKE_OPPFYLT)
                .medOriginalBehandling(førstegangsbehandling, BehandlingÅrsakType.RE_HENDELSE_FØDSEL)
                .medBehandlingsresultat(Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INGEN_ENDRING));
        var revurdering1 = førsteRevurderingsscenario.lagMocked();

        var andreRevurderingsscenario = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medBehandlingType(BehandlingType.REVURDERING)
                .leggTilVilkår(medlVilkårType, VilkårUtfallType.IKKE_OPPFYLT)
                .leggTilVilkår(oppVilkårType, VilkårUtfallType.OPPFYLT)
                .medOriginalBehandling(revurdering1, BehandlingÅrsakType.RE_HENDELSE_FØDSEL);
        var revurdering2 = andreRevurderingsscenario.lagMocked();

        var repositoryProvider = andreRevurderingsscenario.mockBehandlingRepositoryProvider();
        Behandlingsresultat.builderEndreEksisterende(revurdering2.getBehandlingsresultat())
                .medBehandlingResultatType(BehandlingResultatType.INNVILGET);
        var kontekst = new BehandlingskontrollKontekst(revurdering2.getSaksnummer(), revurdering2.getFagsakId(),
                repositoryProvider.getBehandlingRepository().taSkriveLås(revurdering2));

        var val = new RegelResultat(revurdering2.getBehandlingsresultat().getVilkårResultat(), emptyList(), emptyMap());
        when(regelOrkestrerer.vurderInngangsvilkår(eq(Set.of(oppVilkårType, medlVilkårType)), any(), any())).thenReturn(val);
        var inngangsvilkårFellesTjeneste = new InngangsvilkårFellesTjeneste(regelOrkestrerer);
        var behrepo = andreRevurderingsscenario.mockBehandlingRepository();
        when(behrepo.hentBehandling(førstegangsbehandling.getId())).thenReturn(førstegangsbehandling);
        when(behrepo.hentBehandling(revurdering1.getId())).thenReturn(revurdering1);
        // Act
        var stegResultat = new SutOpptjeningOgMedlVilkårSteg(repositoryProvider, inngangsvilkårFellesTjeneste).utførSteg(kontekst);

        // Assert
        assertThat(stegResultat.getTransisjon().stegTransisjon()).isEqualTo(StegTransisjon.HOPPOVER);
        assertThat(stegResultat.getTransisjon().målSteg()).isEqualTo(BehandlingStegType.FORESLÅ_BEHANDLINGSRESULTAT);
    }

    @Test
    void skal_ved_tilbakehopp_rydde_vilkårresultat_og_vilkår_og_behandlingsresultattype() {
        // Arrange
        var scenario = ScenarioMorSøkerEngangsstønad
                .forFødsel()
                .leggTilVilkår(medlVilkårType, VilkårUtfallType.OPPFYLT);
        var behandling = scenario.lagMocked();
        var repositoryProvider = scenario.mockBehandlingRepositoryProvider();
        Behandlingsresultat.builderEndreEksisterende(behandling.getBehandlingsresultat())
                .medBehandlingResultatType(BehandlingResultatType.INNVILGET);
        var kontekst = new BehandlingskontrollKontekst(behandling.getSaksnummer(), behandling.getFagsakId(),
                repositoryProvider.getBehandlingRepository().taSkriveLås(behandling));
        var inngangsvilkårFellesTjeneste = new InngangsvilkårFellesTjeneste(regelOrkestrerer);
        // Act
        new SutMedlemskapsvilkårSteg(repositoryProvider, inngangsvilkårFellesTjeneste)
                .vedTransisjon(kontekst, modell, BehandlingSteg.TransisjonType.HOPP_OVER_BAKOVER, null, null);

        // Assert
        var vilkårResultat = behandling.getBehandlingsresultat().getVilkårResultat();
        assertThat(vilkårResultat.getVilkårene().stream().map(Vilkår::getGjeldendeVilkårUtfall).collect(toList()))
                .containsExactly(VilkårUtfallType.IKKE_VURDERT);
        assertThat(behandling.getBehandlingsresultat().getBehandlingResultatType())
                .isEqualTo(BehandlingResultatType.IKKE_FASTSATT);
    }

    // ***** Testklasser *****
    class SutMedlemskapsvilkårSteg extends InngangsvilkårStegImpl {

        SutMedlemskapsvilkårSteg(BehandlingRepositoryProvider repositoryProvider, InngangsvilkårFellesTjeneste inngangsvilkårFellesTjeneste) {
            super(repositoryProvider, inngangsvilkårFellesTjeneste, BehandlingStegType.VURDER_MEDLEMSKAPVILKÅR);
        }

        @Override
        public List<VilkårType> vilkårHåndtertAvSteg() {
            return singletonList(medlVilkårType);
        }
    }

    class SutOpptjeningOgMedlVilkårSteg extends InngangsvilkårStegImpl {

        SutOpptjeningOgMedlVilkårSteg(BehandlingRepositoryProvider repositoryProvider, InngangsvilkårFellesTjeneste inngangsvilkårFellesTjeneste) {
            super(repositoryProvider, inngangsvilkårFellesTjeneste, BehandlingStegType.VURDER_OPPTJENINGSVILKÅR);
        }

        @Override
        public List<VilkårType> vilkårHåndtertAvSteg() {
            return of(oppVilkårType, medlVilkårType);
        }
    }

    class SutOpptjeningSteg extends VurderOpptjeningsvilkårSteg {

        SutOpptjeningSteg(BehandlingRepositoryProvider repositoryProvider,
                          InngangsvilkårFellesTjeneste inngangsvilkårFellesTjeneste) {
            super(repositoryProvider, inngangsvilkårFellesTjeneste);
        }

        @Override
        public List<VilkårType> vilkårHåndtertAvSteg() {
            return of(oppVilkårType);
        }
    }
}
