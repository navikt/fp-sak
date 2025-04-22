package no.nav.foreldrepenger.familiehendelse.omsorg;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AVKLAR_VILKÅR_FOR_OMSORGSOVERTAKELSE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.OmsorgsovertakelseVilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.FarSøkerType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning.Personopplysning;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.AvklarOmsorgOgForeldreansvarOppdaterer;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.AvklarFaktaForOmsorgOgForeldreansvarAksjonspunktDto;
import no.nav.foreldrepenger.skjæringstidspunkt.OpplysningsPeriodeTjeneste;

class AvklarOmsorgOgForeldreansvarOppdatererTest extends EntityManagerAwareTest {

    private final ScenarioFarSøkerEngangsstønad scenario = ScenarioFarSøkerEngangsstønad.forAdopsjon();
    private final VilkårResultat.Builder vilkårBuilder = VilkårResultat.builder();

    private BehandlingRepositoryProvider repositoryProvider;
    private FamilieHendelseTjeneste familieHendelseTjeneste;

    @BeforeEach
    void setUp() {
        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        familieHendelseTjeneste = new FamilieHendelseTjeneste(null, repositoryProvider.getFamilieHendelseRepository());
    }

    @Test
    void skal_oppdatere_vilkår_for_omsorg() {
        // Arrange
        var forelderId = AktørId.dummy();

        scenario.medSøknadHendelse()
            .medAntallBarn(2)
            .leggTilBarn(LocalDate.now().minusYears(1)).leggTilBarn(LocalDate.now().minusYears(1))
            .medAdopsjon(scenario.medSøknadHendelse().getAdopsjonBuilder().medOmsorgsovertakelseDato(LocalDate.now()));

        var forelder = scenario.opprettBuilderForRegisteropplysninger()
            .leggTilPersonopplysninger(
                Personopplysning.builderMedDefaultVerdier(forelderId)
                    .navn("Forelder"))
            .build();

        scenario.medRegisterOpplysninger(forelder);
        scenario.leggTilAksjonspunkt(AVKLAR_VILKÅR_FOR_OMSORGSOVERTAKELSE, BehandlingStegType.KONTROLLER_FAKTA);

        var behandling = scenario.lagre(repositoryProvider);

        var dto = new AvklarFaktaForOmsorgOgForeldreansvarAksjonspunktDto();
        dto.setOmsorgsovertakelseDato(LocalDate.now());
        dto.setVilkårType(VilkårType.OMSORGSVILKÅRET);

        avklarOmsorgOgForeldreansvar(behandling, dto);
        vilkårBuilder.buildFor(behandling);

        // Assert
        var gjellendeVersjon = repositoryProvider.getFamilieHendelseRepository().hentAggregat(behandling.getId()).getGjeldendeVersjon();
        var adopsjon = gjellendeVersjon.getAdopsjon();
        assertThat(gjellendeVersjon.getAntallBarn()).isEqualTo(2);
        assertThat(adopsjon).hasValueSatisfying(value -> {
            assertThat(value.getOmsorgsovertakelseDato()).as("omsorgsovertakelsesDato").isEqualTo(LocalDate.now());
            assertThat(value.getOmsorgovertakelseVilkår()).as("omsorgsovertakelsesVilkår").isEqualTo(OmsorgsovertakelseVilkårType.OMSORGSVILKÅRET);
        });
    }

