package no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.app;

import static no.nav.foreldrepenger.behandlingslager.behandling.InternalManipulerBehandling.forceOppdaterBehandlingSteg;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingModellTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.totrinn.Totrinnsvurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.vedtak.TotrinnTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.dto.TotrinnskontrollAksjonspunkterDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.dto.TotrinnskontrollSkjermlenkeContextDto;

@CdiDbAwareTest
class TotrinnskontrollAksjonspunkterTjenesteTest {

    private static final BehandlingStegType STEG_KONTROLLER_FAKTA = BehandlingStegType.KONTROLLER_FAKTA;
    private static final BehandlingStegType STEG_FATTE_VEDTAK = BehandlingStegType.FATTE_VEDTAK;


    private TotrinnskontrollAksjonspunkterTjeneste totrinnskontrollAksjonspunkterTjeneste;
    private final TotrinnTjeneste totrinnTjeneste = Mockito.mock(TotrinnTjeneste.class);
    private final TotrinnsaksjonspunktDtoTjeneste totrinnsaksjonspunktDtoTjeneste = Mockito.mock(TotrinnsaksjonspunktDtoTjeneste.class);
    private Behandling behandling;
    private Behandlingsresultat behandlingsresultat;

    @BeforeEach
    void oppsett() {
        totrinnskontrollAksjonspunkterTjeneste = new TotrinnskontrollAksjonspunkterTjeneste(totrinnsaksjonspunktDtoTjeneste, totrinnTjeneste,
            new BehandlingModellTjeneste());
    }

    @Test
    void skal_hente_tom_skjermlenkecontext_for_behandling_med_ikke_status_FATTER_VEDTAK_og_ingen_totrinnsvurdering_og_ingen_aksjonspunkter(){
        // Arrange
        opprettBehandlingForFP(Optional.empty());
        forceOppdaterBehandlingSteg(behandling, STEG_KONTROLLER_FAKTA);
        var totrinnskontrollAksjonspunkterDto = opprettTotrinnskontrollAksjonspunkterDto(Optional.empty(), Optional.empty());
        setFelleseMockMetoder(totrinnskontrollAksjonspunkterDto, Collections.emptyList());
        // Act
        var context = totrinnskontrollAksjonspunkterTjeneste.hentTotrinnsSkjermlenkeContext(behandling, behandlingsresultat);
        // Assert
        assertThat(context).isEmpty();
    }

    @Test
    void skal_hente_tom_skjermlenkecontext_for_behandling_med_status_FATTER_VEDTAK_og_ingen_totrinnsvurdering_og_ingen_aksjonspunkter(){
        // Arrange
        opprettBehandlingForFP(Optional.empty());
        forceOppdaterBehandlingSteg(behandling, STEG_FATTE_VEDTAK);
        var totrinnskontrollAksjonspunkterDto = opprettTotrinnskontrollAksjonspunkterDto(Optional.empty(), Optional.empty());
        setFelleseMockMetoder(totrinnskontrollAksjonspunkterDto, Collections.emptyList());
        // Act
        var context = totrinnskontrollAksjonspunkterTjeneste.hentTotrinnsSkjermlenkeContext(behandling, behandlingsresultat);
        // Assert
        assertThat(context).isEmpty();
    }

    @Test
    void skal_hente_tom_skjermlenkecontext_for_behandling_med_ikke_status_FATTER_VEDTAK_og_med_totrinnsvurdering_og_ingen_aksjonspunkter(){

        // Arrange
        var aksjonspunktDefinisjon = AksjonspunktDefinisjon.VURDER_MEDLEMSKAPSVILKÅRET;
        var ttvGodkjent = false;

        opprettBehandlingForFP(Optional.empty());
        forceOppdaterBehandlingSteg(behandling, STEG_KONTROLLER_FAKTA);

        var totrinnskontrollAksjonspunkterDto = opprettTotrinnskontrollAksjonspunkterDto(Optional.empty(), Optional.empty());
        var ttv = opprettTotrinnsvurdering(behandling, aksjonspunktDefinisjon, ttvGodkjent);

        setFelleseMockMetoder(totrinnskontrollAksjonspunkterDto, Collections.singletonList(ttv));

        // Act
        var context = totrinnskontrollAksjonspunkterTjeneste.hentTotrinnsSkjermlenkeContext(behandling, behandlingsresultat);
        // Assert
        assertThat(context).isEmpty();

    }

