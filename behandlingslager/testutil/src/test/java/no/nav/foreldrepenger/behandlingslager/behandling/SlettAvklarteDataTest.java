package no.nav.foreldrepenger.behandlingslager.behandling;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.AdopsjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarnEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.VurdertMedlemskap;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;

public class SlettAvklarteDataTest extends EntityManagerAwareTest {

    private BehandlingRepository behandlingRepository;
    private MedlemskapRepository medlemskapRepository;
    private FamilieHendelseRepository familieHendelseRepository;

    @BeforeEach
    void setUp() {
        var entityManager = getEntityManager();
        behandlingRepository = new BehandlingRepository(entityManager);
        medlemskapRepository = new MedlemskapRepository(entityManager);
        familieHendelseRepository = new FamilieHendelseRepository(entityManager);
    }

    @Test
    public void skal_slette_avklarte_omsorgsovertakelsedata() {
        // Arrange
        final ScenarioFarSøkerEngangsstønad scenario = ScenarioFarSøkerEngangsstønad.forAdopsjon();
        scenario.medSøknadHendelse(scenario.medSøknadHendelse()
            .medAdopsjon(scenario.medSøknadHendelse().getAdopsjonBuilder()
                .medOmsorgsovertakelseDato(LocalDate.now())).medAntallBarn(1));
        scenario.medBekreftetHendelse(scenario.medBekreftetHendelse()
            .medAdopsjon(scenario.medBekreftetHendelse().getAdopsjonBuilder()
                .medOmsorgsovertakelseDato(LocalDate.now())).medAntallBarn(1));

        Behandling behandling = lagre(scenario);

        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, lås);

        // Act
        familieHendelseRepository.slettAvklarteData(behandling.getId(), lås);

        // Assert
        final FamilieHendelseGrunnlagEntitet grunnlag = familieHendelseRepository.hentAggregat(behandling.getId());
        assertThat(grunnlag).isNotNull();
        assertThat(grunnlag.getOverstyrtVersjon().flatMap(FamilieHendelseEntitet::getAdopsjon)).isNotPresent();
        assertThat(grunnlag.getOverstyrtVersjon().map(FamilieHendelseEntitet::getAntallBarn)).isNotPresent();
        assertThat(grunnlag.getOverstyrtVersjon().map(FamilieHendelseEntitet::getBarna)).isNotPresent();
    }

    private Behandling lagre(AbstractTestScenario scenario) {
        return scenario.lagre(new BehandlingRepositoryProvider(getEntityManager()));
    }

    @Test
    public void skal_slette_avklarte_fødseldata() {
        // Arrange
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
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
        Behandling behandling = lagre(scenario);

        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, lås);

        // Act
        familieHendelseRepository.slettAvklarteData(behandling.getId(), lås);

        // Assert
        final FamilieHendelseGrunnlagEntitet grunnlag = familieHendelseRepository.hentAggregat(behandling.getId());
        assertThat(grunnlag).isNotNull();
        assertThat(grunnlag.getOverstyrtVersjon().flatMap(FamilieHendelseEntitet::getTerminbekreftelse)).isNotPresent();
        assertThat(grunnlag.getOverstyrtVersjon().map(FamilieHendelseEntitet::getAntallBarn)).isNotPresent();
        assertThat(grunnlag.getOverstyrtVersjon().map(FamilieHendelseEntitet::getBarna)).isNotPresent();
    }

    @Test
    public void skal_slette_avklarte_adopsjonsdata() {
        // Arrange
        final ScenarioFarSøkerEngangsstønad scenario = ScenarioFarSøkerEngangsstønad.forAdopsjon();
        final FamilieHendelseBuilder familieHendelseBuilder = scenario.medSøknadHendelse();
        familieHendelseBuilder.medAntallBarn(1)
            .medFødselsDato(LocalDate.now())
            .medAdopsjon(familieHendelseBuilder.getAdopsjonBuilder().medOmsorgsovertakelseDato(LocalDate.now()));
        scenario.medBekreftetHendelse(scenario.medBekreftetHendelse()
            .medAdopsjon(scenario.medBekreftetHendelse().getAdopsjonBuilder()
                .medOmsorgsovertakelseDato(LocalDate.now()))
            .leggTilBarn(new UidentifisertBarnEntitet(LocalDate.now(), 1)));

        Behandling behandling = lagre(scenario);

        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, lås);

        // Act
        familieHendelseRepository.slettAvklarteData(behandling.getId(), lås);

        // Assert
        final Optional<AdopsjonEntitet> adopsjon = familieHendelseRepository.hentAggregat(behandling.getId())
            .getOverstyrtVersjon().flatMap(FamilieHendelseEntitet::getAdopsjon);
        assertThat(adopsjon).isNotPresent();
    }

    @Test
    public void skal_slette_avklarte_medlemskapdata() {
        // Arrange
        ScenarioFarSøkerEngangsstønad scenario = ScenarioFarSøkerEngangsstønad.forAdopsjon();
        final FamilieHendelseBuilder familieHendelseBuilder = scenario.medSøknadHendelse();
        familieHendelseBuilder.medAntallBarn(1)
            .medFødselsDato(LocalDate.now())
            .medAdopsjon(familieHendelseBuilder.getAdopsjonBuilder().medOmsorgsovertakelseDato(LocalDate.now()));
        scenario.medMedlemskap().build();
        Behandling behandling = lagre(scenario);

        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, lås);

        // Act
        Long behandlingId = behandling.getId();
        medlemskapRepository.slettAvklarteMedlemskapsdata(behandlingId, lås);

        // Assert
        Optional<MedlemskapAggregat> medlemskap = medlemskapRepository.hentMedlemskap(behandlingId);
        assertThat(medlemskap).isPresent();

        Optional<VurdertMedlemskap> vurdertMedlemskap = medlemskapRepository.hentVurdertMedlemskap(behandlingId);
        assertThat(vurdertMedlemskap).isNotPresent();

        assertThat(medlemskap.get().getVurdertMedlemskap()).isNotPresent();
    }
}
