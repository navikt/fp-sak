package no.nav.foreldrepenger.familiehendelse.omsorg;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltVerdiType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.FarSøkerType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerEngangsstønad;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.OmsorgsvilkårAksjonspunktOppdaterer;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.Foreldreansvarsvilkår1AksjonspunktDto;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.Foreldreansvarsvilkår2AksjonspunktDto;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.OmsorgsvilkårAksjonspunktDto;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

@ExtendWith(MockitoExtension.class)
class OmsorgsvilkårOppdatererTest {

    private HistorikkInnslagTekstBuilder tekstBuilder;
    @Mock
    private HistorikkTjenesteAdapter mockHistory;

    @BeforeEach
    public void setup() {
        tekstBuilder = new HistorikkInnslagTekstBuilder();
        when(mockHistory.tekstBuilder()).thenReturn(tekstBuilder);
    }

    @Test
    void skal_generere_historikkinnslag_ved_avklaring_av_omsorgsvilkår() {
        var scenario = ScenarioFarSøkerEngangsstønad.forAdopsjon();
        scenario.medSøknad().medFarSøkerType(FarSøkerType.OVERTATT_OMSORG);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_OMSORGSVILKÅRET,
            BehandlingStegType.SØKERS_RELASJON_TIL_BARN);
        scenario.lagMocked();