    @Test
    void skal_hente_tom_skjermlenkecontext_for_behandling_med_status_FATTER_VEDTAK_og_med_totrinnsvurdering_og_ingen_aksjonspunkter(){
        // Arrange
        var aksjonspunktDefinisjon = AksjonspunktDefinisjon.VURDER_MEDLEMSKAPSVILKÅRET;
        var ttvGodkjent = false;

        opprettBehandlingForFP(Optional.empty());
        forceOppdaterBehandlingSteg(behandling, STEG_FATTE_VEDTAK);

        var totrinnskontrollAksjonspunkterDto = opprettTotrinnskontrollAksjonspunkterDto(Optional.empty(), Optional.empty());
        var ttv = opprettTotrinnsvurdering(behandling, aksjonspunktDefinisjon, ttvGodkjent);

        setFelleseMockMetoder(totrinnskontrollAksjonspunkterDto, Collections.singletonList(ttv));

        // Act
        var context = totrinnskontrollAksjonspunkterTjeneste.hentTotrinnsSkjermlenkeContext(behandling, behandlingsresultat);
        // Assert
        assertThat(context).isEmpty();
    }

    @Test
    void skal_hente_en_skjermlenketype_og_ett_totrinnskontrollaksjonspunkt_for_behandling_med_en_totrinnsvurdering_og_ett_aksjonspunkt_som_ikke_omhandler_mottat_stotte_eller_omsorgsovertakelse(){

        // Arrange
        var aksjonspunktDefinisjon = AksjonspunktDefinisjon.VURDER_MEDLEMSKAPSVILKÅRET;
        var ttvGodkjent = false;
        var apAvbrutt = false;

        opprettBehandlingForFP(Optional.empty());

        var ttv = opprettTotrinnsvurdering(behandling, aksjonspunktDefinisjon, ttvGodkjent);
        var totrinnskontrollAksjonspunkterDto = opprettTotrinnskontrollAksjonspunkterDto(Optional.of(aksjonspunktDefinisjon), Optional.of(ttv));
        opprettAksjonspunkt(behandling, aksjonspunktDefinisjon, apAvbrutt);

        setFelleseMockMetoder(totrinnskontrollAksjonspunkterDto, Collections.singletonList(ttv));

        // Act
        var context = totrinnskontrollAksjonspunkterTjeneste.hentTotrinnsSkjermlenkeContext(behandling, behandlingsresultat);

        // Assert
        assertThat(context).hasSize(1);

        var totrinnskontrollSkjermlenkeContextDto = context.get(0);
        assertThat(totrinnskontrollSkjermlenkeContextDto.getSkjermlenkeType()).isEqualTo(SkjermlenkeType.FAKTA_OM_MEDLEMSKAP.getKode());

        var totrinnskontrollAksjonspunkter = totrinnskontrollSkjermlenkeContextDto.getTotrinnskontrollAksjonspunkter();
        assertThat(totrinnskontrollAksjonspunkter).hasSize(1);

        var enesteTotrinnskontrollAksjonspunkt = totrinnskontrollAksjonspunkter.get(0);
        assertThat(enesteTotrinnskontrollAksjonspunkt.getAksjonspunktKode()).isEqualTo(aksjonspunktDefinisjon.getKode());
        assertThat(enesteTotrinnskontrollAksjonspunkt.getTotrinnskontrollGodkjent()).isFalse();

    }

