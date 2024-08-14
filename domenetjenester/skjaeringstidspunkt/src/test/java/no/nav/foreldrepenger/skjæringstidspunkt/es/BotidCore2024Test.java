package no.nav.foreldrepenger.skjæringstidspunkt.es;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Month;
import java.time.Period;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;

class BotidCore2024Test {

    private static final LocalDate IKRAFT = LocalDate.of(2024, Month.AUGUST, 1);
    private static final Period OVERGANG = Period.parse("P18W3D");

    @Test
    void skal_returnere_uten_botidskrav_hvis_bekreftet_termin_før_ikrafttredelsedato() {
        // Arrange
        var bekreftettermindato = IKRAFT.plusWeeks(10);

        var førstegangScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        var terminbekreftelse = førstegangScenario.medBekreftetHendelse().getTerminbekreftelseBuilder()
            .medTermindato(bekreftettermindato)
            .medNavnPå("LEGEN LEGESEN")
            .medUtstedtDato(bekreftettermindato.minusMonths(2));
        førstegangScenario.medBekreftetHendelse().medTerminbekreftelse(terminbekreftelse);
        var mockprovider = førstegangScenario.mockBehandlingRepositoryProvider();
        var behandling = førstegangScenario.lagMocked();

        // Act/Assert
        var fhg = mockprovider.getFamilieHendelseRepository().hentAggregat(behandling.getId());
        assertThat(new BotidCore2024(IKRAFT, OVERGANG).ikkeBotidskrav(fhg)).isTrue();
    }

    @Test
    void skal_returnere_uten_botidskrav_hvis_bekreftet_termin_og_fødsel_før_ikrafttredelsedato() {
        // Arrange
        var bekreftettermindato = IKRAFT.plusWeeks(10);
        var fødselsdato = IKRAFT.minusWeeks(1);

        var førstegangScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        var terminbekreftelse = førstegangScenario.medBekreftetHendelse().getTerminbekreftelseBuilder()
            .medTermindato(bekreftettermindato)
            .medNavnPå("LEGEN LEGESEN")
            .medUtstedtDato(bekreftettermindato.minusMonths(2));
        førstegangScenario.medBekreftetHendelse().medFødselsDato(fødselsdato, 1).medTerminbekreftelse(terminbekreftelse);
        var mockprovider = førstegangScenario.mockBehandlingRepositoryProvider();
        var behandling = førstegangScenario.lagMocked();

        // Act/Assert
        var fhg = mockprovider.getFamilieHendelseRepository().hentAggregat(behandling.getId());
        assertThat(new BotidCore2024(IKRAFT, OVERGANG).ikkeBotidskrav(fhg)).isTrue();
    }

    @Test
    void skal_returnere_uten_botidskrav_hvis_bekreftet_fødsel_før_ikrafttredelsedato_uten_termin() {
        // Arrange
        var fødselsdato = IKRAFT.minusWeeks(1);

        var førstegangScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        førstegangScenario.medBekreftetHendelse().medFødselsDato(fødselsdato, 1);
        var mockprovider = førstegangScenario.mockBehandlingRepositoryProvider();
        var behandling = førstegangScenario.lagMocked();

        // Act/Assert
        var fhg = mockprovider.getFamilieHendelseRepository().hentAggregat(behandling.getId());
        assertThat(new BotidCore2024(IKRAFT, OVERGANG).ikkeBotidskrav(fhg)).isTrue();
    }

    @Test
    void skal_returnere_uten_botidskrav_hvis_bekreftet_adopsjon_før_ikrafttredelsedato() {
        // Arrange
        var omsorgsdato = IKRAFT.minusWeeks(1);

        var førstegangScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        var adopsjon = førstegangScenario.medBekreftetHendelse().getAdopsjonBuilder()
            .medOmsorgsovertakelseDato(omsorgsdato)
            .medAdoptererAlene(false).medErEktefellesBarn(false);
        førstegangScenario.medBekreftetHendelse().medFødselsDato(IKRAFT.minusYears(2), 1)
            .medAdopsjon(adopsjon);
        var mockprovider = førstegangScenario.mockBehandlingRepositoryProvider();
        var behandling = førstegangScenario.lagMocked();

        // Act/Assert
        var fhg = mockprovider.getFamilieHendelseRepository().hentAggregat(behandling.getId());
        assertThat(new BotidCore2024(IKRAFT, OVERGANG).ikkeBotidskrav(fhg)).isTrue();
    }

