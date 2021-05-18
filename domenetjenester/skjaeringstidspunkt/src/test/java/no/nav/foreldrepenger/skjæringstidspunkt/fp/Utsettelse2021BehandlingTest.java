package no.nav.foreldrepenger.skjæringstidspunkt.fp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.skjæringstidspunkt.UtsettelseBehandling2021;
import no.nav.foreldrepenger.skjæringstidspunkt.UtsettelseCore2021;

public class Utsettelse2021BehandlingTest {

    @Test
    public void skal_returnere_sammenhengende_uttak_hvis_bekreftet_hendelse_før_dato() {
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
        assertThat(new UtsettelseBehandling2021(new UtsettelseCore2021(ikraftredelse), mockprovider).kreverSammenhengendeUttak(behandling.getId())).isTrue();
    }

    @Test
    public void skal_returnere_fritt_uttak_hvis_bekreftet_hendelse_etter_dato() {
        // Arrange
        var ikraftredelse = LocalDate.of(2021, 10, 1);
        var skjæringsdato = ikraftredelse;
        var bekreftetfødselsdato = skjæringsdato.plusWeeks(3);

        var førstegangScenario = ScenarioMorSøkerForeldrepenger.forAdopsjon()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        førstegangScenario.medBekreftetHendelse()
            .medAdopsjon(førstegangScenario.medBekreftetHendelse().getAdopsjonBuilder().medOmsorgsovertakelseDato(bekreftetfødselsdato));
        var mockprovider = førstegangScenario.mockBehandlingRepositoryProvider();
        var behandling = førstegangScenario.lagMocked();

        // Act/Assert
        assertThat(new UtsettelseBehandling2021(new UtsettelseCore2021(ikraftredelse), mockprovider).kreverSammenhengendeUttak(behandling.getId())).isFalse();
    }

    @Test
    public void skal_returnere_fritt_uttak_hvis_bekreftet_termin_20_dager_etter() {
        // Arrange
        var ikraftredelse = LocalDate.now().minusDays(20);
        var skjæringsdato = ikraftredelse;
        var bekreftetfødselsdato = skjæringsdato.plusWeeks(3);

        var førstegangScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        førstegangScenario.medBekreftetHendelse()
            .medTerminbekreftelse(førstegangScenario.medBekreftetHendelse().getTerminbekreftelseBuilder()
                .medTermindato(bekreftetfødselsdato));
        var mockprovider = førstegangScenario.mockBehandlingRepositoryProvider();
        var behandling = førstegangScenario.lagMocked();

        // Act/Assert
        assertThat(new UtsettelseBehandling2021(new UtsettelseCore2021(ikraftredelse), mockprovider).kreverSammenhengendeUttak(behandling.getId())).isFalse();
    }

    @Test
    public void skal_returnere_sammenhengende_uttak_hvis_bekreftet_termin_2_dager_etter() {
        // Arrange
        var ikraftredelse = LocalDate.now().minusDays(2);
        var skjæringsdato = ikraftredelse;
        var bekreftetfødselsdato = skjæringsdato.plusWeeks(3);

        var førstegangScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        førstegangScenario.medBekreftetHendelse()
            .medTerminbekreftelse(førstegangScenario.medBekreftetHendelse().getTerminbekreftelseBuilder()
                .medTermindato(bekreftetfødselsdato));
        var mockprovider = førstegangScenario.mockBehandlingRepositoryProvider();
        var behandling = førstegangScenario.lagMocked();

        // Act/Assert
        assertThat(new UtsettelseBehandling2021(new UtsettelseCore2021(ikraftredelse), mockprovider).kreverSammenhengendeUttak(behandling.getId())).isTrue();
    }

    @Test
    public void skal_returnere_fritt_uttak_hvis_søkt_adopsjon_2_dager_etter() {
        // Arrange
        var ikraftredelse = LocalDate.now().minusDays(2);
        var skjæringsdato = ikraftredelse;
        var bekreftetfødselsdato = skjæringsdato.plusWeeks(3);

        var førstegangScenario = ScenarioMorSøkerForeldrepenger.forAdopsjon()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        førstegangScenario.medSøknadHendelse()
            .medAdopsjon(førstegangScenario.medSøknadHendelse().getAdopsjonBuilder()
            .medOmsorgsovertakelseDato(bekreftetfødselsdato));
        var mockprovider = førstegangScenario.mockBehandlingRepositoryProvider();
        var behandling = førstegangScenario.lagMocked();

        // Act/Assert
        assertThat(new UtsettelseBehandling2021(new UtsettelseCore2021(ikraftredelse), mockprovider).kreverSammenhengendeUttak(behandling.getId())).isFalse();
    }

