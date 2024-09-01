package no.nav.foreldrepenger.behandling.steg.avklarfakta.es;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.familiehendelse.YtelserSammeBarnTjeneste;

class AksjonspunktUtlederForTidligereMottattYtelseTest extends EntityManagerAwareTest {

    private static final LocalDate FØDSELSDATO = LocalDate.now();
    private static final LocalDate TERMINDATO = LocalDate.now();
    private BehandlingRepositoryProvider repositoryProvider;
    private AksjonspunktUtlederForTidligereMottattYtelse utleder;
    private final Skjæringstidspunkt skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(LocalDate.now()).build();

    @BeforeEach
    void setUp() {
        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        var fhTjeneste = new FamilieHendelseTjeneste(null, repositoryProvider.getFamilieHendelseRepository());
        var ytelsetjeneste = new YtelserSammeBarnTjeneste(repositoryProvider, fhTjeneste);
        utleder = new AksjonspunktUtlederForTidligereMottattYtelse(ytelsetjeneste, repositoryProvider.getPersonopplysningRepository());
    }

    @Test
    void skal_ikke_opprette_aksjonspunkt_om_soker_ikke_har_mottatt_stønad_før() {
        // Arrange
        var aktørId = AktørId.dummy();
        var annenAktørId = AktørId.dummy();
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        var behandling = byggBehandlingFødsel(scenario, aktørId, annenAktørId, FØDSELSDATO);

        // Act
        var aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater).isEmpty();
    }

    private AksjonspunktUtlederInput lagRef(Behandling behandling) {
        return new AksjonspunktUtlederInput(BehandlingReferanse.fra(behandling), skjæringstidspunkt);
    }

    @Test
    void skal_ikke_opprette_aksjonspunkt_om_soker_har_mottatt_stønad_lenge_før() {
        // Arrange
        var aktørId = AktørId.dummy();
        var annenAktørId = AktørId.dummy();

        var scenarioEldre = ScenarioMorSøkerForeldrepenger.forFødsel();
        byggBehandlingFødsel(scenarioEldre, aktørId, annenAktørId, FØDSELSDATO.minusMonths(7));

        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var behandling = byggBehandlingTermin(scenario, aktørId, annenAktørId, TERMINDATO);

        // Act
        var aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater).isEmpty();
    }

    @Test
    void skal_opprette_aksjonspunkt_om_soker_har_mottatt_stønad_før() {
        // Arrange
        var aktørId = AktørId.dummy();
        var annenAktørId = AktørId.dummy();

        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        var behandling = byggBehandlingTermin(scenario, aktørId, annenAktørId, TERMINDATO);

        var scenarioNy = ScenarioMorSøkerEngangsstønad.forFødsel();
        var behandlingNy = byggBehandlingFødsel(scenarioNy, aktørId, annenAktørId, FØDSELSDATO.minusDays(2));

        // Act
        var aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater).hasSize(1);
        assertThat(aksjonspunktResultater.get(0).getAksjonspunktDefinisjon())
                .isEqualTo(AksjonspunktDefinisjon.AVKLAR_OM_SØKER_HAR_MOTTATT_STØTTE);
    }

    @Test
    void skal_opprette_aksjonspunkt_om_soker_har_foreldrepenge_sak_under_behandling() {
        // Arrange
        var aktørId = AktørId.dummy();
        var annenAktørId = AktørId.dummy();

        var scenarioFP = ScenarioMorSøkerForeldrepenger.forFødsel();
        byggBehandlingTermin(scenarioFP, aktørId, annenAktørId, TERMINDATO);

        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        var behandling = byggBehandlingTermin(scenario, aktørId, annenAktørId, TERMINDATO.minusWeeks(1));

        // Act
        var aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater).hasSize(1);
        assertThat(aksjonspunktResultater.get(0).getAksjonspunktDefinisjon())
                .isEqualTo(AksjonspunktDefinisjon.AVKLAR_OM_SØKER_HAR_MOTTATT_STØTTE);
    }

    @Test
    void skal_ikke_opprette_aksjonspunkt_hvis_behandling_har_type_REVURDERING() {
        // Arrange
        var aktørId = AktørId.dummy();
        var annenAktørId = AktørId.dummy();
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();

        var behandling = byggBehandlingTermin(scenario, aktørId, annenAktørId, TERMINDATO);

        var builder = Behandling.fraTidligereBehandling(behandling, BehandlingType.REVURDERING);
        var revurdering = builder.build();
        repositoryProvider.getBehandlingRepository().lagre(revurdering, repositoryProvider.getBehandlingRepository().taSkriveLås(revurdering));

        // Act
        var aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(revurdering));

        // Assert
        assertThat(aksjonspunktResultater).isEmpty();
    }


    @Test
    void skal_opprette_aksjonspunkt_om_soker_annenpart_har_mottatt_fp_før() {
        // Arrange
        var aktørId = AktørId.dummy();
        var annenAktørId = AktørId.dummy();
        var scenarioFar = ScenarioFarSøkerForeldrepenger.forFødsel();
        byggBehandlingFødsel(scenarioFar, annenAktørId, aktørId, FØDSELSDATO);
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        var behandling = byggBehandlingFødsel(scenario, aktørId, annenAktørId, FØDSELSDATO);

        // Act
        var aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater).hasSize(1);
        assertThat(aksjonspunktResultater.get(0).getAksjonspunktDefinisjon())
                .isEqualTo(AksjonspunktDefinisjon.AVKLAR_OM_ANNEN_FORELDRE_HAR_MOTTATT_STØTTE);
    }

    private Behandling byggBehandlingFødsel(AbstractTestScenario<?> scenario, AktørId aktørId, AktørId annenAktørId, LocalDate fødselsdato) {
        scenario.medSøknadHendelse().medFødselsDato(fødselsdato).medAntallBarn(1);
        scenario.medBruker(aktørId)
            .medSøknad().medMottattDato(LocalDate.now().minusWeeks(2));
        scenario.medSøknadAnnenPart().medAktørId(annenAktørId);
        return scenario.lagre(repositoryProvider);
    }

    private Behandling byggBehandlingTermin(AbstractTestScenario<?> scenario, AktørId aktørId, AktørId annenAktørId, LocalDate termindato) {
        scenario.medSøknadHendelse().medAntallBarn(1)
            .medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder()
                .medTermindato(termindato)
                .medNavnPå("LEGEN LEGESEN")
                .medUtstedtDato(termindato.minusMonths(1)));
        scenario.medBruker(aktørId)
            .medSøknad().medMottattDato(LocalDate.now().minusWeeks(2));
        scenario.medSøknadAnnenPart().medAktørId(annenAktørId);
        return scenario.lagre(repositoryProvider);
    }


}
