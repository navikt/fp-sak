package no.nav.foreldrepenger.skjæringstidspunkt.overganger;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;

class Utsettelse2021CoreTest {

    @Test
    void skal_returnere_sammenhengende_uttak_hvis_bekreftet_hendelse_før_dato() {
        // Arrange
        var ikraftredelse = LocalDate.of(2021, 10, 1);
        var skjæringsdato = ikraftredelse.minusWeeks(4);
        var bekreftetfødselsdato = skjæringsdato.plusWeeks(3);

        var førstegangScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        førstegangScenario.medBekreftetHendelse().medFødselsDato(bekreftetfødselsdato);
        var mockprovider = førstegangScenario.mockBehandlingRepositoryProvider();
        var behandling = førstegangScenario.lagMocked();

        // Act/Assert
        var fhg = mockprovider.getFamilieHendelseRepository().hentAggregat(behandling.getId());
        assertThat(UtsettelseCore2021.kreverSammenhengendeUttak(fhg)).isTrue();
    }

    @Test
    void skal_returnere_fritt_uttak_hvis_bekreftet_hendelse_etter_dato() {
        // Arrange
        var ikraftredelse = LocalDate.of(2021, 10, 1);
        var bekreftetfødselsdato = ikraftredelse.plusWeeks(3);

        var førstegangScenario = ScenarioMorSøkerForeldrepenger.forAdopsjon()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        førstegangScenario.medBekreftetHendelse()
            .medAdopsjon(førstegangScenario.medBekreftetHendelse().getAdopsjonBuilder().medOmsorgsovertakelseDato(bekreftetfødselsdato));
        var mockprovider = førstegangScenario.mockBehandlingRepositoryProvider();
        var behandling = førstegangScenario.lagMocked();

        // Act/Assert
        var fhg = mockprovider.getFamilieHendelseRepository().hentAggregat(behandling.getId());
        assertThat(UtsettelseCore2021.kreverSammenhengendeUttak(fhg)).isFalse();
    }

    @Test
    void skal_returnere_fritt_uttak_hvis_bekreftet_termin_20_dager_etter() {
        // Arrange
        var ikraftredelse = LocalDate.now().minusDays(20);
        var bekreftetfødselsdato = ikraftredelse.plusWeeks(3);

        var førstegangScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        førstegangScenario.medBekreftetHendelse()
            .medTerminbekreftelse(førstegangScenario.medBekreftetHendelse().getTerminbekreftelseBuilder()
                .medTermindato(bekreftetfødselsdato));
        var mockprovider = førstegangScenario.mockBehandlingRepositoryProvider();
        var behandling = førstegangScenario.lagMocked();

        // Act/Assert
        var fhg = mockprovider.getFamilieHendelseRepository().hentAggregat(behandling.getId());
        assertThat(UtsettelseCore2021.kreverSammenhengendeUttak(fhg)).isFalse();
    }

    @Test
    void skal_returnere_fritt_uttak_hvis_bekreftet_termin_2_dager_etter() {
        // Arrange
        var ikraftredelse = LocalDate.now().minusDays(2);
        var bekreftetfødselsdato = ikraftredelse.plusWeeks(3);

        var førstegangScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        førstegangScenario.medBekreftetHendelse()
            .medTerminbekreftelse(førstegangScenario.medBekreftetHendelse().getTerminbekreftelseBuilder()
                .medTermindato(bekreftetfødselsdato));
        var mockprovider = førstegangScenario.mockBehandlingRepositoryProvider();
        var behandling = førstegangScenario.lagMocked();

        // Act/Assert
        var fhg = mockprovider.getFamilieHendelseRepository().hentAggregat(behandling.getId());
        assertThat(UtsettelseCore2021.kreverSammenhengendeUttak(fhg)).isFalse();
    }

    @Test
    void skal_returnere_fritt_uttak_hvis_søkt_adopsjon_2_dager_etter() {
        // Arrange
        var ikraftredelse = LocalDate.now().minusDays(2);
        var bekreftetfødselsdato = ikraftredelse.plusWeeks(3);

        var førstegangScenario = ScenarioMorSøkerForeldrepenger.forAdopsjon()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        førstegangScenario.medSøknadHendelse()
            .medAdopsjon(førstegangScenario.medSøknadHendelse().getAdopsjonBuilder()
            .medOmsorgsovertakelseDato(bekreftetfødselsdato));
        var mockprovider = førstegangScenario.mockBehandlingRepositoryProvider();
        var behandling = førstegangScenario.lagMocked();

        // Act/Assert
        var fhg = mockprovider.getFamilieHendelseRepository().hentAggregat(behandling.getId());
        assertThat(UtsettelseCore2021.kreverSammenhengendeUttak(fhg)).isFalse();
    }

    @Test
    void skal_returnere_fritt_uttak_hvis_søkt_fødsel_10_dager_etter() {
        // Arrange
        var ikraftredelse = LocalDate.now().minusDays(10);
        var bekreftetfødselsdato = ikraftredelse.plusWeeks(3);

        var førstegangScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        førstegangScenario.medSøknadHendelse()
            .medFødselsDato(bekreftetfødselsdato);
        var mockprovider = førstegangScenario.mockBehandlingRepositoryProvider();
        var behandling = førstegangScenario.lagMocked();

        // Act/Assert
        var fhg = mockprovider.getFamilieHendelseRepository().hentAggregat(behandling.getId());
        assertThat(UtsettelseCore2021.kreverSammenhengendeUttak(fhg)).isFalse();
    }

    @Test
    void skal_returnere_fritt_uttak_hvis_søkt_termin_30_dager_etter() {
        // Arrange
        var ikraftredelse = LocalDate.now().minusDays(30);
        var bekreftetfødselsdato = ikraftredelse.plusWeeks(3);

        var førstegangScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        førstegangScenario.medSøknadHendelse()
            .medTerminbekreftelse(førstegangScenario.medSøknadHendelse().getTerminbekreftelseBuilder()
                .medTermindato(bekreftetfødselsdato));
        var mockprovider = førstegangScenario.mockBehandlingRepositoryProvider();
        var behandling = førstegangScenario.lagMocked();
        // Act/Assert
        var fhg = mockprovider.getFamilieHendelseRepository().hentAggregat(behandling.getId());
        assertThat(UtsettelseCore2021.kreverSammenhengendeUttak(fhg)).isFalse();
    }

}
