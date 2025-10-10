package no.nav.foreldrepenger.web.app.tjenester.behandling.fødsel;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.OmsorgsovertakelseVilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.fødsel.dto.Kilde;

public class OmsorgsovertakelseTjenesteTest extends EntityManagerAwareTest {
    private static final LocalDate OMSORGSOVERTAGELSEDATO = LocalDate.now();
    private static final LocalDate FØDSELSDATO = LocalDate.now().minusMonths(10);

    private BehandlingRepositoryProvider repositoryProvider;
    private OmsorgsovertakelseTjeneste tjeneste;

    @BeforeEach
    void setUp() {
        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        var fhTjeneste = new FamilieHendelseTjeneste(null, repositoryProvider.getFamilieHendelseRepository());
        tjeneste = new OmsorgsovertakelseTjeneste(fhTjeneste, repositoryProvider.getBehandlingRepository(),
            repositoryProvider.getSøknadRepository(), repositoryProvider.getBehandlingsresultatRepository());
    }

    @Test
    void skal_kunne_hente_fakta_om_omsorgsovertakelse_kun_søknad() {
        // Arrange
        var scenario = ScenarioMorSøkerForeldrepenger.forAdopsjon();
        byggSøknadhendelseTermin(scenario, OMSORGSOVERTAGELSEDATO, 1, true, OmsorgsovertakelseVilkårType.UDEFINERT);

        var behandling = scenario.lagre(repositoryProvider);

        // Act
        var omsorgsovertakelseDto = tjeneste.hentOmsorgsovertakelse(behandling.getId());

        // Assert
        var gjeldende = omsorgsovertakelseDto.gjeldende();
        assertThat(gjeldende).isNotNull();
        assertThat(omsorgsovertakelseDto.kildeGjeldende()).isEqualTo(Kilde.SØKNAD);
        assertThat(gjeldende).isEqualTo(omsorgsovertakelseDto.søknad());
        assertThat(gjeldende.antallBarn()).isEqualTo(1);
        assertThat(gjeldende.delvilkår()).isEqualTo(OmsorgsovertakelseVilkårType.FP_STEBARNSADOPSJONSVILKÅRET);
        assertThat(gjeldende.omsorgsovertakelseDato()).isEqualTo(OMSORGSOVERTAGELSEDATO);
        assertThat(gjeldende.erEktefellesBarn()).isTrue();
        assertThat(omsorgsovertakelseDto.register()).isNotNull();
        assertThat(omsorgsovertakelseDto.register().barn()).isEmpty();
        assertThat(omsorgsovertakelseDto.aktuelleDelvilkårAvslagsårsaker().keySet()).hasSize(3);
        assertThat(omsorgsovertakelseDto.aktuelleDelvilkårAvslagsårsaker().get(OmsorgsovertakelseVilkårType.FP_STEBARNSADOPSJONSVILKÅRET)).hasSize(6);
        assertThat(omsorgsovertakelseDto.saksbehandlerVurdering()).isNull();
    }

    @Test
    void skal_kunne_hente_fakta_om_omsorgsovertakelse_søknad_register() {
        // Arrange
        var scenario = ScenarioMorSøkerEngangsstønad.forAdopsjon();
        byggSøknadhendelseTermin(scenario, OMSORGSOVERTAGELSEDATO, 2, false, OmsorgsovertakelseVilkårType.ES_ADOPSJONSVILKÅRET);
        scenario.medBekreftetHendelse().medAntallBarn(2).leggTilBarn(FØDSELSDATO).leggTilBarn(FØDSELSDATO.minusYears(1));

        var behandling = scenario.lagre(repositoryProvider);

        // Act
        var omsorgsovertakelseDto = tjeneste.hentOmsorgsovertakelse(behandling.getId());

        // Assert
        var gjeldende = omsorgsovertakelseDto.gjeldende();
        assertThat(gjeldende).isNotNull();
        assertThat(omsorgsovertakelseDto.kildeGjeldende()).isEqualTo(Kilde.SØKNAD);
        assertThat(gjeldende).isEqualTo(omsorgsovertakelseDto.søknad());
        assertThat(gjeldende.antallBarn()).isEqualTo(2);
        assertThat(gjeldende.delvilkår()).isEqualTo(OmsorgsovertakelseVilkårType.ES_ADOPSJONSVILKÅRET);
        assertThat(gjeldende.omsorgsovertakelseDato()).isEqualTo(OMSORGSOVERTAGELSEDATO);
        assertThat(gjeldende.erEktefellesBarn()).isFalse();
        assertThat(omsorgsovertakelseDto.register()).isNotNull();
        assertThat(omsorgsovertakelseDto.register().barn()).hasSize(2);
        assertThat(omsorgsovertakelseDto.aktuelleDelvilkårAvslagsårsaker().keySet()).hasSize(4);
        assertThat(omsorgsovertakelseDto.aktuelleDelvilkårAvslagsårsaker().get(OmsorgsovertakelseVilkårType.ES_ADOPSJONSVILKÅRET)).hasSize(7);
        assertThat(omsorgsovertakelseDto.saksbehandlerVurdering()).isNull();
    }