    @Test
    void skal_hente_en_skjermlenketype_og_ett_totrinnskontrollaksjonspunkt_for_behandling_med_en_totrinnsvurdering_og_ett_aksjonspunkt_som_omhandler_mottat_stotte(){

        // Arrange
        List<AksjonspunktDefinisjon> aksjonspunktDefinisjons = new ArrayList<>();
        aksjonspunktDefinisjons.add(AksjonspunktDefinisjon.AVKLAR_OM_SØKER_HAR_MOTTATT_STØTTE);
        var ttvGodkjent = false;
        var apAvbrutt = false;

        var vilkårTypeSkjermlenkeTypeMap = Map.of(
            VilkårType.FØDSELSVILKÅRET_MOR, SkjermlenkeType.PUNKT_FOR_FOEDSEL,
            VilkårType.FØDSELSVILKÅRET_FAR_MEDMOR, SkjermlenkeType.PUNKT_FOR_FOEDSEL,
            VilkårType.ADOPSJONSVILKÅRET_ENGANGSSTØNAD, SkjermlenkeType.PUNKT_FOR_ADOPSJON,
            VilkårType.ADOPSJONSVILKARET_FORELDREPENGER, SkjermlenkeType.PUNKT_FOR_ADOPSJON,
            VilkårType.OMSORGSVILKÅRET, SkjermlenkeType.PUNKT_FOR_OMSORG,
            VilkårType.FORELDREANSVARSVILKÅRET_2_LEDD, SkjermlenkeType.PUNKT_FOR_FORELDREANSVAR,
            VilkårType.FORELDREANSVARSVILKÅRET_4_LEDD, SkjermlenkeType.PUNKT_FOR_FORELDREANSVAR);

        for (var aksjonspunktDefinisjon : aksjonspunktDefinisjons) {
            vilkårTypeSkjermlenkeTypeMap.keySet().forEach(vilkårType -> {

                opprettBehandlingForFP(Optional.of(vilkårType));

                var ttv = opprettTotrinnsvurdering(behandling, aksjonspunktDefinisjon, ttvGodkjent);
                var totrinnskontrollAksjonspunkterDto = opprettTotrinnskontrollAksjonspunkterDto(Optional.of(aksjonspunktDefinisjon), Optional.of(ttv));
                opprettAksjonspunkt(behandling, aksjonspunktDefinisjon, apAvbrutt);

                setFelleseMockMetoder(totrinnskontrollAksjonspunkterDto, Collections.singletonList(ttv));

                // Act
                var context = totrinnskontrollAksjonspunkterTjeneste.hentTotrinnsSkjermlenkeContext(behandling, behandlingsresultat);

                // Arrange
                assertThat(context).hasSize(1);

                var totrinnskontrollSkjermlenkeContextDto = context.get(0);
                assertThat(totrinnskontrollSkjermlenkeContextDto.getSkjermlenkeType()).isEqualTo(vilkårTypeSkjermlenkeTypeMap.get(vilkårType).getKode());

                var totrinnskontrollAksjonspunkter = totrinnskontrollSkjermlenkeContextDto.getTotrinnskontrollAksjonspunkter();
                assertThat(totrinnskontrollAksjonspunkter).hasSize(1);

                var enesteTotrinnskontrollAksjonspunkt = totrinnskontrollAksjonspunkter.get(0);
                assertThat(enesteTotrinnskontrollAksjonspunkt.getAksjonspunktKode()).isEqualTo(aksjonspunktDefinisjon.getKode());
                assertThat(enesteTotrinnskontrollAksjonspunkt.getTotrinnskontrollGodkjent()).isFalse();

            });
        }

    }

