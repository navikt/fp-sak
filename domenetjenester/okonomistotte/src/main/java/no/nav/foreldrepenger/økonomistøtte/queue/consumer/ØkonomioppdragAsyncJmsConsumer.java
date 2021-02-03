package no.nav.foreldrepenger.økonomistøtte.queue.consumer;

import no.nav.vedtak.felles.integrasjon.jms.QueueSelftest;

public interface ØkonomioppdragAsyncJmsConsumer extends QueueSelftest {
    void handle(String message);
}
