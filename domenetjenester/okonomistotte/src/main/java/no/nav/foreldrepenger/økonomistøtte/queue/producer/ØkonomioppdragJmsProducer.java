package no.nav.foreldrepenger.økonomistøtte.queue.producer;

import no.nav.foreldrepenger.felles.jms.JmsKonfig;
import no.nav.foreldrepenger.felles.jms.QueueProducer;

public abstract class ØkonomioppdragJmsProducer extends QueueProducer {
    protected ØkonomioppdragJmsProducer() {
    }

    protected ØkonomioppdragJmsProducer(JmsKonfig konfig) {
        super(konfig);
    }

    /**
     * Legg oppdragXml på kø til oppdragssystemet.
     *
     * @param oppdragXML OppdragXml som representerer en oppdragsmottaker.
     */
    public abstract void sendØkonomiOppdrag(String oppdragXML);
}