    @Test
    public void skal_returnere_sammenhengende_uttak_hvis_søkt_fødsel_10_dager_etter() {
        // Arrange
        var ikraftredelse = LocalDate.now().minusDays(10);
        var skjæringsdato = ikraftredelse;
        var bekreftetfødselsdato = skjæringsdato.plusWeeks(3);

        var førstegangScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        førstegangScenario.medSøknadHendelse()
            .medFødselsDato(bekreftetfødselsdato);
        var mockprovider = førstegangScenario.mockBehandlingRepositoryProvider();
        var behandling = førstegangScenario.lagMocked();

        // Act/Assert
        assertThat(new UtsettelseBehandling2021(new UtsettelseCore2021(ikraftredelse), mockprovider).kreverSammenhengendeUttak(behandling.getId())).isTrue();
    }

    @Test
    public void skal_returnere_fritt_uttak_hvis_søkt_termin_30_dager_etter() {
        // Arrange
        var ikraftredelse = LocalDate.now().minusDays(30);
        var skjæringsdato = ikraftredelse;
        var bekreftetfødselsdato = skjæringsdato.plusWeeks(3);

        var førstegangScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        førstegangScenario.medSøknadHendelse()
            .medTerminbekreftelse(førstegangScenario.medSøknadHendelse().getTerminbekreftelseBuilder()
                .medTermindato(bekreftetfødselsdato));
        var mockprovider = førstegangScenario.mockBehandlingRepositoryProvider();
        var behandling = førstegangScenario.lagMocked();
        // Act/Assert
        assertThat(new UtsettelseBehandling2021(new UtsettelseCore2021(ikraftredelse), mockprovider).kreverSammenhengendeUttak(behandling.getId())).isFalse();
    }

    @Test
    public void skal_returnere_sammenhengende_uttak_pga_medforelder() {
        // Arrange
        var ikraftredelse = LocalDate.of(2021, 10, 1);
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

        mockprovider.getFagsakRelasjonRepository().opprettRelasjon(behandling.getFagsak(), Dekningsgrad._100);
        mockprovider.getFagsakRelasjonRepository().opprettRelasjon(behandlingFar.getFagsak(), Dekningsgrad._100);
        mockprovider.getFagsakRelasjonRepository().kobleFagsaker(behandling.getFagsak(), behandlingFar.getFagsak(), behandling);
        when(behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(behandling.getFagsakId())).thenReturn(Optional.of(behandling));
        when(behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(behandlingFar.getFagsakId())).thenReturn(Optional.empty());

        var tjenesteCore = new UtsettelseCore2021(ikraftredelse);
        var tjeneste = new UtsettelseBehandling2021(tjenesteCore, mockprovider);

        // Act/Assert
        assertThat(tjeneste.kreverSammenhengendeUttak(behandlingFar.getId())).isTrue();
    }

    @Test
    public void skal_returnere_fritt_uttak_pga_medforelder() {
        // Arrange
        var ikraftredelse = LocalDate.now().minusDays(30);
        var skjæringsdato = ikraftredelse;
        var bekreftetfødselsdato = skjæringsdato.plusWeeks(3);

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

        mockprovider.getFagsakRelasjonRepository().opprettRelasjon(behandling.getFagsak(), Dekningsgrad._100);
        mockprovider.getFagsakRelasjonRepository().opprettRelasjon(behandlingFar.getFagsak(), Dekningsgrad._100);
        mockprovider.getFagsakRelasjonRepository().kobleFagsaker(behandling.getFagsak(), behandlingFar.getFagsak(), behandling);
        when(behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(behandling.getFagsakId())).thenReturn(Optional.of(behandling));
        when(behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(behandlingFar.getFagsakId())).thenReturn(Optional.empty());


        var tjenesteCore = new UtsettelseCore2021(ikraftredelse);
        var tjeneste = new UtsettelseBehandling2021(tjenesteCore, mockprovider);

        // Act/Assert
        assertThat(tjeneste.kreverSammenhengendeUttak(behandlingFar.getId())).isFalse();
    }

}
