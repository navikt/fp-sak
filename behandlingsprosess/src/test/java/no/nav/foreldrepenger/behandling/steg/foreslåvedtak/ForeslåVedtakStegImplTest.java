package no.nav.foreldrepenger.behandling.steg.foreslåvedtak;


import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Test;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;

public class ForeslåVedtakStegImplTest {

    private ForeslåVedtakTjeneste foreslåVedtakTjeneste = mock(ForeslåVedtakTjeneste.class);

    @Test
    public void skalKalleTjeneste() {
        // Arrange
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        Behandling behandling = scenario.lagMocked();
        var behandlingRepository = scenario.mockBehandlingRepository();
        ForeslåVedtakStegImpl steg = new ForeslåVedtakStegImpl(behandlingRepository, foreslåVedtakTjeneste);

        // Act
        Fagsak fagsak = behandling.getFagsak();
        BehandlingLås behandlingLås = behandlingRepository.taSkriveLås(behandling);
        BehandlingskontrollKontekst kontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(), behandlingLås);
        steg.utførSteg(kontekst);

        // Assert
        verify(foreslåVedtakTjeneste).foreslåVedtak(eq(behandling), eq(kontekst));
    }
}
