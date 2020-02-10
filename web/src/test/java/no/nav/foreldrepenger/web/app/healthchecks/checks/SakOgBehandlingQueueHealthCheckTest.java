package no.nav.foreldrepenger.web.app.healthchecks.checks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.Test;

import no.nav.vedtak.felles.integrasjon.sakogbehandling.SakOgBehandlingClient;

public class SakOgBehandlingQueueHealthCheckTest {

    @Test
    public void test_alt() {
        SakOgBehandlingClient mockClient = mock(SakOgBehandlingClient.class);
        SakOgBehandlingQueueHealthCheck check = new SakOgBehandlingQueueHealthCheck(mockClient);

        assertThat(check.getDescriptionSuffix()).isNotNull();

        new SakOgBehandlingQueueHealthCheck(); // som CDI trenger
    }
}