    @Test
    void skal_returnere_botidskrav_hvis_bekreftet_termin_etter_ikrafttredelsedato() {
        // Arrange
        var bekreftettermindato = IKRAFT.plus(OVERGANG).plusWeeks(1);

        var førstegangScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        var terminbekreftelse = førstegangScenario.medBekreftetHendelse().getTerminbekreftelseBuilder()
            .medTermindato(bekreftettermindato)
            .medNavnPå("LEGEN LEGESEN")
            .medUtstedtDato(bekreftettermindato.minusMonths(2));
        førstegangScenario.medBekreftetHendelse().medTerminbekreftelse(terminbekreftelse);
        var mockprovider = førstegangScenario.mockBehandlingRepositoryProvider();
        var behandling = førstegangScenario.lagMocked();

        // Act/Assert
        var fhg = mockprovider.getFamilieHendelseRepository().hentAggregat(behandling.getId());
        assertThat(new BotidCore2024(IKRAFT, OVERGANG).ikkeBotidskrav(fhg)).isFalse();
    }

    @Test
    void skal_returnere_botidskrav_hvis_bekreftet_termin_og_fødsel_etter_ikrafttredelsedato() {
        // Arrange
        var bekreftettermindato = IKRAFT.plus(OVERGANG).plusWeeks(1);
        var fødselsdato = IKRAFT.plus(OVERGANG);

        var førstegangScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        var terminbekreftelse = førstegangScenario.medBekreftetHendelse().getTerminbekreftelseBuilder()
            .medTermindato(bekreftettermindato)
            .medNavnPå("LEGEN LEGESEN")
            .medUtstedtDato(bekreftettermindato.minusMonths(2));
        førstegangScenario.medBekreftetHendelse().medFødselsDato(fødselsdato, 1).medTerminbekreftelse(terminbekreftelse);
        var mockprovider = førstegangScenario.mockBehandlingRepositoryProvider();
        var behandling = førstegangScenario.lagMocked();

        // Act/Assert
        var fhg = mockprovider.getFamilieHendelseRepository().hentAggregat(behandling.getId());
        assertThat(new BotidCore2024(IKRAFT, OVERGANG).ikkeBotidskrav(fhg)).isFalse();
    }

    @Test
    void skal_returnere_botidskrav_hvis_bekreftet_termin_etter_ikrafttredelsedato_fødsel_før() {
        // Arrange
        var bekreftettermindato = IKRAFT.plus(OVERGANG).plusWeeks(1);
        var fødselsdato = IKRAFT.minusDays(2);

        var førstegangScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        var terminbekreftelse = førstegangScenario.medBekreftetHendelse().getTerminbekreftelseBuilder()
            .medTermindato(bekreftettermindato)
            .medNavnPå("LEGEN LEGESEN")
            .medUtstedtDato(bekreftettermindato.minusMonths(2));
        førstegangScenario.medBekreftetHendelse().medFødselsDato(fødselsdato, 1).medTerminbekreftelse(terminbekreftelse);
        var mockprovider = førstegangScenario.mockBehandlingRepositoryProvider();
        var behandling = førstegangScenario.lagMocked();

        // Act/Assert
        var fhg = mockprovider.getFamilieHendelseRepository().hentAggregat(behandling.getId());
        assertThat(new BotidCore2024(IKRAFT, OVERGANG).ikkeBotidskrav(fhg)).isFalse();
    }

    @Test
    void skal_returnere_botidskrav_hvis_bekreftet_fødsel_etter_ikrafttredelsedato_uten_termin() {
        // Arrange
        var fødselsdato = IKRAFT.plusWeeks(2);

        var førstegangScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        førstegangScenario.medBekreftetHendelse().medFødselsDato(fødselsdato, 1);
        var mockprovider = førstegangScenario.mockBehandlingRepositoryProvider();
        var behandling = førstegangScenario.lagMocked();

        // Act/Assert
        var fhg = mockprovider.getFamilieHendelseRepository().hentAggregat(behandling.getId());
        assertThat(new BotidCore2024(IKRAFT, OVERGANG).ikkeBotidskrav(fhg)).isFalse();
    }

    @Test
    void skal_returnere_botidskrav_hvis_bekreftet_adopsjon_etter_ikrafttredelsedato() {
        // Arrange
        var omsorgsdato = IKRAFT.plus(OVERGANG).plusWeeks(1);

        var førstegangScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        var adopsjon = førstegangScenario.medBekreftetHendelse().getAdopsjonBuilder()
            .medOmsorgsovertakelseDato(omsorgsdato)
            .medAdoptererAlene(false).medErEktefellesBarn(false);
        førstegangScenario.medBekreftetHendelse().medFødselsDato(IKRAFT.minusYears(2), 1)
            .medAdopsjon(adopsjon);
        var mockprovider = førstegangScenario.mockBehandlingRepositoryProvider();
        var behandling = førstegangScenario.lagMocked();

        // Act/Assert
        var fhg = mockprovider.getFamilieHendelseRepository().hentAggregat(behandling.getId());
        assertThat(new BotidCore2024(IKRAFT, OVERGANG).ikkeBotidskrav(fhg)).isFalse();
    }

