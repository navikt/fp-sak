package no.nav.foreldrepenger.skjæringstidspunkt.overganger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;

class Minsterett2022BehandlingTest {

    @Test
    void skal_returnere_uten_minsterett_uttak_hvis_bekreftet_hendelse_før_dato() {
        // Arrange
        var ikraftredelse = MinsterettCore2022.IKRAFT_FRA_DATO;
        var skjæringsdato = ikraftredelse.minusWeeks(4);
        var bekreftetfødselsdato = skjæringsdato.plusWeeks(3);

        var førstegangScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        førstegangScenario.medBekreftetHendelse().medFødselsDato(bekreftetfødselsdato);
        var mockprovider = førstegangScenario.mockBehandlingRepositoryProvider();
        var behandling = førstegangScenario.lagMocked();

        // Act/Assert
        var fagsakRelasjonTjeneste = new FagsakRelasjonTjeneste(mockprovider);
        assertThat(new MinsterettBehandling2022(mockprovider, fagsakRelasjonTjeneste).utenMinsterett(behandling.getId())).isTrue();
    }

    @Test
    void skal_returnere_minsterett_hvis_bekreftet_hendelse_etter_dato() {
        // Arrange
        var ikraftredelse = MinsterettCore2022.IKRAFT_FRA_DATO;
        var bekreftetfødselsdato = ikraftredelse.plusWeeks(3);

        var førstegangScenario = ScenarioMorSøkerForeldrepenger.forAdopsjon()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        førstegangScenario.medBekreftetHendelse()
            .medAdopsjon(førstegangScenario.medBekreftetHendelse().getAdopsjonBuilder().medOmsorgsovertakelseDato(bekreftetfødselsdato));
        var mockprovider = førstegangScenario.mockBehandlingRepositoryProvider();
        var behandling = førstegangScenario.lagMocked();

        // Act/Assert
        var fagsakRelasjonTjeneste = new FagsakRelasjonTjeneste(mockprovider);
        assertThat(new MinsterettBehandling2022(mockprovider, fagsakRelasjonTjeneste).utenMinsterett(behandling.getId())).isFalse();
    }

    @Test
    void skal_returnere_minsterett_hvis_bekreftet_termin_20_dager_etter() {
        // Arrange
        var ikraftredelse = MinsterettCore2022.IKRAFT_FRA_DATO;
        var bekreftetfødselsdato = ikraftredelse.plusWeeks(3);

        var førstegangScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        førstegangScenario.medBekreftetHendelse()
            .medTerminbekreftelse(førstegangScenario.medBekreftetHendelse().getTerminbekreftelseBuilder()
                .medTermindato(bekreftetfødselsdato));
        var mockprovider = førstegangScenario.mockBehandlingRepositoryProvider();
        var behandling = førstegangScenario.lagMocked();

        // Act/Assert
        var fagsakRelasjonTjeneste = new FagsakRelasjonTjeneste(mockprovider);
        assertThat(new MinsterettBehandling2022(mockprovider, fagsakRelasjonTjeneste).utenMinsterett(behandling.getId())).isFalse();
    }

    @Test
    void skal_returnere_minsterett_hvis_bekreftet_termin_2_dager_etter() {
        // Arrange
        var ikraftredelse = MinsterettCore2022.IKRAFT_FRA_DATO;
        var bekreftetfødselsdato = ikraftredelse.plusWeeks(3);

        var førstegangScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        førstegangScenario.medBekreftetHendelse()
            .medTerminbekreftelse(førstegangScenario.medBekreftetHendelse().getTerminbekreftelseBuilder()
                .medTermindato(bekreftetfødselsdato));
        var mockprovider = førstegangScenario.mockBehandlingRepositoryProvider();
        var behandling = førstegangScenario.lagMocked();

        // Act/Assert
        var fagsakRelasjonTjeneste = new FagsakRelasjonTjeneste(mockprovider);
        assertThat(new MinsterettBehandling2022(mockprovider, fagsakRelasjonTjeneste).utenMinsterett(behandling.getId())).isFalse();
    }

    @Test
    void skal_returnere_uten_minsterett_uttak_hvis_bekreftet_termin_2_dager_før() {
        // Arrange
        var ikraftredelse = MinsterettCore2022.IKRAFT_FRA_DATO;
        var bekreftetfødselsdato = ikraftredelse.minusDays(2);

        var førstegangScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        førstegangScenario.medBekreftetHendelse()
            .medTerminbekreftelse(førstegangScenario.medBekreftetHendelse().getTerminbekreftelseBuilder()
                .medTermindato(bekreftetfødselsdato));
        var mockprovider = førstegangScenario.mockBehandlingRepositoryProvider();
        var behandling = førstegangScenario.lagMocked();

        // Act/Assert
        var fagsakRelasjonTjeneste = new FagsakRelasjonTjeneste(mockprovider);
        assertThat(new MinsterettBehandling2022(mockprovider, fagsakRelasjonTjeneste).utenMinsterett(behandling.getId())).isTrue();
    }

    @Test
    void skal_returnere_minsterett_hvis_søkt_adopsjon_2_dager_etter() {
        // Arrange
        var ikraftredelse = MinsterettCore2022.IKRAFT_FRA_DATO;
        var bekreftetfødselsdato = ikraftredelse.plusWeeks(3);

        var førstegangScenario = ScenarioMorSøkerForeldrepenger.forAdopsjon()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        førstegangScenario.medSøknadHendelse()
            .medAdopsjon(førstegangScenario.medSøknadHendelse().getAdopsjonBuilder()
            .medOmsorgsovertakelseDato(bekreftetfødselsdato));
        var mockprovider = førstegangScenario.mockBehandlingRepositoryProvider();
        var behandling = førstegangScenario.lagMocked();

        // Act/Assert
        var fagsakRelasjonTjeneste = new FagsakRelasjonTjeneste(mockprovider);
        assertThat(new MinsterettBehandling2022(mockprovider, fagsakRelasjonTjeneste).utenMinsterett(behandling.getId())).isFalse();
    }