    @Test
    void skal_hente_en_skjermlenketype_og_ett_totrinnskontrollaksjonspunkt_for_behandling_men_en_totrinnsvurdering_og_ett_aksjonspunkt_som_omhander_omsorgsovertakelse(){

        // Arrange
        var aksjonspunktDefinisjon = AksjonspunktDefinisjon.AVKLAR_VILKÅR_FOR_OMSORGSOVERTAKELSE;
        var ttvGodkjent = true;
        var apAvbrutt = false;

        Map<FagsakYtelseType, SkjermlenkeType> fagsakYtelseTypeSkjermlenkeTypeMap = new HashMap<>();
        fagsakYtelseTypeSkjermlenkeTypeMap.put(FagsakYtelseType.ENGANGSTØNAD, SkjermlenkeType.FAKTA_OM_OMSORG_OG_FORELDREANSVAR);
        fagsakYtelseTypeSkjermlenkeTypeMap.put(FagsakYtelseType.FORELDREPENGER, SkjermlenkeType.FAKTA_FOR_OMSORG);

        fagsakYtelseTypeSkjermlenkeTypeMap.keySet().forEach(fagsakYtelseType -> {

            if (FagsakYtelseType.ENGANGSTØNAD.equals(fagsakYtelseType)) {
                opprettBehandlingForEngangsstønad();
            } else {
                opprettBehandlingForFP(Optional.empty());
            }

            var ttv = opprettTotrinnsvurdering(behandling, aksjonspunktDefinisjon, ttvGodkjent);
            var totrinnskontrollAksjonspunkterDto = opprettTotrinnskontrollAksjonspunkterDto(Optional.of(aksjonspunktDefinisjon), Optional.of(ttv));
            opprettAksjonspunkt(behandling, aksjonspunktDefinisjon, apAvbrutt);

            setFelleseMockMetoder(totrinnskontrollAksjonspunkterDto, Collections.singletonList(ttv));

            // Act
            var context = totrinnskontrollAksjonspunkterTjeneste.hentTotrinnsSkjermlenkeContext(behandling, behandlingsresultat);

            // Arrange
            assertThat(context).hasSize(1);

            var totrinnskontrollSkjermlenkeContextDto = context.get(0);
            assertThat(totrinnskontrollSkjermlenkeContextDto.getSkjermlenkeType()).isEqualTo(fagsakYtelseTypeSkjermlenkeTypeMap.get(fagsakYtelseType).getKode());

            var totrinnskontrollAksjonspunkter = totrinnskontrollSkjermlenkeContextDto.getTotrinnskontrollAksjonspunkter();
            assertThat(totrinnskontrollAksjonspunkter).hasSize(1);

            var enesteTotrinnskontrollAksjonspunkt = totrinnskontrollAksjonspunkter.get(0);
            assertThat(enesteTotrinnskontrollAksjonspunkt.getAksjonspunktKode()).isEqualTo(aksjonspunktDefinisjon.getKode());
            assertThat(enesteTotrinnskontrollAksjonspunkt.getTotrinnskontrollGodkjent()).isTrue();

        });

    }

    @Test
    void skal_hente_tom_skjermlenkecontext_for_behandling_med_en_totrinnsvurdering_og_ett_aksjonspunkt_som_omhandler_mottate_stotte_men_hvor_skjermlenketypen_blir_underfinert(){

        // Arrange
        List<AksjonspunktDefinisjon> aksjonspunktDefinisjons = new ArrayList<>();
        aksjonspunktDefinisjons.add(AksjonspunktDefinisjon.AVKLAR_OM_SØKER_HAR_MOTTATT_STØTTE);
        var ttvGodkjent = false;
        var apAvbrutt = false;

        var vilkårTypeSkjermlenkeTypeMap = Map.of(
            VilkårType.SØKERSOPPLYSNINGSPLIKT, SkjermlenkeType.UDEFINERT,
            VilkårType.MEDLEMSKAPSVILKÅRET, SkjermlenkeType.UDEFINERT,
            VilkårType.SØKNADSFRISTVILKÅRET, SkjermlenkeType.UDEFINERT,
            VilkårType.OPPTJENINGSVILKÅRET, SkjermlenkeType.UDEFINERT,
            VilkårType.OPPTJENINGSPERIODEVILKÅR, SkjermlenkeType.UDEFINERT,
            VilkårType.BEREGNINGSGRUNNLAGVILKÅR, SkjermlenkeType.UDEFINERT);

        for (var aksjonspunktDefinisjon : aksjonspunktDefinisjons) {
            vilkårTypeSkjermlenkeTypeMap.keySet().forEach(vilkårType -> {

                opprettBehandlingForFP(Optional.of(vilkårType));

                var ttv = opprettTotrinnsvurdering(behandling, aksjonspunktDefinisjon, ttvGodkjent);
                var totrinnskontrollAksjonspunkterDto = opprettTotrinnskontrollAksjonspunkterDto(Optional.of(aksjonspunktDefinisjon), Optional.of(ttv));
                opprettAksjonspunkt(behandling, aksjonspunktDefinisjon, apAvbrutt);

                setFelleseMockMetoder(totrinnskontrollAksjonspunkterDto, Collections.singletonList(ttv));

                // Act
                var context = totrinnskontrollAksjonspunkterTjeneste.hentTotrinnsSkjermlenkeContext(behandling, behandlingsresultat);
                // Arrange
                assertThat(context).isEmpty();

            });
        }

    }

