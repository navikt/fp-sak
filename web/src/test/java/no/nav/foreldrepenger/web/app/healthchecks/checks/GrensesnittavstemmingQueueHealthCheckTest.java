package no.nav.foreldrepenger.web.app.healthchecks.checks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.økonomi.grensesnittavstemming.queue.producer.GrensesnittavstemmingJmsProducer;

public class GrensesnittavstemmingQueueHealthCheckTest {

    @Test
    public void skalRetunererKøNavn() {
        GrensesnittavstemmingJmsProducer grensesnittavstemmingJmsProducer = mock(GrensesnittavstemmingJmsProducer.class);

        GrensesnittavstemmingQueueHealthCheck grensesnittavstemmingQueueHealthCheck = new GrensesnittavstemmingQueueHealthCheck(
                grensesnittavstemmingJmsProducer);

        assertThat(grensesnittavstemmingQueueHealthCheck.getDescriptionSuffix()).isEqualTo("Grensesnittavstemming");
    }
}
