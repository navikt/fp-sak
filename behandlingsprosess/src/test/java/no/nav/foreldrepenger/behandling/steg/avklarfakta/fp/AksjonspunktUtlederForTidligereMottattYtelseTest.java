package no.nav.foreldrepenger.behandling.steg.avklarfakta.fp;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerSvangerskapspenger;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.familiehendelse.YtelserSammeBarnTjeneste;

class AksjonspunktUtlederForTidligereMottattYtelseTest extends EntityManagerAwareTest {

    private static final LocalDate FØDSELSDATO = LocalDate.now();
    private static final LocalDate TERMINDATO = LocalDate.now();
    private BehandlingRepositoryProvider repositoryProvider;
    private AksjonspunktUtlederForTidligereMottattYtelse utleder;

    private Behandling lagre(AbstractTestScenario<?> scenario) {
        return scenario.lagre(repositoryProvider);
    }

    @BeforeEach
    void setUp() {
        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        var fhTjeneste = new FamilieHendelseTjeneste(null, repositoryProvider.getFamilieHendelseRepository());
        var ytelsetjeneste = new YtelserSammeBarnTjeneste(repositoryProvider, fhTjeneste);
        utleder = new AksjonspunktUtlederForTidligereMottattYtelse(ytelsetjeneste);
    }

    @Test
    void skal_ikke_opprette_aksjonspunkt_om_søker_ikke_har_mottatt_foreldrepenger_før() {
        // Arrange
        var aktørId = AktørId.dummy();
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var behandling = byggBehandlingFødsel(scenario, aktørId, FØDSELSDATO);

        // Act
        var aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater).isEmpty();
    }

    private AksjonspunktUtlederInput lagRef(Behandling behandling) {
        var skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(LocalDate.now()).build();
        return new AksjonspunktUtlederInput(BehandlingReferanse.fra(behandling), skjæringstidspunkt);
    }

    @Test
    void skal_ikke_opprette_aksjonspunkt_om_søker_mottatt_foreldrepenger_for_eldre_barn() {
        // Arrange
        var aktørId = AktørId.dummy();
        var scenarioEldre = ScenarioMorSøkerForeldrepenger.forFødsel();
        byggBehandlingFødsel(scenarioEldre, aktørId, FØDSELSDATO.minusYears(1));

        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var behandling = byggBehandlingTermin(scenario, aktørId, TERMINDATO);

        // Act
        var aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater).isEmpty();
    }

    @Test
    void skal_ikke_opprette_aksjonspunkt_om_søker_mottatt_svangerskapspenger_samme_barn() {
        // Arrange
        var aktørId = AktørId.dummy();
        var scenarioSVP = ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger();
        byggBehandlingTermin(scenarioSVP, aktørId, TERMINDATO);

        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var behandling = byggBehandlingTermin(scenario, aktørId, TERMINDATO);

        // Act
        var aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater).isEmpty();
    }

    @Test
    void skal_opprette_aksjonspunkt_om_soker_har_es_sak_under_behandling() {
        // Arrange
        var aktørId = AktørId.dummy();

        var scenarioES = ScenarioMorSøkerEngangsstønad.forFødsel();
        byggBehandlingTermin(scenarioES, aktørId, TERMINDATO.plusDays(7));

        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var behandling = byggBehandlingTermin(scenario, aktørId, TERMINDATO);

        // Act
        var aksjonspunktResultater = utleder.utledAksjonspunkterFor(lagRef(behandling));

        // Assert
        assertThat(aksjonspunktResultater).hasSize(1);
        assertThat(aksjonspunktResultater.get(0).getAksjonspunktDefinisjon())
                .isEqualTo(AksjonspunktDefinisjon.AVKLAR_OM_SØKER_HAR_MOTTATT_STØTTE);
    }


    private Behandling byggBehandlingFødsel(AbstractTestScenario<?> scenario, AktørId aktørId, LocalDate fødselsdato) {
        scenario.medSøknadHendelse().medFødselsDato(fødselsdato).medAntallBarn(1);
        scenario.medBruker(aktørId)
                .medSøknad().medMottattDato(LocalDate.now().minusWeeks(2));
        return lagre(scenario);
    }

    private Behandling byggBehandlingTermin(AbstractTestScenario<?> scenario, AktørId aktørId, LocalDate termindato) {
        scenario.medSøknadHendelse().medAntallBarn(1)
            .medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder()
                .medTermindato(termindato)
                .medNavnPå("LEGEN LEGESEN")
                .medUtstedtDato(termindato.minusMonths(1)));
        scenario.medBruker(aktørId)
            .medSøknad().medMottattDato(LocalDate.now().minusWeeks(2));
        return lagre(scenario);
    }


}