    @Test
    void skal_hente_en_skjermlenketype_og_ett_totrinnskontrollaksjonspunkt_for_behandling_med_status_FATTE_VEDTAK_og_ingen_totrinnsvurdering_og_ett_aksjonspunkt(){

        // Arrange
        var aksjonspunktDefinisjon = AksjonspunktDefinisjon.VURDER_MEDLEMSKAPSVILKÅRET;
        var apAvbrutt = false;

        opprettBehandlingForFP(Optional.empty());
        forceOppdaterBehandlingSteg(behandling, STEG_FATTE_VEDTAK);

        var totrinnskontrollAksjonspunkterDto = opprettTotrinnskontrollAksjonspunkterDto(Optional.of(aksjonspunktDefinisjon), Optional.empty());
        opprettAksjonspunkt(behandling, aksjonspunktDefinisjon, apAvbrutt);

        setFelleseMockMetoder(totrinnskontrollAksjonspunkterDto, Collections.emptyList());

        // Act
        var context = totrinnskontrollAksjonspunkterTjeneste.hentTotrinnsSkjermlenkeContext(behandling, behandlingsresultat);

        // Assert
        assertThat(context).hasSize(1);
        assertThat(context.get(0).getSkjermlenkeType()).isEqualTo(SkjermlenkeType.FAKTA_OM_MEDLEMSKAP.getKode());
        var totrinnskontrollAksjonspunkter = context.get(0).getTotrinnskontrollAksjonspunkter();
        assertThat(totrinnskontrollAksjonspunkter).hasSize(1);
        assertThat(totrinnskontrollAksjonspunkter.get(0).getAksjonspunktKode()).isEqualTo(AksjonspunktDefinisjon.VURDER_MEDLEMSKAPSVILKÅRET.getKode());

    }

    @Test
    void skal_hente_en_skjermlenketype_og_ett_ikke_godkjent_totrinnskontrollaksjonspunkt_for_behandling_med_en_godkjent_totrinnsvurdering_og_ett_aksjonspunkt_som_ikke_har_samme_aksjonspunktdefinisjon(){

        var adFraAksjonspunkt = AksjonspunktDefinisjon.VURDER_MEDLEMSKAPSVILKÅRET;
        var adFraTotrinnvurdering = AksjonspunktDefinisjon.AUTO_VENT_PÅ_FØDSELREGISTRERING;
        var ttvGodkjent = true;
        var apAvbrutt = false;

        opprettBehandlingForFP(Optional.empty());

        var ttvFraBehandling = opprettTotrinnsvurdering(behandling, adFraTotrinnvurdering, ttvGodkjent);
        var ttvOpprettetAvMetode = opprettTotrinnsvurdering(behandling, adFraAksjonspunkt, !ttvGodkjent);
        var totrinnskontrollAksjonspunkterDto = opprettTotrinnskontrollAksjonspunkterDto(Optional.of(adFraAksjonspunkt), Optional.of(ttvOpprettetAvMetode));
        opprettAksjonspunkt(behandling, adFraAksjonspunkt, apAvbrutt);

        setFelleseMockMetoder(totrinnskontrollAksjonspunkterDto, Collections.singletonList(ttvFraBehandling));

        // Act
        var context = totrinnskontrollAksjonspunkterTjeneste.hentTotrinnsSkjermlenkeContext(behandling, behandlingsresultat);

        // Assert
        assertThat(context).hasSize(1);
        assertThat(context.get(0).getSkjermlenkeType()).isEqualTo(SkjermlenkeType.FAKTA_OM_MEDLEMSKAP.getKode());

        var totrinnskontrollAksjonspunkter = context.get(0).getTotrinnskontrollAksjonspunkter();
        assertThat(totrinnskontrollAksjonspunkter).hasSize(1);

        var enesteTotrinnskontrollAksjonspunkt = totrinnskontrollAksjonspunkter.get(0);
        assertThat(enesteTotrinnskontrollAksjonspunkt.getAksjonspunktKode()).isEqualTo(adFraAksjonspunkt.getKode());
        assertThat(enesteTotrinnskontrollAksjonspunkt.getTotrinnskontrollGodkjent()).isFalse();

    }