    @Test
    void skal_kunne_hente_fakta_om_saksbehandlet_innvilget() {
        // Arrange
        var scenario = ScenarioMorSøkerEngangsstønad.forAdopsjon();
        byggSøknadhendelseTermin(scenario, OMSORGSOVERTAGELSEDATO, 1, false, OmsorgsovertakelseVilkårType.ES_FORELDREANSVARSVILKÅRET_4_LEDD);

        scenario.medBekreftetHendelse().medAntallBarn(1).leggTilBarn(FØDSELSDATO);

        var overstyrthendelse = scenario.medOverstyrtHendelse();
        var adopsjonBuilder = overstyrthendelse.getAdopsjonBuilder()
            .medOmsorgsovertakelseDato(OMSORGSOVERTAGELSEDATO.minusWeeks(1))
            .medErEktefellesBarn(false)
            .medOmsorgovertalseVilkårType(OmsorgsovertakelseVilkårType.ES_FORELDREANSVARSVILKÅRET_2_LEDD);
        overstyrthendelse.medAdopsjon(adopsjonBuilder).medAntallBarn(1).medFødselsDato(FØDSELSDATO);

        scenario.leggTilVilkår(VilkårType.OMSORGSOVERTAKELSEVILKÅR, VilkårUtfallType.OPPFYLT);


        var behandling = scenario.lagre(repositoryProvider);

        // Act
        var omsorgsovertakelseDto = tjeneste.hentOmsorgsovertakelse(behandling.getId());

        // Assert
        assertThat(omsorgsovertakelseDto.kildeGjeldende()).isEqualTo(Kilde.SAKSBEHANDLER);
        assertThat(omsorgsovertakelseDto.register()).isNotNull();
        assertThat(omsorgsovertakelseDto.register().barn()).hasSize(1);
        assertThat(omsorgsovertakelseDto.søknad().antallBarn()).isEqualTo(1);
        assertThat(omsorgsovertakelseDto.søknad().delvilkår()).isEqualTo(OmsorgsovertakelseVilkårType.ES_FORELDREANSVARSVILKÅRET_4_LEDD);
        assertThat(omsorgsovertakelseDto.søknad().omsorgsovertakelseDato()).isEqualTo(OMSORGSOVERTAGELSEDATO);
        assertThat(omsorgsovertakelseDto.søknad().erEktefellesBarn()).isFalse();
        assertThat(omsorgsovertakelseDto.søknad().barn()).hasSize(1);

        var gjeldende = omsorgsovertakelseDto.gjeldende();
        assertThat(gjeldende).isNotNull();
        assertThat(gjeldende.antallBarn()).isEqualTo(1);
        assertThat(gjeldende.delvilkår()).isEqualTo(OmsorgsovertakelseVilkårType.ES_FORELDREANSVARSVILKÅRET_2_LEDD);
        assertThat(gjeldende.omsorgsovertakelseDato()).isEqualTo(OMSORGSOVERTAGELSEDATO.minusWeeks(1));
        assertThat(gjeldende.erEktefellesBarn()).isFalse();
        assertThat(gjeldende.barn()).hasSize(1);
        assertThat(omsorgsovertakelseDto.aktuelleDelvilkårAvslagsårsaker().keySet()).hasSize(4);
        assertThat(omsorgsovertakelseDto.aktuelleDelvilkårAvslagsårsaker().get(OmsorgsovertakelseVilkårType.ES_FORELDREANSVARSVILKÅRET_2_LEDD)).hasSize(7);
        var vurdering = omsorgsovertakelseDto.saksbehandlerVurdering();
        assertThat(vurdering).isNotNull();
        assertThat(vurdering.vilkårUtfallType()).isEqualTo(VilkårUtfallType.OPPFYLT);
        assertThat(vurdering.avslagsårsak()).isNull();
    }

