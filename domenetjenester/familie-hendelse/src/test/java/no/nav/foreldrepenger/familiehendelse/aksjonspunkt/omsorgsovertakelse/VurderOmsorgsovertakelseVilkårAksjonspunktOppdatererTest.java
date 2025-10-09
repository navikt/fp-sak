package no.nav.foreldrepenger.familiehendelse.aksjonspunkt.omsorgsovertakelse;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.VURDER_OMSORGSOVERTAKELSEVILKÅRET;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;

import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdateringTransisjon;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OverhoppKontroll;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.OmsorgsovertakelseVilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinje;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.omsorgsovertakelse.dto.VurderOmsorgsovertakelseVilkårAksjonspunktDto;
import no.nav.foreldrepenger.skjæringstidspunkt.OpplysningsPeriodeTjeneste;

@ExtendWith(MockitoExtension.class)
class VurderOmsorgsovertakelseVilkårAksjonspunktOppdatererTest extends EntityManagerAwareTest {

    private static final LocalDate FØDSELSDATO = LocalDate.now().minusYears(1);
    private static final LocalDate OMSORGSOVERTAKELSESDATO = LocalDate.now();

    private BehandlingRepositoryProvider repositoryProvider;
    private FamilieHendelseTjeneste familieHendelseTjeneste;
    @Mock
    private OpplysningsPeriodeTjeneste opplysningsPeriodeTjeneste;

    @BeforeEach
    void setUp() {
        lenient().when(opplysningsPeriodeTjeneste.utledFikspunktForRegisterInnhenting(anyLong(), any())).thenReturn(LocalDate.now());
        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        familieHendelseTjeneste = new FamilieHendelseTjeneste(null, repositoryProvider.getFamilieHendelseRepository());
    }