    @Test
    void skal_hente_en_tom_skjermlenkecontext_for_behandling_med_en_totrinnsvurdering_og_ett_avbrutt_aksjonspunkt(){

        // Arrange
        var aksjonspunktDefinisjon = AksjonspunktDefinisjon.VURDER_MEDLEMSKAPSVILKÅRET;
        var ttvGodkjent = false;
        var apAvbrutt = true;

        opprettBehandlingForFP(Optional.empty());

        var ttv = opprettTotrinnsvurdering(behandling, aksjonspunktDefinisjon, ttvGodkjent);
        var totrinnskontrollAksjonspunkterDto = opprettTotrinnskontrollAksjonspunkterDto(Optional.of(aksjonspunktDefinisjon), Optional.of(ttv));
        opprettAksjonspunkt(behandling, aksjonspunktDefinisjon, apAvbrutt);

        setFelleseMockMetoder(totrinnskontrollAksjonspunkterDto, Collections.singletonList(ttv));

        // Act
        var context = totrinnskontrollAksjonspunkterTjeneste.hentTotrinnsSkjermlenkeContext(behandling, behandlingsresultat);

        // Assert
        assertThat(context).isEmpty();

    }

    @Test
    void skal_hente_en_tom_skjermlenkecontext_for_en_behandling_med_en_totrinnsvurdering_med_et_aksjonspunktdefinisjon_som_gir_en_undefinert_skjermlenketype(){

        // Arrange
        var aksjonspunktDefinisjon = AksjonspunktDefinisjon.AVKLAR_OM_SØKER_HAR_MOTTATT_STØTTE;
        var ttvGodkjent = false;

        opprettBehandlingForFP(Optional.empty());

        var ttv = opprettTotrinnsvurdering(behandling, aksjonspunktDefinisjon, ttvGodkjent);
        var totrinnskontrollAksjonspunkterDto = opprettTotrinnskontrollAksjonspunkterDto(Optional.of(aksjonspunktDefinisjon), Optional.of(ttv));

        setFelleseMockMetoder(totrinnskontrollAksjonspunkterDto, Collections.singletonList(ttv));

        // Act
        var context = totrinnskontrollAksjonspunkterTjeneste.hentTotrinnsvurderingSkjermlenkeContext(behandling, behandlingsresultat);

        // Assert
        assertThat(context).isEmpty();

    }

    @Test
    void skal_hente_en_tom_skjermlenkecontext_for_en_behandling_med_ingen_totrinnaksjonspunktvurdering(){
        // Arrange
        opprettBehandlingForFP(Optional.empty());
        when(totrinnTjeneste.hentTotrinnaksjonspunktvurderinger(behandling.getId())).thenReturn(Collections.emptyList());
        // Act
        var context = totrinnskontrollAksjonspunkterTjeneste.hentTotrinnsvurderingSkjermlenkeContext(behandling, behandlingsresultat);
        // Assert
        assertThat(context).isEmpty();
    }

