package no.nav.foreldrepenger.familiehendelse.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.Test;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning.PersonInformasjon;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning.Personopplysning;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.VurdereYtelseSammeBarnAnnenForelderAksjonspunktDto;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

public class VurdereYtelseSammeBarnAnnenForelderOppdatererTest {

    private VilkårResultat.Builder vilkårBuilder = VilkårResultat.builder();

    @Test
    public void skal_oppdatere_vilkår_for_adopsjon() {
        // Arrange
        ScenarioFarSøkerEngangsstønad scenario = scenario();
        Behandling behandling = scenario.lagMocked();

        VurdereYtelseSammeBarnAnnenForelderAksjonspunktDto dto = new VurdereYtelseSammeBarnAnnenForelderAksjonspunktDto();
        dto.setErVilkarOk(true);

        utførAksjonspunktOppdaterer(behandling, dto);

        // Assert
        Behandlingsresultat behandlingsresultat = behandling.getBehandlingsresultat();
        assertThat(behandlingsresultat.getVilkårResultat().getVilkårene()).hasSize(1);
        assertThat(behandlingsresultat.getVilkårResultat().getVilkårene().get(0).getGjeldendeVilkårUtfall()).isEqualTo(VilkårUtfallType.OPPFYLT);
    }

    @Test
    public void skal_sette_adopsjonsvilkåret_til_ikke_oppfylt_med_avslagskode() {
        ScenarioFarSøkerEngangsstønad scenario = scenario();
        Behandling behandling = scenario.lagMocked();

        VurdereYtelseSammeBarnAnnenForelderAksjonspunktDto dto = new VurdereYtelseSammeBarnAnnenForelderAksjonspunktDto();
        dto.setErVilkarOk(false);
        dto.setAvslagskode("1006");

        utførAksjonspunktOppdaterer(behandling, dto);

        // Assert
        VilkårResultat vilkårResultat = behandling.getBehandlingsresultat().getVilkårResultat();
        assertThat(vilkårResultat.getVilkårene()).hasSize(1);
        Vilkår vilkår = vilkårResultat.getVilkårene().get(0);
        assertThat(vilkår.getGjeldendeVilkårUtfall()).isEqualTo(VilkårUtfallType.IKKE_OPPFYLT);
        assertThat(vilkår.getAvslagsårsak()).isEqualTo(Avslagsårsak.MANN_ADOPTERER_IKKE_ALENE);
    }

    private HistorikkTjenesteAdapter lagMockHistory() {
        HistorikkInnslagTekstBuilder tekstBuilder = new HistorikkInnslagTekstBuilder();
        HistorikkTjenesteAdapter mockHistory = mock(HistorikkTjenesteAdapter.class);
        Mockito.when(mockHistory.tekstBuilder()).thenReturn(tekstBuilder);
        return mockHistory;
    }

    private ScenarioFarSøkerEngangsstønad scenario() {
        ScenarioFarSøkerEngangsstønad scenario = ScenarioFarSøkerEngangsstønad.forAdopsjon();

        PersonInformasjon forelder = scenario.opprettBuilderForRegisteropplysninger()
            .leggTilPersonopplysninger(
                Personopplysning.builder()
                    .aktørId(AktørId.dummy())
                    .navn("Forelder")
            )
            .build();

        scenario.medRegisterOpplysninger(forelder);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_OM_ANNEN_FORELDRE_HAR_MOTTATT_STØTTE, BehandlingStegType.KONTROLLER_FAKTA);
        scenario.leggTilVilkår(VilkårType.ADOPSJONSVILKÅRET_ENGANGSSTØNAD, VilkårUtfallType.IKKE_VURDERT);
        return scenario;
    }

    private void utførAksjonspunktOppdaterer(Behandling behandling, VurdereYtelseSammeBarnAnnenForelderAksjonspunktDto dto) {
        HistorikkTjenesteAdapter mockHistory = lagMockHistory();
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getKode());
        // Act
        new VurdereYtelseSammeBarnOppdaterer.VurdereYtelseSammeBarnAnnenForelderOppdaterer(mockHistory)
            .oppdater(dto, new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, null, vilkårBuilder, dto));
        vilkårBuilder.buildFor(behandling);
    }

}
