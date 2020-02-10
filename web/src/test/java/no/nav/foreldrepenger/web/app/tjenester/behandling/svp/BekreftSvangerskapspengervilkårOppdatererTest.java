package no.nav.foreldrepenger.web.app.tjenester.behandling.svp;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.Optional;

import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.dokumentarkiv.DokumentArkivTjeneste;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.historikk.dto.HistorikkInnslagKonverter;

public class BekreftSvangerskapspengervilkårOppdatererTest {

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());

    @Test
    public void skal_sette_totrinn_ved_avslag() {
        BekreftSvangerskapspengervilkårOppdaterer oppdaterer = oppdaterer();
        BekreftSvangerskapspengervilkårDto dto = new BekreftSvangerskapspengervilkårDto("begrunnelse",
            Avslagsårsak.SØKER_IKKE_GRAVID_KVINNE.getKode());

        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_SVANGERSKAPSPENGERVILKÅRET, BehandlingStegType.VURDER_SVANGERSKAPSPENGERVILKÅR);
        Behandling behandling = scenario.lagre(repositoryProvider);
        AksjonspunktOppdaterParameter param = new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto);
        OppdateringResultat resultat = oppdaterer.oppdater(dto, param);

        assertThat(resultat.kreverTotrinnsKontroll()).isTrue();
    }

    //TODO ikke gå til totrinn når innvilgesbrev fungerer
    @Test
    public void skal_ha_totrinn_ved_innvilgelse() {
        BekreftSvangerskapspengervilkårOppdaterer oppdaterer = oppdaterer();
        BekreftSvangerskapspengervilkårDto dto = new BekreftSvangerskapspengervilkårDto("begrunnelse", null);

        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_SVANGERSKAPSPENGERVILKÅRET, BehandlingStegType.VURDER_SVANGERSKAPSPENGERVILKÅR);
        Behandling behandling = scenario.lagre(repositoryProvider);
        AksjonspunktOppdaterParameter param = new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto);
        OppdateringResultat resultat = oppdaterer.oppdater(dto, param);

        assertThat(resultat.kreverTotrinnsKontroll()).isTrue();
    }

    @Test
    public void skal_avslå_vilkår() {
        BekreftSvangerskapspengervilkårOppdaterer oppdaterer = oppdaterer();
        BekreftSvangerskapspengervilkårDto dto = new BekreftSvangerskapspengervilkårDto("begrunnelse",
            Avslagsårsak.SØKER_ER_IKKE_I_ARBEID.getKode());

        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_SVANGERSKAPSPENGERVILKÅRET, BehandlingStegType.VURDER_SVANGERSKAPSPENGERVILKÅR);
        Behandling behandling = scenario.lagre(repositoryProvider);
        AksjonspunktOppdaterParameter param = new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto);
        oppdaterer.oppdater(dto, param);

        assertThat(param.getVilkårResultatBuilder().buildFor(behandling).getVilkårene().get(0).getGjeldendeVilkårUtfall()).isEqualTo(VilkårUtfallType.IKKE_OPPFYLT);
    }

    @Test
    public void skal_innvilge_vilkår() {
        BekreftSvangerskapspengervilkårOppdaterer oppdaterer = oppdaterer();
        BekreftSvangerskapspengervilkårDto dto = new BekreftSvangerskapspengervilkårDto("begrunnelse", null);

        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_SVANGERSKAPSPENGERVILKÅRET, BehandlingStegType.VURDER_SVANGERSKAPSPENGERVILKÅR);
        Behandling behandling = scenario.lagre(repositoryProvider);
        AksjonspunktOppdaterParameter param = new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto);
        oppdaterer.oppdater(dto, param);

        assertThat(param.getVilkårResultatBuilder().buildFor(behandling).getVilkårene().get(0).getGjeldendeVilkårUtfall()).isEqualTo(VilkårUtfallType.OPPFYLT);
    }

    private BekreftSvangerskapspengervilkårOppdaterer oppdaterer() {
        return new BekreftSvangerskapspengervilkårOppdaterer(new HistorikkTjenesteAdapter(repositoryProvider.getHistorikkRepository(),
            new HistorikkInnslagKonverter(),
            mock(DokumentArkivTjeneste.class)));
    }

}