    @Test
    void skal_hente_en_skjermlenketype_og_to_totrinnskontrollaksjonspunkt_for_behandling_med_to_totrinnsvurdering_med_aksjonspunktdefinisjoner_som_omhandler_mottat_stotte(){

        // Arrange
        var aksjonspunktDefinisjon1 = AksjonspunktDefinisjon.AVKLAR_OM_SØKER_HAR_MOTTATT_STØTTE;
        var ttv1Godkjent = false;
        var ttv2Godkjent = true;

        Map<VilkårType, SkjermlenkeType> vilkårTypeSkjermlenkeTypeMap = new HashMap<>();
        vilkårTypeSkjermlenkeTypeMap.put(VilkårType.FØDSELSVILKÅRET_MOR, SkjermlenkeType.PUNKT_FOR_FOEDSEL);
        vilkårTypeSkjermlenkeTypeMap.put(VilkårType.FØDSELSVILKÅRET_FAR_MEDMOR, SkjermlenkeType.PUNKT_FOR_FOEDSEL);
        vilkårTypeSkjermlenkeTypeMap.put(VilkårType.ADOPSJONSVILKÅRET_ENGANGSSTØNAD, SkjermlenkeType.PUNKT_FOR_ADOPSJON);
        vilkårTypeSkjermlenkeTypeMap.put(VilkårType.ADOPSJONSVILKARET_FORELDREPENGER, SkjermlenkeType.PUNKT_FOR_ADOPSJON);
        vilkårTypeSkjermlenkeTypeMap.put(VilkårType.OMSORGSVILKÅRET, SkjermlenkeType.PUNKT_FOR_OMSORG);
        vilkårTypeSkjermlenkeTypeMap.put(VilkårType.FORELDREANSVARSVILKÅRET_2_LEDD, SkjermlenkeType.PUNKT_FOR_FORELDREANSVAR);
        vilkårTypeSkjermlenkeTypeMap.put(VilkårType.FORELDREANSVARSVILKÅRET_4_LEDD, SkjermlenkeType.PUNKT_FOR_FORELDREANSVAR);

        vilkårTypeSkjermlenkeTypeMap.keySet().forEach(vilkårType -> {

            opprettBehandlingForFP(Optional.of(vilkårType));

            var ttv1 = opprettTotrinnsvurdering(behandling, aksjonspunktDefinisjon1, ttv1Godkjent);
            var totrinnskontrollAksjonspunkterDto1 = opprettTotrinnskontrollAksjonspunkterDto(Optional.of(aksjonspunktDefinisjon1), Optional.of(ttv1));

            when(totrinnTjeneste.hentTotrinnaksjonspunktvurderinger(behandling.getId())).thenReturn(List.of(ttv1));
            when(totrinnsaksjonspunktDtoTjeneste.lagTotrinnskontrollAksjonspunktDto(eq(ttv1), eq(behandling)))
                .thenReturn(totrinnskontrollAksjonspunkterDto1);

            // Act
            var context = totrinnskontrollAksjonspunkterTjeneste.hentTotrinnsvurderingSkjermlenkeContext(behandling, behandlingsresultat);

            // Arrange
            assertThat(context).hasSize(1);

            var totrinnskontrollSkjermlenkeContextDto = context.getFirst();
            assertThat(totrinnskontrollSkjermlenkeContextDto.getSkjermlenkeType()).isEqualTo(vilkårTypeSkjermlenkeTypeMap.get(vilkårType).getKode());

            var totrinnskontrollAksjonspunkter = totrinnskontrollSkjermlenkeContextDto.getTotrinnskontrollAksjonspunkter();
            assertThat(totrinnskontrollAksjonspunkter).hasSize(1);

            var førsteTotrinnskontrollAksjonspunkt = totrinnskontrollAksjonspunkter.getFirst();
            assertThat(førsteTotrinnskontrollAksjonspunkt.getAksjonspunktKode()).isEqualTo(aksjonspunktDefinisjon1.getKode());
            assertThat(førsteTotrinnskontrollAksjonspunkt.getTotrinnskontrollGodkjent()).isFalse();

        });

    }

    @Test
    void skal_sortere_basert_på_behandlingsrekkefølge() {
        var aksjonspunktDefs = List.of(AksjonspunktDefinisjon.FASTSETT_UTTAKPERIODER, AksjonspunktDefinisjon.KONTROLLER_ANNENPART_EØS, //Samme skjermlenke, samme steg, samme vurderingspunkt
            AksjonspunktDefinisjon.VURDER_MEDLEMSKAPSVILKÅRET, AksjonspunktDefinisjon.OVERSTYRING_AV_AVKLART_STARTDATO, //Ulik skjermlenke, samme steg, ulik vurderingspunkt
            AksjonspunktDefinisjon.OVERSTYRING_AV_RETT_OG_OMSORG, AksjonspunktDefinisjon.VURDER_UTTAK_DOKUMENTASJON);

        var dtos = aksjonspunktDefs.stream()
            .map(ad -> new TotrinnskontrollSkjermlenkeContextDto(ad.getSkjermlenkeType().getKode(),
                List.of(opprettTotrinnskontrollAksjonspunkterDto(Optional.of(ad), Optional.empty()))))
            .toList();

        var sortert = TotrinnskontrollAksjonspunkterTjeneste.sorterPåBehandlingsrekkefølge(new BehandlingModellTjeneste(), FagsakYtelseType.FORELDREPENGER,
            BehandlingType.FØRSTEGANGSSØKNAD, dtos);

        assertThat(sortert).hasSize(6);
        assertThat(sortert.getFirst().getSkjermlenkeType()).isEqualTo(AksjonspunktDefinisjon.VURDER_MEDLEMSKAPSVILKÅRET.getSkjermlenkeType().getKode());
        assertThat(sortert.getFirst().getTotrinnskontrollAksjonspunkter()).hasSize(1);

        assertThat(sortert.get(1).getSkjermlenkeType()).isEqualTo(AksjonspunktDefinisjon.OVERSTYRING_AV_AVKLART_STARTDATO.getSkjermlenkeType().getKode());
        assertThat(sortert.get(1).getTotrinnskontrollAksjonspunkter()).hasSize(1);

        assertThat(sortert.get(2).getSkjermlenkeType()).isEqualTo(AksjonspunktDefinisjon.OVERSTYRING_AV_RETT_OG_OMSORG.getSkjermlenkeType().getKode());
        assertThat(sortert.get(2).getTotrinnskontrollAksjonspunkter()).hasSize(1);

        assertThat(sortert.get(3).getSkjermlenkeType()).isEqualTo(AksjonspunktDefinisjon.VURDER_UTTAK_DOKUMENTASJON.getSkjermlenkeType().getKode());
        assertThat(sortert.get(3).getTotrinnskontrollAksjonspunkter()).hasSize(1);

        assertThat(sortert.get(4).getSkjermlenkeType()).isEqualTo(AksjonspunktDefinisjon.FASTSETT_UTTAKPERIODER.getSkjermlenkeType().getKode());
        assertThat(sortert.get(4).getTotrinnskontrollAksjonspunkter()).hasSize(1);

        assertThat(sortert.get(5).getSkjermlenkeType()).isEqualTo(AksjonspunktDefinisjon.FASTSETT_UTTAKPERIODER.getSkjermlenkeType().getKode());
        assertThat(sortert.get(5).getTotrinnskontrollAksjonspunkter()).hasSize(1);

    }

