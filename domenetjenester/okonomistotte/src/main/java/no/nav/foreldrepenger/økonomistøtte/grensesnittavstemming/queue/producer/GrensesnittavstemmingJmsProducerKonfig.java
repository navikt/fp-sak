package no.nav.foreldrepenger.økonomistøtte.grensesnittavstemming.queue.producer;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import no.nav.vedtak.felles.integrasjon.jms.BaseJmsKonfig;

@Named("grensesnittavstemmingjmsproducerkonfig")
@ApplicationScoped
public class GrensesnittavstemmingJmsProducerKonfig extends BaseJmsKonfig {

    public static final String JNDI_QUEUE = "jms/QueueFpsakGrensesnittavstemmingSend";

    private static final String UT_QUEUE_PREFIX = "RAY.AVSTEM_DATA";

    public GrensesnittavstemmingJmsProducerKonfig() {
        super(UT_QUEUE_PREFIX);
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