    private OppdateringResultat avklarOmsorgOgForeldreansvar(Behandling behandling, AvklarFaktaForOmsorgOgForeldreansvarAksjonspunktDto dto) {
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getAksjonspunktDefinisjon());
        var resultat = new AvklarOmsorgOgForeldreansvarOppdaterer(repositoryProvider, mock(OpplysningsPeriodeTjeneste.class), familieHendelseTjeneste)
            .oppdater(dto, new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto, aksjonspunkt));
        byggVilkårResultat(vilkårBuilder, resultat);
        return resultat;
    }

    private void byggVilkårResultat(VilkårResultat.Builder vilkårBuilder, OppdateringResultat delresultat) {
        delresultat.getVilkårUtfallSomSkalLeggesTil()
            .forEach(v -> vilkårBuilder.manueltVilkår(v.getVilkårType(), v.getVilkårUtfallType(), v.getAvslagsårsak()));
        delresultat.getVilkårTyperSomSkalFjernes().forEach(vilkårBuilder::fjernVilkår); // TODO: Vilkår burde ryddes på ein annen måte enn dette
    }

    @Test
    void skal_sette_andre_aksjonspunkter_knyttet_til_omsorgsvilkåret_som_utført() {
        // Arrange
        var forelderId = AktørId.dummy();
        var dødsdato = LocalDate.now();
        scenario.medSøknadHendelse()
            .medAdopsjon(scenario.medSøknadHendelse().getAdopsjonBuilder()
                .medOmsorgsovertakelseDato(LocalDate.now()));
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_VILKÅR_FOR_OMSORGSOVERTAKELSE,
            BehandlingStegType.SØKERS_RELASJON_TIL_BARN);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_FORELDREANSVARSVILKÅRET_2_LEDD,
            BehandlingStegType.SØKERS_RELASJON_TIL_BARN);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_FORELDREANSVARSVILKÅRET_4_LEDD,
            BehandlingStegType.SØKERS_RELASJON_TIL_BARN);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_OMSORGSVILKÅRET,
            BehandlingStegType.SØKERS_RELASJON_TIL_BARN);

        var forelder = scenario.opprettBuilderForRegisteropplysninger()
            .leggTilPersonopplysninger(
                Personopplysning.builderMedDefaultVerdier(forelderId)
                    .dødsdato(dødsdato)
                    .navn("Navn"))
            .build();

        scenario.medRegisterOpplysninger(forelder);
        var behandling = scenario.lagre(repositoryProvider);

        var dto = new AvklarFaktaForOmsorgOgForeldreansvarAksjonspunktDto();
        dto.setOmsorgsovertakelseDato(LocalDate.now());
        dto.setVilkårType(VilkårType.OMSORGSVILKÅRET);

        var resultat = avklarOmsorgOgForeldreansvar(behandling, dto);

        // Assert
        assertThat(resultat.getVilkårTyperSomSkalFjernes()).isEmpty();
        assertThat(resultat.getEkstraAksjonspunktResultat().stream().filter(ear -> AksjonspunktStatus.AVBRUTT.equals(ear.aksjonspunktStatus())))
            .anySatisfy(ear -> assertThat(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_OMSORGSVILKÅRET).isEqualTo(ear.aksjonspunktDefinisjon()));

        assertThat(resultat.getEkstraAksjonspunktResultat().stream().filter(ear -> AksjonspunktStatus.AVBRUTT.equals(ear.aksjonspunktStatus())))
            .allMatch(apr -> !Objects.equals(AksjonspunktDefinisjon.AVKLAR_VILKÅR_FOR_OMSORGSOVERTAKELSE, apr.aksjonspunktDefinisjon()));

    }

    @Test
    void skal_generere_historikkinnslag_ved_avklaring_av_omsorgsovertakelsesdato() {
        // Arrange
        var omsorgsovertakelsesdatoOppgitt = LocalDate.of(2019, 3, 4);
        var omsorgsovertakelsesdatoBekreftet = omsorgsovertakelsesdatoOppgitt.plusDays(1);

        // Behandling
        scenario.medSøknad()
            .medFarSøkerType(FarSøkerType.OVERTATT_OMSORG);
        scenario.medSøknadHendelse()
            .medAdopsjon(scenario.medSøknadHendelse().getAdopsjonBuilder()
                .medOmsorgsovertakelseDato(omsorgsovertakelsesdatoOppgitt));
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_VILKÅR_FOR_OMSORGSOVERTAKELSE, BehandlingStegType.KONTROLLER_FAKTA);
        scenario.lagre(repositoryProvider);

        var behandling = scenario.getBehandling();

        // Dto
        var dto = new AvklarFaktaForOmsorgOgForeldreansvarAksjonspunktDto();
        dto.setOmsorgsovertakelseDato(omsorgsovertakelsesdatoBekreftet);
        dto.setVilkårType(VilkårType.OMSORGSVILKÅRET);

        avklarOmsorgOgForeldreansvar(behandling, dto);
        var historikkinnslag = repositoryProvider.getHistorikkinnslagRepository().hent(behandling.getSaksnummer()).getFirst();
        var linjer = historikkinnslag.getLinjer();

        // Assert
        assertThat(linjer).hasSize(2);
        assertThat(linjer.getFirst().getTekst()).contains("Omsorgsovertakelsesdato", "04.03.2019", "05.03.2019");
    }
}