    private void opprettBehandlingForFP(Optional<VilkårType> vilkårTypeOpt) {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        vilkårTypeOpt.ifPresent(vt -> scenario.leggTilVilkår(vt, VilkårUtfallType.IKKE_VURDERT));
        behandling = scenario.lagMocked();
        behandlingsresultat = scenario.mockBehandlingRepositoryProvider().getBehandlingsresultatRepository().hentHvisEksisterer(behandling.getId()).orElse(null);
    }

    private void opprettBehandlingForEngangsstønad() {
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        behandling = scenario.lagMocked();
    }

    private void setFelleseMockMetoder(TotrinnskontrollAksjonspunkterDto totrinnskontrollAksjonspunkterDto, List<Totrinnsvurdering> ttv) {
        when(totrinnTjeneste.hentTotrinnaksjonspunktvurderinger(behandling.getId())).thenReturn(ttv);
        when(totrinnsaksjonspunktDtoTjeneste.lagTotrinnskontrollAksjonspunktDto(any(), eq(behandling)))
            .thenReturn(totrinnskontrollAksjonspunkterDto);
    }

    private TotrinnskontrollAksjonspunkterDto opprettTotrinnskontrollAksjonspunkterDto(Optional<AksjonspunktDefinisjon> aksjonspunktDefinisjonOpt, Optional<Totrinnsvurdering> ttvOpt) {
        var builder = new TotrinnskontrollAksjonspunkterDto.Builder();
        aksjonspunktDefinisjonOpt.ifPresent(ad -> builder.medAksjonspunktKode(ad.getKode()));
        ttvOpt.ifPresent(ttv -> builder.medTotrinnskontrollGodkjent(ttv.isGodkjent()));
        return  builder.build();
    }

    private Totrinnsvurdering opprettTotrinnsvurdering(Behandling behandling, AksjonspunktDefinisjon aksjonspunktDefinisjon, boolean godkjent) {
        return new Totrinnsvurdering.Builder(behandling, aksjonspunktDefinisjon)
            .medGodkjent(godkjent)
            .build();
    }

    private void opprettAksjonspunkt(Behandling behandling, AksjonspunktDefinisjon aksjonspunktDefinisjon, boolean erAvbrutt) {
        var aksjonspunkt = AksjonspunktTestSupport.leggTilAksjonspunkt(behandling, aksjonspunktDefinisjon);
        AksjonspunktTestSupport.setToTrinnsBehandlingKreves(aksjonspunkt);
        if (erAvbrutt) {
            AksjonspunktTestSupport.setTilAvbrutt(aksjonspunkt);
        }
    }

}
