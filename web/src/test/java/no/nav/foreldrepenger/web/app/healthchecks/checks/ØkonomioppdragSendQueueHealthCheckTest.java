package no.nav.foreldrepenger.web.app.healthchecks.checks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.Test;

import no.nav.foreldrepenger.økonomi.økonomistøtte.queue.producer.ØkonomioppdragJmsProducer;


public class ØkonomioppdragSendQueueHealthCheckTest {

    @Test
    public void skalRetunererKøNavn() {
        ØkonomioppdragJmsProducer økonomioppdragJmsProducer = mock(ØkonomioppdragJmsProducer.class);

        ØkonomioppdragSendQueueHealthCheck økonomioppdragSendQueueHealthCheck = new ØkonomioppdragSendQueueHealthCheck(økonomioppdragJmsProducer);

        assertThat(økonomioppdragSendQueueHealthCheck.getDescriptionSuffix()).isEqualTo("Økonomioppdrag Send");
    }
}