    // Søknadshendelse
    @Test
    void skal_returnere_uten_botidskrav_hvis_gjeldende_termin_før_ikrafttredelsedato() {
        // Arrange
        var bekreftettermindato = IKRAFT.plusWeeks(10);

        var førstegangScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        var terminbekreftelse = førstegangScenario.medSøknadHendelse().getTerminbekreftelseBuilder()
            .medTermindato(bekreftettermindato)
            .medNavnPå("LEGEN LEGESEN")
            .medUtstedtDato(bekreftettermindato.minusMonths(2));
        førstegangScenario.medSøknadHendelse().medTerminbekreftelse(terminbekreftelse);
        var mockprovider = førstegangScenario.mockBehandlingRepositoryProvider();
        var behandling = førstegangScenario.lagMocked();

        // Act/Assert
        var fhg = mockprovider.getFamilieHendelseRepository().hentAggregat(behandling.getId());
        assertThat(new BotidCore2024(IKRAFT, OVERGANG).ikkeBotidskrav(fhg)).isTrue();
    }

    @Test
    void skal_returnere_uten_botidskrav_hvis_gjeldende_termin_og_fødsel_før_ikrafttredelsedato() {
        // Arrange
        var bekreftettermindato = IKRAFT.plusWeeks(10);
        var fødselsdato = IKRAFT.minusWeeks(1);

        var førstegangScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        var terminbekreftelse = førstegangScenario.medSøknadHendelse().getTerminbekreftelseBuilder()
            .medTermindato(bekreftettermindato)
            .medNavnPå("LEGEN LEGESEN")
            .medUtstedtDato(bekreftettermindato.minusMonths(2));
        førstegangScenario.medSøknadHendelse().medFødselsDato(fødselsdato, 1).medTerminbekreftelse(terminbekreftelse);
        var mockprovider = førstegangScenario.mockBehandlingRepositoryProvider();
        var behandling = førstegangScenario.lagMocked();

        // Act/Assert
        var fhg = mockprovider.getFamilieHendelseRepository().hentAggregat(behandling.getId());
        assertThat(new BotidCore2024(IKRAFT, OVERGANG).ikkeBotidskrav(fhg)).isTrue();
    }

    @Test
    void skal_returnere_uten_botidskrav_hvis_gjeldende_fødsel_før_ikrafttredelsedato_uten_termin() {
        // Arrange
        var fødselsdato = IKRAFT.minusWeeks(1);

        var førstegangScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        førstegangScenario.medSøknadHendelse().medFødselsDato(fødselsdato, 1);
        var mockprovider = førstegangScenario.mockBehandlingRepositoryProvider();
        var behandling = førstegangScenario.lagMocked();

        // Act/Assert
        var fhg = mockprovider.getFamilieHendelseRepository().hentAggregat(behandling.getId());
        assertThat(new BotidCore2024(IKRAFT, OVERGANG).ikkeBotidskrav(fhg)).isTrue();
    }

    @Test
    void skal_returnere_uten_botidskrav_hvis_gjeldende_adopsjon_før_ikrafttredelsedato() {
        // Arrange
        var omsorgsdato = IKRAFT.minusWeeks(1);

        var førstegangScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        var adopsjon = førstegangScenario.medSøknadHendelse().getAdopsjonBuilder()
            .medOmsorgsovertakelseDato(omsorgsdato)
            .medAdoptererAlene(false).medErEktefellesBarn(false);
        førstegangScenario.medSøknadHendelse().medFødselsDato(IKRAFT.minusYears(2), 1)
            .medAdopsjon(adopsjon);
        var mockprovider = førstegangScenario.mockBehandlingRepositoryProvider();
        var behandling = førstegangScenario.lagMocked();

        // Act/Assert
        var fhg = mockprovider.getFamilieHendelseRepository().hentAggregat(behandling.getId());
        assertThat(new BotidCore2024(IKRAFT, OVERGANG).ikkeBotidskrav(fhg)).isTrue();
    }