    @Test
    void skal_bekrefte_som_oppfylt_uten_endring() {
        // Arrange

        var scenario = ScenarioFarSøkerForeldrepenger.forAdopsjon();
        scenario.medSøknadHendelse()
            .medAntallBarn(1)
            .leggTilBarn(FØDSELSDATO)
            .medAdopsjon(scenario.medSøknadHendelse().getAdopsjonBuilder()
                .medOmsorgsovertakelseDato(OMSORGSOVERTAKELSESDATO)
                .medOmsorgovertalseVilkårType(OmsorgsovertakelseVilkårType.FP_STEBARNSADOPSJONSVILKÅRET)
                .medErEktefellesBarn(true));

        scenario.leggTilAksjonspunkt(VURDER_OMSORGSOVERTAKELSEVILKÅRET, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);

        var behandling = scenario.lagre(repositoryProvider);


        var dto = new VurderOmsorgsovertakelseVilkårAksjonspunktDto("begrunnelse", OMSORGSOVERTAKELSESDATO, Map.of(1, FØDSELSDATO),
            null, OmsorgsovertakelseVilkårType.FP_STEBARNSADOPSJONSVILKÅRET, true);

        var vilkårBuilder = VilkårResultat.builder();
        var oppdateringResultat = oppdater(behandling, dto, vilkårBuilder);
        vilkårBuilder.buildFor(behandling);

        // Assert
        assertThat(oppdateringResultat.getOverhoppKontroll()).isEqualTo(OverhoppKontroll.UTEN_OVERHOPP);
        assertThat(oppdateringResultat.getVilkårUtfallSomSkalLeggesTil()).hasSize(1);
        assertThat(oppdateringResultat.getVilkårUtfallSomSkalLeggesTil().getFirst().getVilkårType()).isEqualTo(VilkårType.OMSORGSOVERTAKELSEVILKÅR);
        assertThat(oppdateringResultat.getVilkårUtfallSomSkalLeggesTil().getFirst().getVilkårUtfallType()).isEqualTo(VilkårUtfallType.OPPFYLT);
        assertThat(oppdateringResultat.getVilkårUtfallSomSkalLeggesTil().getFirst().avslagsårsak()).isEqualTo(Avslagsårsak.UDEFINERT);

        var famHendelseOverstyrtOpt = familieHendelseTjeneste.hentAggregat(behandling.getId()).getOverstyrtVersjon();

        assertThat(famHendelseOverstyrtOpt).isPresent().hasValueSatisfying(overstyrt -> {
            assertThat(familieHendelseTjeneste.hentAggregat(behandling.getId()).getGjeldendeVersjon()).isEqualTo(overstyrt);
            assertThat(overstyrt.getAntallBarn()).isEqualTo(1);
            assertThat(overstyrt.getBarna().getFirst().getFødselsdato()).isEqualTo(FØDSELSDATO);
            assertThat(overstyrt.getSkjæringstidspunkt()).isEqualTo(OMSORGSOVERTAKELSESDATO);
            assertThat(overstyrt.getAdopsjon()).isPresent().hasValueSatisfying(adopsjon -> {
                assertThat(adopsjon.getOmsorgsovertakelseDato()).isEqualTo(OMSORGSOVERTAKELSESDATO);
                assertThat(adopsjon.getErEktefellesBarn()).isTrue();
                assertThat(adopsjon.getOmsorgovertakelseVilkår()).isEqualTo(OmsorgsovertakelseVilkårType.FP_STEBARNSADOPSJONSVILKÅRET);
            });
        });

        var historikk = repositoryProvider.getHistorikkinnslagRepository().hent(behandling.getSaksnummer());
        assertThat(historikk).hasSize(1);
        assertThat(historikk.getFirst().getSkjermlenke()).isEqualTo(SkjermlenkeType.FAKTA_OM_OMSORGSOVERTAKELSE);
        assertThat(historikk.getFirst().getLinjer()).hasSize(2);
        assertThat(historikk.getFirst().getLinjer().stream().map(HistorikkinnslagLinje::getTekst).anyMatch("__Adopsjons- og omsorgsvilkåret__ er satt til __Oppfylt__."::equals)).isTrue();
        assertThat(historikk.getFirst().getLinjer().stream().map(HistorikkinnslagLinje::getTekst).anyMatch("begrunnelse."::equals)).isTrue();
    }

