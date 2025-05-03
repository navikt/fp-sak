package no.nav.foreldrepenger.behandling.steg.foreslåvedtak;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;

class ForeslåVedtakStegImplTest {

    private final ForeslåVedtakTjeneste foreslåVedtakTjeneste = mock(ForeslåVedtakTjeneste.class);

    @Test
    void skalKalleTjeneste() {
        // Arrange
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var behandling = scenario.lagMocked();
        var behandlingRepository = scenario.mockBehandlingRepository();
        var steg = new ForeslåVedtakStegImpl(behandlingRepository, foreslåVedtakTjeneste);

        // Act
        var behandlingLås = behandlingRepository.taSkriveLås(behandling);
        var kontekst = new BehandlingskontrollKontekst(behandling, behandlingLås);
        steg.utførSteg(kontekst);

        // Assert
        verify(foreslåVedtakTjeneste).foreslåVedtak(behandling);
    }
}
