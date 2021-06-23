package no.nav.foreldrepenger.økonomistøtte.queue.producer;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import no.nav.vedtak.felles.integrasjon.jms.BaseJmsKonfig;


@Named("økonomioppdragjmsproducerkonfig")
@ApplicationScoped
public class ØkonomioppdragJmsProducerKonfig extends BaseJmsKonfig {

    public static final String JNDI_QUEUE = "jms/QueueFpsakOkonomiOppdragSend";

    private static final String UT_QUEUE_PREFIX = "fpsak_okonomi_oppdrag_send";
    private static final String KVITTERING_QUEUE_PREFIX = "fpsak_okonomi_oppdrag_mottak";

    public ØkonomioppdragJmsProducerKonfig() {
        super(UT_QUEUE_PREFIX, KVITTERING_QUEUE_PREFIX);
    }

    @Override
    public String getQueueManagerUsername() {
        return "srvappserver"; // TODO - hent fra konfig når ny MQ-konfig innføres i august/september
    }

    @Override
    public String getQueueManagerPassword() {
        return null; // TODO - hent fra vault e.l. når ny MQ-konfig innføres i august/september
    }

}