        var behandling = scenario.getBehandling();
        // Act
        var dto = new OmsorgsvilkårAksjonspunktDto("begrunnelse", true, "-");
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getAksjonspunktDefinisjon());
        var omsorgsvilkarOppdaterer = new OmsorgsvilkårAksjonspunktOppdaterer.OmsorgsvilkårOppdaterer(mockHistory, scenario.mockBehandlingRepository());
        var resultat = omsorgsvilkarOppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto,
            aksjonspunkt));
        var historikkinnslag = new Historikkinnslag();
        historikkinnslag.setType(HistorikkinnslagType.FAKTA_ENDRET);
        var historikkInnslag = tekstBuilder.build(historikkinnslag);

        // Assert
        assertThat(aksjonspunkt.isToTrinnsBehandling()).isTrue();
        assertThat(resultat.getVilkårUtfallSomSkalLeggesTil()).hasSize(1);
        assertThat(resultat.getVilkårUtfallSomSkalLeggesTil().get(0).getVilkårType()).isEqualTo(VilkårType.OMSORGSVILKÅRET);
        assertThat(resultat.getVilkårUtfallSomSkalLeggesTil().get(0).getVilkårUtfallType()).isEqualTo(VilkårUtfallType.OPPFYLT);

        assertThat(historikkInnslag).hasSize(1);

        var del = historikkInnslag.get(0);
        var feltList = del.getEndredeFelt();
        assertThat(feltList).hasSize(1);
        assertThat(feltList.get(0)).satisfies(felt -> {
            assertThat(felt.getNavn()).isEqualTo(HistorikkEndretFeltType.OMSORGSVILKAR.getKode());
            assertThat(felt.getFraVerdi()).isNull();
            assertThat(felt.getTilVerdi()).isEqualTo(HistorikkEndretFeltVerdiType.OPPFYLT.getKode());
        });
    }

    @Test
    void skal_generere_historikkinnslag_ved_avklaring_av_foreldreansvar_andre_ledd_vilkår() {
        var scenario = ScenarioFarSøkerEngangsstønad.forAdopsjon();
        scenario.medSøknad().medFarSøkerType(FarSøkerType.OVERTATT_OMSORG);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_FORELDREANSVARSVILKÅRET_2_LEDD,
            BehandlingStegType.SØKERS_RELASJON_TIL_BARN);
        scenario.lagMocked();

        var behandling = scenario.getBehandling();
        // Act
        var dto = new Foreldreansvarsvilkår1AksjonspunktDto("begrunnelse", true, "-");
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getAksjonspunktDefinisjon());
        var oppdaterer = new OmsorgsvilkårAksjonspunktOppdaterer.Foreldreansvarsvilkår1Oppdaterer(mockHistory, scenario.mockBehandlingRepository());
        var resultat = oppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto, aksjonspunkt));
        var historikkinnslag = new Historikkinnslag();
        historikkinnslag.setType(HistorikkinnslagType.FAKTA_ENDRET);
        var historikkInnslag = tekstBuilder.build(historikkinnslag);

        // Assert
        assertThat(resultat.getVilkårUtfallSomSkalLeggesTil()).hasSize(1);
        assertThat(resultat.getVilkårUtfallSomSkalLeggesTil().get(0).getVilkårType()).isEqualTo(VilkårType.FORELDREANSVARSVILKÅRET_2_LEDD);
        assertThat(resultat.getVilkårUtfallSomSkalLeggesTil().get(0).getVilkårUtfallType()).isEqualTo(VilkårUtfallType.OPPFYLT);
        assertThat(historikkInnslag).hasSize(1);

        var del = historikkInnslag.get(0);
        var feltList = del.getEndredeFelt();
        assertThat(feltList).hasSize(1);
        assertThat(feltList.get(0)).satisfies(felt -> {
            assertThat(felt.getNavn()).isEqualTo(HistorikkEndretFeltType.FORELDREANSVARSVILKARET.getKode());
            assertThat(felt.getFraVerdi()).isNull();
            assertThat(felt.getTilVerdi()).isEqualTo(HistorikkEndretFeltVerdiType.OPPFYLT.getKode());
        });
    }

    @Test
    void skal_generere_historikkinnslag_ved_avklaring_av_foreldreansvar_fjerde_ledd_vilkår() {
        var scenario = ScenarioFarSøkerEngangsstønad.forAdopsjon();
        scenario.medSøknad().medFarSøkerType(FarSøkerType.OVERTATT_OMSORG);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_FORELDREANSVARSVILKÅRET_4_LEDD,
            BehandlingStegType.SØKERS_RELASJON_TIL_BARN);
        scenario.lagMocked();

        var behandling = scenario.getBehandling();
        // Act
        var dto = new Foreldreansvarsvilkår2AksjonspunktDto("begrunnelse", false, Avslagsårsak.IKKE_FORELDREANSVAR_ALENE_ETTER_BARNELOVA.getKode());
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getAksjonspunktDefinisjon());
        var oppdaterer = new OmsorgsvilkårAksjonspunktOppdaterer.Foreldreansvarsvilkår2Oppdaterer(mockHistory, scenario.mockBehandlingRepository());
        var resultat = oppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto, aksjonspunkt));
        var historikkinnslag = new Historikkinnslag();
        historikkinnslag.setType(HistorikkinnslagType.FAKTA_ENDRET);
        var historikkInnslag = tekstBuilder.build(historikkinnslag);

        // Assert
        assertThat(resultat.kreverTotrinnsKontroll()).isFalse();
        assertThat(resultat.getVilkårResultatType()).isEqualTo(VilkårResultatType.AVSLÅTT);
        assertThat(resultat.getTransisjon()).isEqualTo(FellesTransisjoner.FREMHOPP_VED_AVSLAG_VILKÅR);
        assertThat(resultat.getVilkårUtfallSomSkalLeggesTil()).hasSize(1);
        assertThat(resultat.getVilkårUtfallSomSkalLeggesTil().get(0).getVilkårType()).isEqualTo(VilkårType.FORELDREANSVARSVILKÅRET_4_LEDD);
        assertThat(resultat.getVilkårUtfallSomSkalLeggesTil().get(0).getVilkårUtfallType()).isEqualTo(VilkårUtfallType.IKKE_OPPFYLT);
        assertThat(resultat.getVilkårUtfallSomSkalLeggesTil().get(0).getAvslagsårsak()).isEqualTo(Avslagsårsak.IKKE_FORELDREANSVAR_ALENE_ETTER_BARNELOVA);
        assertThat(historikkInnslag).hasSize(1);

        var del = historikkInnslag.get(0);
        var feltList = del.getEndredeFelt();
        assertThat(feltList).hasSize(1);
        assertThat(feltList.get(0)).satisfies(felt -> {
            assertThat(felt.getNavn()).isEqualTo(HistorikkEndretFeltType.FORELDREANSVARSVILKARET.getKode());
            assertThat(felt.getFraVerdi()).isNull();
            assertThat(felt.getTilVerdi()).isEqualTo(HistorikkEndretFeltVerdiType.IKKE_OPPFYLT.getKode());
        });
    }

}
