package no.nav.foreldrepenger.behandling.steg.avklarfakta.es;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.YtelserKonsolidertTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.YtelseBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder.AktørYtelseBuilder;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektsKilde;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektspostType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.OffentligYtelseType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.RelatertYtelseTilstand;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.vedtak.util.FPDateUtil;

public class AksjonspunktUtlederForTidligereMottattYtelseTest {

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());
    private InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
    private AksjonspunktUtlederForTidligereMottattYtelse utleder = new AksjonspunktUtlederForTidligereMottattYtelse(
        iayTjeneste,
        new YtelserKonsolidertTjeneste(repositoryProvider.getFagsakRepository()),
        new PersonopplysningRepository(repoRule.getEntityManager()));

    private Skjæringstidspunkt skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(FPDateUtil.iDag()).build();

    private Behandling lagre(AbstractTestScenario<?> scenario) {
        return scenario.lagre(repositoryProvider);
    }

    @Test
    public void skal_ikke_opprette_aksjonspunkt_om_soker_ikke_har_mottatt_stønad_før() {
        // Arrange
        AktørId aktørId = AktørId.dummy();
        AktørId annenAktørId = AktørId.dummy();
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        Behandling behandling = byggBehandling(scenario, aktørId, annenAktørId);

        // Act
        List<AksjonspunktResultat> aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater).isEmpty();
    }

    private AksjonspunktUtlederInput lagRef(Behandling behandling) {
        return new AksjonspunktUtlederInput(BehandlingReferanse.fra(behandling, skjæringstidspunkt));
    }

    @Test
    public void skal_ikke_opprette_aksjonspunkt_om_soker_har_mottatt_stønad_lenge_før() {
        // Arrange
        AktørId aktørId = AktørId.dummy();
        AktørId annenAktørId = AktørId.dummy();

        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        Behandling behandling = byggBehandling(scenario, aktørId, annenAktørId);

        leggTilYtelseForAktør(behandling, behandling.getAktørId(), RelatertYtelseType.FORELDREPENGER, FPDateUtil.iDag().minusMonths(15), FPDateUtil.iDag().minusMonths(5));

        // Act
        List<AksjonspunktResultat> aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater).isEmpty();
    }

    @Test
    public void skal_opprette_aksjonspunkt_om_soker_har_mottatt_stønad_før() {
        // Arrange
        AktørId aktørId = AktørId.dummy();
        AktørId annenAktørId = AktørId.dummy();

        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        Behandling behandling = byggBehandling(scenario, aktørId, annenAktørId);

        leggTilYtelseForAktør(behandling, behandling.getAktørId(), RelatertYtelseType.FORELDREPENGER, FPDateUtil.iDag().minusMonths(9), FPDateUtil.iDag().minusMonths(0));

        // Act
        List<AksjonspunktResultat> aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater).hasSize(1);
        assertThat(aksjonspunktResultater.get(0).getAksjonspunktDefinisjon())
            .isEqualTo(AksjonspunktDefinisjon.AVKLAR_OM_SØKER_HAR_MOTTATT_STØTTE);
    }

    @Test
    public void skal_opprette_aksjonspunkt_om_soker_har_foreldrepenge_sak_under_behandling() {
        // Arrange
        AktørId aktørId = AktørId.dummy();
        AktørId annenAktørId = AktørId.dummy();

        var scenarioFP = ScenarioMorSøkerForeldrepenger.forFødsel();
        byggBehandling(scenarioFP, aktørId, annenAktørId);

        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        Behandling behandling = byggBehandling(scenario, aktørId, annenAktørId);

        leggTilYtelseForAktør(behandling, behandling.getAktørId(), RelatertYtelseType.SYKEPENGER, FPDateUtil.iDag().minusMonths(5), FPDateUtil.iDag().minusMonths(4)); // For å

        // Act
        List<AksjonspunktResultat> aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater).hasSize(1);
        assertThat(aksjonspunktResultater.get(0).getAksjonspunktDefinisjon())
            .isEqualTo(AksjonspunktDefinisjon.AVKLAR_OM_SØKER_HAR_MOTTATT_STØTTE);
    }

    @Test
    public void skal_opprette_aksjonspunkt_om_soker_har__foreldrepenger_med_framtidig_start() {
        // Arrange
        AktørId aktørId = AktørId.dummy();
        AktørId annenAktørId = AktørId.dummy();

        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        Behandling behandling = byggBehandling(scenario, aktørId, annenAktørId);

        leggTilYtelseForAktør(behandling, behandling.getAktørId(), RelatertYtelseType.FORELDREPENGER, FPDateUtil.iDag().plusMonths(4), FPDateUtil.iDag().plusMonths(7));

        // Act
        List<AksjonspunktResultat> aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater).hasSize(1);
        assertThat(aksjonspunktResultater.get(0).getAksjonspunktDefinisjon())
            .isEqualTo(AksjonspunktDefinisjon.AVKLAR_OM_SØKER_HAR_MOTTATT_STØTTE);
    }

    @Test
    public void skal_opprette_aksjonspunkt_om_soker_har_mottatt_stønad_før_men_etter_stp() {
        // Arrange
        AktørId aktørId = AktørId.dummy();
        AktørId annenAktørId = AktørId.dummy();
        LocalDate fødselsdato = FPDateUtil.iDag().minusWeeks(6);

        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadDato(FPDateUtil.iDag());
        Behandling behandling = byggBehandling(scenario, aktørId, annenAktørId);

        leggTilYtelseForAktør(behandling, behandling.getAktørId(), RelatertYtelseType.ENGANGSSTØNAD, fødselsdato.plusWeeks(3), fødselsdato.plusWeeks(3));

        // Act
        List<AksjonspunktResultat> aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater).hasSize(1);
        assertThat(aksjonspunktResultater.get(0).getAksjonspunktDefinisjon())
            .isEqualTo(AksjonspunktDefinisjon.AVKLAR_OM_SØKER_HAR_MOTTATT_STØTTE);
    }

    @Test
    public void skal_opprette_aksjonspunkt_om_soker_har_inntekt_fra_stønad() {
        // Arrange
        AktørId aktørId = AktørId.dummy();
        AktørId annenAktørId = AktørId.dummy();

        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        Behandling behandling = byggBehandling(scenario, aktørId, annenAktørId);

        leggTilYtelseInntektForAktør(behandling.getId(), aktørId, FPDateUtil.iDag().minusMonths(2));

        // Act
        List<AksjonspunktResultat> aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater).hasSize(1);
        assertThat(aksjonspunktResultater.get(0).getAksjonspunktDefinisjon())
            .isEqualTo(AksjonspunktDefinisjon.AVKLAR_OM_SØKER_HAR_MOTTATT_STØTTE);
    }

    @Test
    public void skal_ikke_opprette_aksjonspunkt_hvis_behandling_har_type_REVURDERING() {
        // Arrange
        AktørId aktørId = AktørId.dummy();
        AktørId annenAktørId = AktørId.dummy();
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();

        Behandling behandling = byggBehandling(scenario, aktørId, annenAktørId);

        Behandling.Builder builder = Behandling.fraTidligereBehandling(behandling, BehandlingType.REVURDERING);
        Behandling revurdering = builder.build();

        // Act
        List<AksjonspunktResultat> aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(revurdering));

        // Assert
        assertThat(aksjonspunktResultater).isEmpty();
    }

    @Test
    public void skal_opprette_aksjonspunkt_om_soker_annenpart_har_mottatt_stønad_før() {
        // Arrange
        AktørId aktørId = AktørId.dummy();
        AktørId annenAktørId = AktørId.dummy();
        var scenario = ScenarioFarSøkerEngangsstønad.forFødsel();
        Behandling behandling = byggBehandling(scenario, aktørId, annenAktørId);

        leggTilYtelseForAktør(behandling, annenAktørId, RelatertYtelseType.ENGANGSSTØNAD, FPDateUtil.iDag().minusMonths(2), FPDateUtil.iDag().minusMonths(2));

        // Act
        List<AksjonspunktResultat> aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater).hasSize(1);
        assertThat(aksjonspunktResultater.get(0).getAksjonspunktDefinisjon())
            .isEqualTo(AksjonspunktDefinisjon.AVKLAR_OM_ANNEN_FORELDRE_HAR_MOTTATT_STØTTE);
    }

    @Test
    public void skal_opprette_aksjonspunkt_om_soker_annenpart_har_mottatt_fp_før() {
        // Arrange
        AktørId aktørId = AktørId.dummy();
        AktørId annenAktørId = AktørId.dummy();
        var scenarioFar = ScenarioFarSøkerForeldrepenger.forFødsel();
        @SuppressWarnings("unused")
        Behandling behandlingFar = byggBehandling(scenarioFar, annenAktørId, aktørId);
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        Behandling behandling = byggBehandling(scenario, aktørId, annenAktørId);
        leggTilYtelseForAktør(behandling, behandling.getAktørId(), RelatertYtelseType.ENGANGSSTØNAD, FPDateUtil.iDag().minusYears(2), FPDateUtil.iDag().minusYears(2));

        // Act
        List<AksjonspunktResultat> aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater).hasSize(1);
        assertThat(aksjonspunktResultater.get(0).getAksjonspunktDefinisjon())
            .isEqualTo(AksjonspunktDefinisjon.AVKLAR_OM_ANNEN_FORELDRE_HAR_MOTTATT_STØTTE);
    }

    private Behandling byggBehandling(AbstractTestScenario<?> scenario, AktørId aktørId, AktørId annenAktørId) {
        scenario.medBruker(aktørId)
            .medSøknad().medMottattDato(FPDateUtil.iDag().minusWeeks(2));
        scenario.medSøknadAnnenPart().medAktørId(annenAktørId);
        return lagre(scenario);
    }

    private void leggTilYtelseForAktør(Behandling behandling,
                                       AktørId aktørId,
                                       RelatertYtelseType relatertYtelseType, LocalDate fom, LocalDate tom) {

        var aggregatBuilder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
        aggregatBuilder.leggTilAktørYtelse(AktørYtelseBuilder.oppdatere(Optional.empty()).medAktørId(aktørId)
            .leggTilYtelse(YtelseBuilder.oppdatere(Optional.empty())
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom))
                .medStatus(RelatertYtelseTilstand.AVSLUTTET)
                .medYtelseType(relatertYtelseType)));
        iayTjeneste.lagreIayAggregat(behandling.getId(), aggregatBuilder);

    }

    private void leggTilYtelseInntektForAktør(Long behandlingId,
                                              AktørId aktørId, LocalDate tom) {

        var iayAggregatBuilder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
        var aktørInntektBuilder = iayAggregatBuilder.getAktørInntektBuilder(aktørId);
        var inntektBuilder = aktørInntektBuilder
            .getInntektBuilder(InntektsKilde.INNTEKT_OPPTJENING, new Opptjeningsnøkkel(null, null, aktørId.getId()));
        var inntektspost = inntektBuilder.getInntektspostBuilder()
            .medBeløp(BigDecimal.TEN)
            .medPeriode(tom.minusMonths(1), tom)
            .medInntektspostType(InntektspostType.YTELSE)
            .medYtelse(OffentligYtelseType.FORELDREPENGER);

        inntektBuilder.leggTilInntektspost(inntektspost);
        aktørInntektBuilder.leggTilInntekt(inntektBuilder);
        iayAggregatBuilder.leggTilAktørInntekt(aktørInntektBuilder);

        iayTjeneste.lagreIayAggregat(behandlingId, iayAggregatBuilder);
    }

}
