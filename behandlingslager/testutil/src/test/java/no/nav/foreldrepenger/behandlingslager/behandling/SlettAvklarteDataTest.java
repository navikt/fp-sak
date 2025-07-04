package no.nav.foreldrepenger.behandlingslager.behandling;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarnEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;

class SlettAvklarteDataTest extends EntityManagerAwareTest {

    private BehandlingRepository behandlingRepository;
    private FamilieHendelseRepository familieHendelseRepository;

    @BeforeEach
    void setUp() {
        var entityManager = getEntityManager();
        behandlingRepository = new BehandlingRepository(entityManager);
        familieHendelseRepository = new FamilieHendelseRepository(entityManager);
    }

    @Test
    void skal_slette_avklarte_omsorgsovertakelsedata() {
        // Arrange
        var scenario = ScenarioFarSøkerEngangsstønad.forAdopsjon();
        scenario.medSøknadHendelse(scenario.medSøknadHendelse()
            .medAdopsjon(scenario.medSøknadHendelse().getAdopsjonBuilder()
                .medOmsorgsovertakelseDato(LocalDate.now())).leggTilBarn(LocalDate.now().minusYears(5)).medAntallBarn(1));
        scenario.medBekreftetHendelse(scenario.medBekreftetHendelse()
            .medAdopsjon(scenario.medBekreftetHendelse().getAdopsjonBuilder()
                .medOmsorgsovertakelseDato(LocalDate.now())).leggTilBarn(LocalDate.now().minusYears(5)).medAntallBarn(1));

        var behandling = lagre(scenario);

        var lås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, lås);

        // Act
        familieHendelseRepository.slettAvklarteData(behandling.getId(), lås);

        // Assert
        var grunnlag = familieHendelseRepository.hentAggregat(behandling.getId());
        assertThat(grunnlag).isNotNull();
        assertThat(grunnlag.getOverstyrtVersjon().flatMap(FamilieHendelseEntitet::getAdopsjon)).isNotPresent();
        assertThat(grunnlag.getOverstyrtVersjon().map(FamilieHendelseEntitet::getAntallBarn)).isNotPresent();
        assertThat(grunnlag.getOverstyrtVersjon().map(FamilieHendelseEntitet::getBarna)).isNotPresent();
    }

    private Behandling lagre(AbstractTestScenario scenario) {
        return scenario.lagre(new BehandlingRepositoryProvider(getEntityManager()));
    }

    @Test
    void skal_slette_avklarte_fødseldata() {
        // Arrange
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse(scenario.medSøknadHendelse()
            .medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder()
                .medTermindato(LocalDate.now()).medNavnPå("LEGESEN").medUtstedtDato(LocalDate.now()))
            .medFødselsDato(LocalDate.now())
            .medAntallBarn(1));
        scenario.medBekreftetHendelse(scenario.medBekreftetHendelse()
            .medTerminbekreftelse(scenario.medBekreftetHendelse().getTerminbekreftelseBuilder()
                .medTermindato(LocalDate.now()).medNavnPå("LEGESEN").medUtstedtDato(LocalDate.now()))
            .medFødselsDato(LocalDate.now())
            .medAntallBarn(1));
        var behandling = lagre(scenario);

        var lås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, lås);

        // Act
        familieHendelseRepository.slettAvklarteData(behandling.getId(), lås);

        // Assert
        var grunnlag = familieHendelseRepository.hentAggregat(behandling.getId());
        assertThat(grunnlag).isNotNull();
        assertThat(grunnlag.getOverstyrtVersjon().flatMap(FamilieHendelseEntitet::getTerminbekreftelse)).isNotPresent();
        assertThat(grunnlag.getOverstyrtVersjon().map(FamilieHendelseEntitet::getAntallBarn)).isNotPresent();
        assertThat(grunnlag.getOverstyrtVersjon().map(FamilieHendelseEntitet::getBarna)).isNotPresent();
    }

    @Test
    void skal_slette_avklarte_adopsjonsdata() {
        // Arrange
        var scenario = ScenarioFarSøkerEngangsstønad.forAdopsjon();
        var familieHendelseBuilder = scenario.medSøknadHendelse();
        familieHendelseBuilder.medAntallBarn(1)
            .medFødselsDato(LocalDate.now())
            .medAdopsjon(familieHendelseBuilder.getAdopsjonBuilder().medOmsorgsovertakelseDato(LocalDate.now()));
        scenario.medBekreftetHendelse(scenario.medBekreftetHendelse()
            .medAdopsjon(scenario.medBekreftetHendelse().getAdopsjonBuilder().medOmsorgsovertakelseDato(LocalDate.now()))
            .leggTilBarn(new UidentifisertBarnEntitet(LocalDate.now(), 1)));

        var behandling = lagre(scenario);

        var lås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, lås);

        // Act
        familieHendelseRepository.slettAvklarteData(behandling.getId(), lås);

        // Assert
        var adopsjon = familieHendelseRepository.hentAggregat(behandling.getId()).getOverstyrtVersjon().flatMap(FamilieHendelseEntitet::getAdopsjon);
        assertThat(adopsjon).isNotPresent();
    }

}