    @Test
    void skal_bekrefte_som_avslått_uten_endring() {
        // Arrange
        var fødselsdato = LocalDate.now().minusYears(1);
        var omsorgsovertakelsesdato = LocalDate.now();

        var scenario = ScenarioFarSøkerEngangsstønad.forAdopsjon();
        scenario.medSøknadHendelse()
            .medAntallBarn(1)
            .leggTilBarn(fødselsdato)
            .medAdopsjon(scenario.medSøknadHendelse().getAdopsjonBuilder()
                .medOmsorgsovertakelseDato(omsorgsovertakelsesdato)
                .medOmsorgovertalseVilkårType(OmsorgsovertakelseVilkårType.ES_FORELDREANSVARSVILKÅRET_2_LEDD)
                .medErEktefellesBarn(false));

        scenario.leggTilAksjonspunkt(VURDER_OMSORGSOVERTAKELSEVILKÅRET, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);

        var behandling = scenario.lagre(repositoryProvider);


        var dto = new VurderOmsorgsovertakelseVilkårAksjonspunktDto("begrunnelse", omsorgsovertakelsesdato, Map.of(1, fødselsdato),
            Avslagsårsak.SØKER_HAR_HATT_VANLIG_SAMVÆR_MED_BARNET, OmsorgsovertakelseVilkårType.ES_FORELDREANSVARSVILKÅRET_2_LEDD, false);

        var vilkårBuilder = VilkårResultat.builder();
        var oppdateringResultat = oppdater(behandling, dto, vilkårBuilder);
        vilkårBuilder.buildFor(behandling);

        // Assert
        assertThat(oppdateringResultat.getOverhoppKontroll()).isEqualTo(OverhoppKontroll.FREMOVERHOPP);
        assertThat(oppdateringResultat.getTransisjon()).isEqualTo(AksjonspunktOppdateringTransisjon.AVSLAG_VILKÅR);
        assertThat(oppdateringResultat.getVilkårUtfallSomSkalLeggesTil()).hasSize(1);
        assertThat(oppdateringResultat.getVilkårUtfallSomSkalLeggesTil().getFirst().getVilkårType()).isEqualTo(VilkårType.OMSORGSOVERTAKELSEVILKÅR);
        assertThat(oppdateringResultat.getVilkårUtfallSomSkalLeggesTil().getFirst().getVilkårUtfallType()).isEqualTo(VilkårUtfallType.IKKE_OPPFYLT);
        assertThat(oppdateringResultat.getVilkårUtfallSomSkalLeggesTil().getFirst().avslagsårsak()).isEqualTo(Avslagsårsak.SØKER_HAR_HATT_VANLIG_SAMVÆR_MED_BARNET);

        var famHendelseOverstyrtOpt = familieHendelseTjeneste.hentAggregat(behandling.getId()).getOverstyrtVersjon();
        assertThat(famHendelseOverstyrtOpt).isPresent().hasValueSatisfying(overstyrt -> {
            assertThat(familieHendelseTjeneste.hentAggregat(behandling.getId()).getGjeldendeVersjon()).isEqualTo(overstyrt);
            assertThat(overstyrt.getAntallBarn()).isEqualTo(1);
            assertThat(overstyrt.getSkjæringstidspunkt()).isEqualTo(omsorgsovertakelsesdato);
            assertThat(overstyrt.getAdopsjon()).isPresent().hasValueSatisfying(adopsjon -> {
                assertThat(adopsjon.getOmsorgsovertakelseDato()).isEqualTo(omsorgsovertakelsesdato);
                assertThat(adopsjon.getErEktefellesBarn()).isFalse();
                assertThat(adopsjon.getOmsorgovertakelseVilkår()).isEqualTo(OmsorgsovertakelseVilkårType.ES_FORELDREANSVARSVILKÅRET_2_LEDD);
            });
        });

        var historikk = repositoryProvider.getHistorikkinnslagRepository().hent(behandling.getSaksnummer());
        assertThat(historikk).hasSize(1);
        assertThat(historikk.getFirst().getSkjermlenke()).isEqualTo(SkjermlenkeType.FAKTA_OM_OMSORGSOVERTAKELSE);
        assertThat(historikk.getFirst().getLinjer()).hasSize(2);
        assertThat(historikk.getFirst().getLinjer().stream().map(HistorikkinnslagLinje::getTekst).anyMatch("__Adopsjons- og omsorgsvilkåret__ er satt til __Ikke oppfylt__."::equals)).isTrue();
        assertThat(historikk.getFirst().getLinjer().stream().map(HistorikkinnslagLinje::getTekst).anyMatch("begrunnelse."::equals)).isTrue();
    }