    @Test
    void skal_returnere_botidskrav_hvis_gjeldende_termin_etter_ikrafttredelsedato() {
        // Arrange
        var bekreftettermindato = IKRAFT.plus(OVERGANG).plusWeeks(1);

        var førstegangScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        var terminbekreftelse = førstegangScenario.medSøknadHendelse().getTerminbekreftelseBuilder()
            .medTermindato(bekreftettermindato)
            .medNavnPå("LEGEN LEGESEN")
            .medUtstedtDato(bekreftettermindato.minusMonths(2));
        førstegangScenario.medSøknadHendelse().medTerminbekreftelse(terminbekreftelse);
        var mockprovider = førstegangScenario.mockBehandlingRepositoryProvider();
        var behandling = førstegangScenario.lagMocked();

        // Act/Assert
        var fhg = mockprovider.getFamilieHendelseRepository().hentAggregat(behandling.getId());
        assertThat(new BotidCore2024(IKRAFT, OVERGANG).ikkeBotidskrav(fhg)).isFalse();
    }

    @Test
    void skal_returnere_botidskrav_hvis_gjeldende_termin_og_fødsel_etter_ikrafttredelsedato() {
        // Arrange
        var bekreftettermindato = IKRAFT.plus(OVERGANG).plusWeeks(1);
        var fødselsdato = IKRAFT.plus(OVERGANG);

        var førstegangScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        var terminbekreftelse = førstegangScenario.medSøknadHendelse().getTerminbekreftelseBuilder()
            .medTermindato(bekreftettermindato)
            .medNavnPå("LEGEN LEGESEN")
            .medUtstedtDato(bekreftettermindato.minusMonths(2));
        førstegangScenario.medSøknadHendelse().medFødselsDato(fødselsdato, 1).medTerminbekreftelse(terminbekreftelse);
        var mockprovider = førstegangScenario.mockBehandlingRepositoryProvider();
        var behandling = førstegangScenario.lagMocked();

        // Act/Assert
        var fhg = mockprovider.getFamilieHendelseRepository().hentAggregat(behandling.getId());
        assertThat(new BotidCore2024(IKRAFT, OVERGANG).ikkeBotidskrav(fhg)).isFalse();
    }

    @Test
    void skal_returnere_botidskrav_hvis_gjeldende_termin_etter_ikrafttredelsedato_fødsel_før() {
        // Arrange
        var bekreftettermindato = IKRAFT.plus(OVERGANG).plusWeeks(1);
        var fødselsdato = IKRAFT.minusDays(2);

        var førstegangScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        var terminbekreftelse = førstegangScenario.medSøknadHendelse().getTerminbekreftelseBuilder()
            .medTermindato(bekreftettermindato)
            .medNavnPå("LEGEN LEGESEN")
            .medUtstedtDato(bekreftettermindato.minusMonths(2));
        førstegangScenario.medSøknadHendelse().medFødselsDato(fødselsdato, 1).medTerminbekreftelse(terminbekreftelse);
        var mockprovider = førstegangScenario.mockBehandlingRepositoryProvider();
        var behandling = førstegangScenario.lagMocked();

        // Act/Assert
        var fhg = mockprovider.getFamilieHendelseRepository().hentAggregat(behandling.getId());
        assertThat(new BotidCore2024(IKRAFT, OVERGANG).ikkeBotidskrav(fhg)).isFalse();
    }

    @Test
    void skal_returnere_botidskrav_hvis_gjeldende_fødsel_etter_ikrafttredelsedato_uten_termin() {
        // Arrange
        var fødselsdato = IKRAFT.plusWeeks(2);

        var førstegangScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        førstegangScenario.medSøknadHendelse().medFødselsDato(fødselsdato, 1);
        var mockprovider = førstegangScenario.mockBehandlingRepositoryProvider();
        var behandling = førstegangScenario.lagMocked();

        // Act/Assert
        var fhg = mockprovider.getFamilieHendelseRepository().hentAggregat(behandling.getId());
        assertThat(new BotidCore2024(IKRAFT, OVERGANG).ikkeBotidskrav(fhg)).isFalse();
    }

    @Test
    void skal_returnere_botidskrav_hvis_gjeldende_adopsjon_etter_ikrafttredelsedato() {
        // Arrange
        var omsorgsdato = IKRAFT.plusWeeks(1);

        var førstegangScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        var adopsjon = førstegangScenario.medSøknadHendelse().getAdopsjonBuilder()
            .medOmsorgsovertakelseDato(omsorgsdato)
            .medAdoptererAlene(false).medErEktefellesBarn(false);
        førstegangScenario.medSøknadHendelse().medFødselsDato(IKRAFT.minusYears(2), 1)
            .medAdopsjon(adopsjon);
        var mockprovider = førstegangScenario.mockBehandlingRepositoryProvider();
        var behandling = førstegangScenario.lagMocked();

        // Act/Assert
        var fhg = mockprovider.getFamilieHendelseRepository().hentAggregat(behandling.getId());
        assertThat(new BotidCore2024(IKRAFT, OVERGANG).ikkeBotidskrav(fhg)).isFalse();
    }


}