    @Test
    void skal_kunne_hente_fakta_om_saksbehandlet_avslått() {
        // Arrange
        var scenario = ScenarioMorSøkerForeldrepenger.forAdopsjon();
        byggSøknadhendelseTermin(scenario, OMSORGSOVERTAGELSEDATO, 1, true, OmsorgsovertakelseVilkårType.FP_FORELDREANSVARSVILKÅRET_2_LEDD);

        var overstyrthendelse = scenario.medOverstyrtHendelse();
        var adopsjonBuilder = overstyrthendelse.getAdopsjonBuilder()
            .medOmsorgsovertakelseDato(OMSORGSOVERTAGELSEDATO)
            .medErEktefellesBarn(false)
            .medOmsorgovertalseVilkårType(OmsorgsovertakelseVilkårType.FP_FORELDREANSVARSVILKÅRET_2_LEDD);
        overstyrthendelse.medAdopsjon(adopsjonBuilder).medAntallBarn(1).medFødselsDato(FØDSELSDATO);

        scenario.leggTilVilkår(VilkårType.OMSORGSOVERTAKELSEVILKÅR, VilkårUtfallType.IKKE_OPPFYLT);
        scenario.medBehandlingsresultat(Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.AVSLÅTT).medAvslagsårsak(Avslagsårsak.SØKER_HAR_IKKE_FORELDREANSVAR));

        var behandling = scenario.lagre(repositoryProvider);

        // Act
        var omsorgsovertakelseDto = tjeneste.hentOmsorgsovertakelse(behandling.getId());

        // Assert
        assertThat(omsorgsovertakelseDto.kildeGjeldende()).isEqualTo(Kilde.SAKSBEHANDLER);
        assertThat(omsorgsovertakelseDto.register()).isNotNull();
        assertThat(omsorgsovertakelseDto.register().barn()).isEmpty();
        assertThat(omsorgsovertakelseDto.søknad().antallBarn()).isEqualTo(1);
        assertThat(omsorgsovertakelseDto.søknad().delvilkår()).isEqualTo(OmsorgsovertakelseVilkårType.FP_FORELDREANSVARSVILKÅRET_2_LEDD);
        assertThat(omsorgsovertakelseDto.søknad().omsorgsovertakelseDato()).isEqualTo(OMSORGSOVERTAGELSEDATO);
        assertThat(omsorgsovertakelseDto.søknad().erEktefellesBarn()).isTrue();
        assertThat(omsorgsovertakelseDto.søknad().barn()).hasSize(1);

        var gjeldende = omsorgsovertakelseDto.gjeldende();
        assertThat(gjeldende).isNotNull();
        assertThat(gjeldende.antallBarn()).isEqualTo(1);
        assertThat(gjeldende.delvilkår()).isEqualTo(OmsorgsovertakelseVilkårType.FP_FORELDREANSVARSVILKÅRET_2_LEDD);
        assertThat(gjeldende.omsorgsovertakelseDato()).isEqualTo(OMSORGSOVERTAGELSEDATO);
        assertThat(gjeldende.erEktefellesBarn()).isFalse();
        assertThat(gjeldende.barn()).hasSize(1);
        assertThat(omsorgsovertakelseDto.aktuelleDelvilkårAvslagsårsaker().keySet()).hasSize(3);
        assertThat(omsorgsovertakelseDto.aktuelleDelvilkårAvslagsårsaker().get(OmsorgsovertakelseVilkårType.FP_FORELDREANSVARSVILKÅRET_2_LEDD)).hasSize(9);
        var vurdering = omsorgsovertakelseDto.saksbehandlerVurdering();
        assertThat(vurdering).isNotNull();
        assertThat(vurdering.vilkårUtfallType()).isEqualTo(VilkårUtfallType.IKKE_OPPFYLT);
        assertThat(vurdering.avslagsårsak()).isNull(); // Får ikke satt den i scenariobuilder
    }


    private void byggSøknadhendelseTermin(AbstractTestScenario<?> scenario, LocalDate omsorgsovertakelsedato, int antallBarn,
                                          Boolean ektefellesbarn, OmsorgsovertakelseVilkårType delvilkår) {
        var søknadshendelse = scenario.medSøknadHendelse();
        var adopsjonBuilder = søknadshendelse.getAdopsjonBuilder()
            .medOmsorgsovertakelseDato(omsorgsovertakelsedato)
            .medErEktefellesBarn(ektefellesbarn)
            .medOmsorgovertalseVilkårType(delvilkår);
        søknadshendelse.medAdopsjon(adopsjonBuilder).medAntallBarn(antallBarn).medFødselsDato(FØDSELSDATO);
        if (antallBarn == 2) {
            søknadshendelse.leggTilBarn(FØDSELSDATO.minusYears(1));
        }
        scenario.medBruker(AktørId.dummy()).medSøknad().medMottattDato(LocalDate.now().minusWeeks(2));
        scenario.medSøknadAnnenPart().medAktørId(AktørId.dummy());
    }
}