    @Test
    void skal_endre_omsorgsovertakelse_ektefelle_delvilkår_antallbarn() {
        // Arrange

        var scenario = ScenarioMorSøkerForeldrepenger.forAdopsjon();
        scenario.medSøknadHendelse()
            .medAntallBarn(2)
            .leggTilBarn(FØDSELSDATO)
            .leggTilBarn(FØDSELSDATO.minusYears(17))
            .medAdopsjon(scenario.medSøknadHendelse().getAdopsjonBuilder()
                .medOmsorgsovertakelseDato(OMSORGSOVERTAKELSESDATO)
                .medOmsorgovertalseVilkårType(OmsorgsovertakelseVilkårType.FP_ADOPSJONSVILKÅRET)
                .medErEktefellesBarn(false));

        scenario.leggTilAksjonspunkt(VURDER_OMSORGSOVERTAKELSEVILKÅRET, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);

        var behandling = scenario.lagre(repositoryProvider);


        var dto = new VurderOmsorgsovertakelseVilkårAksjonspunktDto("begrunnelse", OMSORGSOVERTAKELSESDATO.plusWeeks(1),
            Map.of(1, FØDSELSDATO), null, OmsorgsovertakelseVilkårType.FP_STEBARNSADOPSJONSVILKÅRET, true);

        var vilkårBuilder = VilkårResultat.builder();
        var oppdateringResultat = oppdater(behandling, dto, vilkårBuilder);
        vilkårBuilder.buildFor(behandling);

        // Assert
        assertThat(oppdateringResultat.getOverhoppKontroll()).isEqualTo(OverhoppKontroll.UTEN_OVERHOPP);
        assertThat(oppdateringResultat.getVilkårUtfallSomSkalLeggesTil()).hasSize(1);
        assertThat(oppdateringResultat.getVilkårUtfallSomSkalLeggesTil().getFirst().getVilkårType()).isEqualTo(VilkårType.OMSORGSOVERTAKELSEVILKÅR);
        assertThat(oppdateringResultat.getVilkårUtfallSomSkalLeggesTil().getFirst().getVilkårUtfallType()).isEqualTo(VilkårUtfallType.OPPFYLT);
        assertThat(oppdateringResultat.getVilkårUtfallSomSkalLeggesTil().getFirst().avslagsårsak()).isEqualTo(Avslagsårsak.UDEFINERT);

        var famHendelseOverstyrtOpt = familieHendelseTjeneste.hentAggregat(behandling.getId()).getOverstyrtVersjon();
        assertThat(famHendelseOverstyrtOpt).isPresent().hasValueSatisfying(overstyrt -> {
            assertThat(familieHendelseTjeneste.hentAggregat(behandling.getId()).getGjeldendeVersjon()).isEqualTo(overstyrt);
            assertThat(overstyrt.getAntallBarn()).isEqualTo(1);
            assertThat(overstyrt.getBarna().getFirst().getFødselsdato()).isEqualTo(FØDSELSDATO);
            assertThat(overstyrt.getSkjæringstidspunkt()).isEqualTo(OMSORGSOVERTAKELSESDATO.plusWeeks(1));
            assertThat(overstyrt.getAdopsjon()).isPresent().hasValueSatisfying(adopsjon -> {
                assertThat(adopsjon.getOmsorgsovertakelseDato()).isEqualTo(OMSORGSOVERTAKELSESDATO.plusWeeks(1));
                assertThat(adopsjon.getErEktefellesBarn()).isTrue();
                assertThat(adopsjon.getOmsorgovertakelseVilkår()).isEqualTo(OmsorgsovertakelseVilkårType.FP_STEBARNSADOPSJONSVILKÅRET);
            });
        });

        var historikk = repositoryProvider.getHistorikkinnslagRepository().hent(behandling.getSaksnummer());
        assertThat(historikk).hasSize(1);
        assertThat(historikk.getFirst().getSkjermlenke()).isEqualTo(SkjermlenkeType.FAKTA_OM_OMSORGSOVERTAKELSE);
        assertThat(historikk.getFirst().getLinjer()).hasSize(7);
        assertThat(historikk.getFirst().getLinjer().stream().map(HistorikkinnslagLinje::getTekst).anyMatch("__Adopsjons- og omsorgsvilkåret__ er satt til __Oppfylt__."::equals)).isTrue();
        assertThat(historikk.getFirst().getLinjer().stream().map(HistorikkinnslagLinje::getTekst).anyMatch("begrunnelse."::equals)).isTrue();
        assertThat(historikk.getFirst().getLinjer().stream().map(HistorikkinnslagLinje::getTekst).anyMatch("__Delvilkår__ er endret fra Adopsjon §14-5 første ledd til __Stebarnsadopsjon §14-5 tredje ledd__."::equals)).isTrue();
        assertThat(historikk.getFirst().getLinjer().stream().map(HistorikkinnslagLinje::getTekst).anyMatch("__Omsorgsovertakelsesdato__ er endret fra 09.10.2025 til __16.10.2025__."::equals)).isTrue();
        assertThat(historikk.getFirst().getLinjer().stream().map(HistorikkinnslagLinje::getTekst).anyMatch("__Ektefelles barn__ er endret fra Nei til __Ja__."::equals)).isTrue();
        assertThat(historikk.getFirst().getLinjer().stream().map(HistorikkinnslagLinje::getTekst).anyMatch("__Antall barn__ er endret fra 2 til __1__."::equals)).isTrue();
        assertThat(historikk.getFirst().getLinjer().stream().map(HistorikkinnslagLinje::getTekst).anyMatch("__Fødselsdato__ __09.10.2007__ er fjernet."::equals)).isTrue();
    }


