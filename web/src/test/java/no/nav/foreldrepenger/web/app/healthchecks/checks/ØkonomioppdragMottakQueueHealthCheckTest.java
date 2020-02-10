package no.nav.foreldrepenger.web.app.healthchecks.checks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.Test;

import no.nav.vedtak.felles.integrasjon.okonomistottejms.consumer.ØkonomioppdragAsyncJmsConsumer;

public class ØkonomioppdragMottakQueueHealthCheckTest {

    @Test
    public void skalRetunererKøNavn() {
        ØkonomioppdragAsyncJmsConsumer økonomioppdragAsyncJmsConsumer = mock(ØkonomioppdragAsyncJmsConsumer.class);

        ØkonomioppdragMottakQueueHealthCheck økonomioppdragMottakQueueHealthCheck = new ØkonomioppdragMottakQueueHealthCheck(økonomioppdragAsyncJmsConsumer);

        assertThat(økonomioppdragMottakQueueHealthCheck.getDescriptionSuffix()).isEqualTo("Økonomioppdrag Mottak");
    }
}
