package no.nav.foreldrepenger.web.app.tjenester.behandling.svp;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.JpaExtension;

@ExtendWith(JpaExtension.class)
class BekreftSvangerskapspengervilkårOppdatererTest {

    private BehandlingRepositoryProvider repositoryProvider;

    @BeforeEach
    void setUp(EntityManager entityManager) {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
    }

    @Test
    void skal_sette_totrinn_ved_avslag() {
        var oppdaterer = oppdaterer();
        var dto = new BekreftSvangerskapspengervilkårDto("begrunnelse",
            Avslagsårsak.SØKER_IKKE_GRAVID_KVINNE.getKode());

        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_SVANGERSKAPSPENGERVILKÅRET,
            BehandlingStegType.VURDER_SVANGERSKAPSPENGERVILKÅR);
        var behandling = scenario.lagre(repositoryProvider);
        var param = new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto);
        var resultat = oppdaterer.oppdater(dto, param);

        assertThat(resultat.kreverTotrinnsKontroll()).isTrue();
    }

    @Test
    void skal_avslå_vilkår() {
        var oppdaterer = oppdaterer();
        var dto = new BekreftSvangerskapspengervilkårDto("begrunnelse",
            Avslagsårsak.SØKER_ER_IKKE_I_ARBEID.getKode());

        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_SVANGERSKAPSPENGERVILKÅRET,
            BehandlingStegType.VURDER_SVANGERSKAPSPENGERVILKÅR);
        var behandling = scenario.lagre(repositoryProvider);
        var param = new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto);
        var resultat = oppdaterer.oppdater(dto, param);

        var builder = VilkårResultat.builder();
        resultat.getVilkårUtfallSomSkalLeggesTil()
            .forEach(v -> builder.manueltVilkår(v.getVilkårType(), v.getVilkårUtfallType(), v.getAvslagsårsak()));

        assertThat(builder.buildFor(behandling).getVilkårene().get(0).getGjeldendeVilkårUtfall()).isEqualTo(
            VilkårUtfallType.IKKE_OPPFYLT);
    }

    @Test
    void skal_innvilge_vilkår() {
        var oppdaterer = oppdaterer();
        var dto = new BekreftSvangerskapspengervilkårDto("begrunnelse", null);

        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_SVANGERSKAPSPENGERVILKÅRET,
            BehandlingStegType.VURDER_SVANGERSKAPSPENGERVILKÅR);
        var behandling = scenario.lagre(repositoryProvider);
        var param = new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto);
        var resultat = oppdaterer.oppdater(dto, param);

        var builder = VilkårResultat.builder();
        resultat.getVilkårUtfallSomSkalLeggesTil()
            .forEach(v -> builder.manueltVilkår(v.getVilkårType(), v.getVilkårUtfallType(), v.getAvslagsårsak()));


        assertThat(builder.buildFor(behandling).getVilkårene().get(0).getGjeldendeVilkårUtfall()).isEqualTo(
            VilkårUtfallType.OPPFYLT);
    }

    private BekreftSvangerskapspengervilkårOppdaterer oppdaterer() {
        return new BekreftSvangerskapspengervilkårOppdaterer(repositoryProvider.getHistorikkinnslagRepository());
    }

}