    @Test
    void valideringer() {
        // Arrange
        var tjeneste = new VurderOmsorgsovertakelseVilkårAksjonspunktOppdaterer(repositoryProvider, familieHendelseTjeneste, opplysningsPeriodeTjeneste);

        var dto1 = new VurderOmsorgsovertakelseVilkårAksjonspunktDto("begrunnelse", OMSORGSOVERTAKELSESDATO, Map.of(1, FØDSELSDATO),
            Avslagsårsak.SØKER_ER_IKKE_MEDLEM, OmsorgsovertakelseVilkårType.ES_FORELDREANSVARSVILKÅRET_2_LEDD, false);
        var param1 = new AksjonspunktOppdaterParameter(null, dto1, null);
        assertThrows(IllegalArgumentException.class, () -> tjeneste.oppdater(dto1, param1));

        var dto2 = new VurderOmsorgsovertakelseVilkårAksjonspunktDto("begrunnelse", OMSORGSOVERTAKELSESDATO, Map.of(1, FØDSELSDATO),
            Avslagsårsak.SØKER_HAR_IKKE_FORELDREANSVAR, OmsorgsovertakelseVilkårType.UDEFINERT, false);
        var param2 = new AksjonspunktOppdaterParameter(null, dto2, null);
        assertThrows(IllegalArgumentException.class, () -> tjeneste.oppdater(dto2, param2));

        var dto3 = new VurderOmsorgsovertakelseVilkårAksjonspunktDto("begrunnelse", OMSORGSOVERTAKELSESDATO, Map.of(1, FØDSELSDATO),
            Avslagsårsak.SØKER_HAR_HATT_VANLIG_SAMVÆR_MED_BARNET, OmsorgsovertakelseVilkårType.FP_ADOPSJONSVILKÅRET, false);
        var param3 = new AksjonspunktOppdaterParameter(null, dto3, null);
        assertThrows(IllegalArgumentException.class, () -> tjeneste.oppdater(dto3, param3));
    }

    private OppdateringResultat oppdater(Behandling behandling, VurderOmsorgsovertakelseVilkårAksjonspunktDto dto, VilkårResultat.Builder vilkårBuilder) {
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getAksjonspunktDefinisjon());
        var resultat = new VurderOmsorgsovertakelseVilkårAksjonspunktOppdaterer(repositoryProvider, familieHendelseTjeneste, opplysningsPeriodeTjeneste)
            .oppdater(dto, new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto, aksjonspunkt));
        byggVilkårResultat(vilkårBuilder, resultat);
        return resultat;
    }

    private void byggVilkårResultat(VilkårResultat.Builder vilkårBuilder, OppdateringResultat delresultat) {
        delresultat.getVilkårUtfallSomSkalLeggesTil()
            .forEach(v -> vilkårBuilder.manueltVilkår(v.getVilkårType(), v.getVilkårUtfallType(), v.getAvslagsårsak()));
        delresultat.getVilkårTyperSomSkalFjernes().forEach(vilkårBuilder::fjernVilkår); // TODO: Vilkår burde ryddes på ein annen måte enn dette
    }

}