    @Test
    void skal_returnere_minsterett_hvis_søkt_fødsel_10_dager_etter() {
        // Arrange
        var ikraftredelse = MinsterettCore2022.IKRAFT_FRA_DATO;
        var bekreftetfødselsdato = ikraftredelse.plusWeeks(3);

        var førstegangScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        førstegangScenario.medSøknadHendelse()
            .medFødselsDato(bekreftetfødselsdato);
        var mockprovider = førstegangScenario.mockBehandlingRepositoryProvider();
        var behandling = førstegangScenario.lagMocked();

        // Act/Assert
        var fagsakRelasjonTjeneste = new FagsakRelasjonTjeneste(mockprovider);
        assertThat(new MinsterettBehandling2022(mockprovider, fagsakRelasjonTjeneste).utenMinsterett(behandling.getId())).isFalse();
    }

    @Test
    void skal_returnere_minsterett_hvis_søkt_termin_30_dager_etter() {
        // Arrange
        var ikraftredelse = MinsterettCore2022.IKRAFT_FRA_DATO;
        var bekreftetfødselsdato = ikraftredelse.plusWeeks(3);

        var førstegangScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        førstegangScenario.medSøknadHendelse()
            .medTerminbekreftelse(førstegangScenario.medSøknadHendelse().getTerminbekreftelseBuilder()
                .medTermindato(bekreftetfødselsdato));
        var mockprovider = førstegangScenario.mockBehandlingRepositoryProvider();
        var behandling = førstegangScenario.lagMocked();
        // Act/Assert
        var fagsakRelasjonTjeneste = new FagsakRelasjonTjeneste(mockprovider);
        assertThat(new MinsterettBehandling2022(mockprovider, fagsakRelasjonTjeneste).utenMinsterett(behandling.getId())).isFalse();
    }

    @Test
    void skal_returnere_uten_minsterett_uttak_pga_medforelder() {
        // Arrange
        var ikraftredelse = MinsterettCore2022.IKRAFT_FRA_DATO;
        var skjæringsdato = ikraftredelse.minusWeeks(4);
        var bekreftetfødselsdato = skjæringsdato.plusWeeks(3);

        var førstegangScenarioFar = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        var behandlingFar = førstegangScenarioFar.lagMocked();
        var førstegangScenarioMor = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        førstegangScenarioMor.medBekreftetHendelse().medFødselsDato(bekreftetfødselsdato);
        var mockprovider = førstegangScenarioMor.mockBehandlingRepositoryProvider();
        var behandlingRepository = førstegangScenarioMor.mockBehandlingRepository();
        var behandling = førstegangScenarioMor.lagMocked();
        behandling.avsluttBehandling();
        behandlingRepository.lagre(behandlingFar, Mockito.mock(BehandlingLås.class));

        mockprovider.getFagsakRelasjonRepository().opprettRelasjon(behandling.getFagsak());
        mockprovider.getFagsakRelasjonRepository().opprettRelasjon(behandlingFar.getFagsak());
        mockprovider.getFagsakRelasjonRepository().kobleFagsaker(behandling.getFagsak(), behandlingFar.getFagsak());
        when(behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(behandling.getFagsakId())).thenReturn(Optional.of(behandling));
        when(behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(behandlingFar.getFagsakId())).thenReturn(Optional.empty());

        var fagsakRelasjonTjeneste = new FagsakRelasjonTjeneste(mockprovider);
        var tjeneste = new MinsterettBehandling2022(mockprovider, fagsakRelasjonTjeneste);

        // Act/Assert
        assertThat(tjeneste.utenMinsterett(behandlingFar.getId())).isTrue();
    }

    @Test
    void skal_returnere_minsterett_pga_medforelder() {
        // Arrange
        var ikraftredelse = MinsterettCore2022.IKRAFT_FRA_DATO;
        var bekreftetfødselsdato = ikraftredelse.plusWeeks(3);

        var førstegangScenarioFar = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        var behandlingFar = førstegangScenarioFar.lagMocked();
        var førstegangScenarioMor = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        førstegangScenarioMor.medSøknadHendelse()
            .medTerminbekreftelse(førstegangScenarioMor.medSøknadHendelse().getTerminbekreftelseBuilder()
                .medTermindato(bekreftetfødselsdato));
        var mockprovider = førstegangScenarioMor.mockBehandlingRepositoryProvider();
        var behandlingRepository = førstegangScenarioMor.mockBehandlingRepository();
        var behandling = førstegangScenarioMor.lagMocked();
        behandling.avsluttBehandling();
        behandlingRepository.lagre(behandlingFar, Mockito.mock(BehandlingLås.class));

        var fagsakRelasjonTjeneste = new FagsakRelasjonTjeneste(mockprovider);
        fagsakRelasjonTjeneste.opprettRelasjon(behandling.getFagsak());
        fagsakRelasjonTjeneste.opprettRelasjon(behandlingFar.getFagsak());
        fagsakRelasjonTjeneste.kobleFagsaker(behandling.getFagsak(), behandlingFar.getFagsak());
        when(behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(behandling.getFagsakId())).thenReturn(Optional.of(behandling));
        when(behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(behandlingFar.getFagsakId())).thenReturn(Optional.empty());


        var tjeneste = new MinsterettBehandling2022(mockprovider, fagsakRelasjonTjeneste);

        // Act/Assert
        assertThat(tjeneste.utenMinsterett(behandlingFar.getId())).isFalse();
    }

}
