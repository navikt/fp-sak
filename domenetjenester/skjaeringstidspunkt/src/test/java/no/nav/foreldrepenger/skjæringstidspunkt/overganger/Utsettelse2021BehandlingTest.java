package no.nav.foreldrepenger.skjæringstidspunkt.overganger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;

class Utsettelse2021BehandlingTest {

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
        var fagsakRelasjonTjeneste = new FagsakRelasjonTjeneste(mockprovider);
        assertThat(new UtsettelseBehandling2021(new UtsettelseCore2021(ikraftredelse), mockprovider, fagsakRelasjonTjeneste).kreverSammenhengendeUttak(behandling.getId())).isTrue();
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
        var fagsakRelasjonTjeneste = new FagsakRelasjonTjeneste(mockprovider);
        assertThat(new UtsettelseBehandling2021(new UtsettelseCore2021(ikraftredelse), mockprovider, fagsakRelasjonTjeneste).kreverSammenhengendeUttak(behandling.getId())).isFalse();
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
        var fagsakRelasjonTjeneste = new FagsakRelasjonTjeneste(mockprovider);
        assertThat(new UtsettelseBehandling2021(new UtsettelseCore2021(ikraftredelse), mockprovider, fagsakRelasjonTjeneste).kreverSammenhengendeUttak(behandling.getId())).isFalse();
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
        var fagsakRelasjonTjeneste = new FagsakRelasjonTjeneste(mockprovider);
        assertThat(new UtsettelseBehandling2021(new UtsettelseCore2021(ikraftredelse), mockprovider, fagsakRelasjonTjeneste).kreverSammenhengendeUttak(behandling.getId())).isFalse();
    }

    @Test
    void skal_returnere_sammenhengende_uttak_hvis_bekreftet_termin_2_dager_før() {
        // Arrange
        var ikraftredelse = LocalDate.now().plusDays(2);
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
        assertThat(new UtsettelseBehandling2021(new UtsettelseCore2021(ikraftredelse), mockprovider, fagsakRelasjonTjeneste).kreverSammenhengendeUttak(behandling.getId())).isTrue();
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
        var fagsakRelasjonTjeneste = new FagsakRelasjonTjeneste(mockprovider);
        assertThat(new UtsettelseBehandling2021(new UtsettelseCore2021(ikraftredelse), mockprovider, fagsakRelasjonTjeneste).kreverSammenhengendeUttak(behandling.getId())).isFalse();
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
        var fagsakRelasjonTjeneste = new FagsakRelasjonTjeneste(mockprovider);
        assertThat(new UtsettelseBehandling2021(new UtsettelseCore2021(ikraftredelse), mockprovider, fagsakRelasjonTjeneste).kreverSammenhengendeUttak(behandling.getId())).isFalse();
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
        var fagsakRelasjonTjeneste = new FagsakRelasjonTjeneste(mockprovider);
        assertThat(new UtsettelseBehandling2021(new UtsettelseCore2021(ikraftredelse), mockprovider, fagsakRelasjonTjeneste).kreverSammenhengendeUttak(behandling.getId())).isFalse();
    }

    @Test
    void skal_returnere_sammenhengende_uttak_pga_medforelder() {
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
        var fagsakRelasjonTjeneste = new FagsakRelasjonTjeneste(mockprovider);
        var tjeneste = new UtsettelseBehandling2021(tjenesteCore, mockprovider, fagsakRelasjonTjeneste);

        // Act/Assert
        assertThat(tjeneste.kreverSammenhengendeUttak(behandlingFar.getId())).isTrue();
    }

    @Test
    void skal_returnere_fritt_uttak_pga_medforelder() {
        // Arrange
        var ikraftredelse = LocalDate.now().minusDays(30);
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

        mockprovider.getFagsakRelasjonRepository().opprettRelasjon(behandling.getFagsak(), Dekningsgrad._100);
        mockprovider.getFagsakRelasjonRepository().opprettRelasjon(behandlingFar.getFagsak(), Dekningsgrad._100);
        mockprovider.getFagsakRelasjonRepository().kobleFagsaker(behandling.getFagsak(), behandlingFar.getFagsak(), behandling);
        when(behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(behandling.getFagsakId())).thenReturn(Optional.of(behandling));
        when(behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(behandlingFar.getFagsakId())).thenReturn(Optional.empty());


        var tjenesteCore = new UtsettelseCore2021(ikraftredelse);
        var fagsakRelasjonTjeneste = new FagsakRelasjonTjeneste(mockprovider);
        var tjeneste = new UtsettelseBehandling2021(tjenesteCore, mockprovider, fagsakRelasjonTjeneste);

        // Act/Assert
        assertThat(tjeneste.kreverSammenhengendeUttak(behandlingFar.getId())).isFalse();
    }

    @Test
    void skal_gi_endret_regler_ved_søkt_termin_etter_og_bekreftet_fødsel_før_ikrafttredelse() {
        var ikraftredelse = LocalDate.now();
        var bekreftetfødselsdato = ikraftredelse.minusWeeks(1);

        var førstegangScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        førstegangScenario.medSøknadHendelse()
            .medTerminbekreftelse(førstegangScenario.medSøknadHendelse().getTerminbekreftelseBuilder()
                .medTermindato(ikraftredelse.plusWeeks(3)));
        var mockprovider = førstegangScenario.mockBehandlingRepositoryProvider();
        var behandling = førstegangScenario.lagMocked();

        // Act/Assert
        var fagsakRelasjonTjeneste = new FagsakRelasjonTjeneste(mockprovider);
        var tjeneste = new UtsettelseBehandling2021(new UtsettelseCore2021(ikraftredelse), mockprovider, fagsakRelasjonTjeneste);
        assertThat(tjeneste.kreverSammenhengendeUttak(behandling.getId())).isFalse();

        var revurderingScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.REVURDERING);
        revurderingScenario.medOriginalBehandling(behandling, BehandlingÅrsakType.RE_MANGLER_FØDSEL);
        førstegangScenario.medSøknadHendelse()
            .medTerminbekreftelse(førstegangScenario.medSøknadHendelse().getTerminbekreftelseBuilder()
                .medTermindato(ikraftredelse.plusWeeks(3)));
        revurderingScenario.medBekreftetHendelse().leggTilBarn(bekreftetfødselsdato);
        var revurdering = revurderingScenario.lagre(mockprovider);

        var g1 = mockprovider.getFamilieHendelseRepository().hentAggregat(behandling.getId());
        var g2 = mockprovider.getFamilieHendelseRepository().hentAggregat(revurdering.getId());

        var skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(bekreftetfødselsdato).build();

        assertThat(tjeneste.endringAvSammenhengendeUttak(BehandlingReferanse.fra(revurdering, skjæringstidspunkt), g1, g2)).isTrue();
    }

    @Test
    void skal_gi_uendret_regler_ved_søkt_termin_etter_og_bekreftet_fødsel_etter_ikrafttredelse() {
        var ikraftredelse = LocalDate.now();
        var bekreftetfødselsdato = ikraftredelse.plusWeeks(2);

        var førstegangScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        førstegangScenario.medSøknadHendelse()
            .medTerminbekreftelse(førstegangScenario.medSøknadHendelse().getTerminbekreftelseBuilder()
                .medTermindato(ikraftredelse.plusWeeks(3)));
        var mockprovider = førstegangScenario.mockBehandlingRepositoryProvider();
        var behandling = førstegangScenario.lagMocked();

        // Act/Assert
        var fagsakRelasjonTjeneste = new FagsakRelasjonTjeneste(mockprovider);
        var tjeneste = new UtsettelseBehandling2021(new UtsettelseCore2021(ikraftredelse), mockprovider, fagsakRelasjonTjeneste);
        assertThat(tjeneste.kreverSammenhengendeUttak(behandling.getId())).isFalse();

        var revurderingScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.REVURDERING);
        revurderingScenario.medOriginalBehandling(behandling, BehandlingÅrsakType.RE_MANGLER_FØDSEL);
        førstegangScenario.medSøknadHendelse()
            .medTerminbekreftelse(førstegangScenario.medSøknadHendelse().getTerminbekreftelseBuilder()
                .medTermindato(ikraftredelse.plusWeeks(3)));
        revurderingScenario.medBekreftetHendelse().leggTilBarn(bekreftetfødselsdato);
        var revurdering = revurderingScenario.lagre(mockprovider);

        var g1 = mockprovider.getFamilieHendelseRepository().hentAggregat(behandling.getId());
        var g2 = mockprovider.getFamilieHendelseRepository().hentAggregat(revurdering.getId());

        var skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(bekreftetfødselsdato).build();

        assertThat(tjeneste.endringAvSammenhengendeUttak(BehandlingReferanse.fra(revurdering, skjæringstidspunkt), g1, g2)).isFalse();
    }

    @Test
    void skal_gi_uendret_regler_ved_søkt_termin_før_og_bekreftet_fødsel_før_ikrafttredelse() {
        var ikraftredelse = LocalDate.now();
        var skjæringsdato = ikraftredelse.minusWeeks(4);
        var bekreftetfødselsdato = skjæringsdato.plusWeeks(2);

        var førstegangScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        førstegangScenario.medSøknadHendelse()
            .medTerminbekreftelse(førstegangScenario.medSøknadHendelse().getTerminbekreftelseBuilder()
                .medTermindato(skjæringsdato.plusWeeks(3)));
        var mockprovider = førstegangScenario.mockBehandlingRepositoryProvider();
        var behandling = førstegangScenario.lagMocked();

        // Act/Assert
        var fagsakRelasjonTjeneste = new FagsakRelasjonTjeneste(mockprovider);
        var tjeneste = new UtsettelseBehandling2021(new UtsettelseCore2021(ikraftredelse), mockprovider, fagsakRelasjonTjeneste);
        assertThat(tjeneste.kreverSammenhengendeUttak(behandling.getId())).isTrue();

        var revurderingScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.REVURDERING);
        revurderingScenario.medOriginalBehandling(behandling, BehandlingÅrsakType.RE_MANGLER_FØDSEL);
        førstegangScenario.medSøknadHendelse()
            .medTerminbekreftelse(førstegangScenario.medSøknadHendelse().getTerminbekreftelseBuilder()
                .medTermindato(skjæringsdato.plusWeeks(3)));
        revurderingScenario.medBekreftetHendelse().leggTilBarn(bekreftetfødselsdato);
        var revurdering = revurderingScenario.lagre(mockprovider);

        var g1 = mockprovider.getFamilieHendelseRepository().hentAggregat(behandling.getId());
        var g2 = mockprovider.getFamilieHendelseRepository().hentAggregat(revurdering.getId());

        var skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(bekreftetfødselsdato).build();

        assertThat(tjeneste.endringAvSammenhengendeUttak(BehandlingReferanse.fra(revurdering, skjæringstidspunkt), g1, g2)).isFalse();
    }

}
