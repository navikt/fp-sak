package no.nav.foreldrepenger.økonomistøtte.queue.producer;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.vedtak.felles.integrasjon.jms.BaseJmsKonfig;


@Named("økonomioppdragjmsproducerkonfig")
@ApplicationScoped
public class ØkonomioppdragJmsProducerKonfig extends BaseJmsKonfig {

    private static final Environment ENV = Environment.current();

    private String mqUsername;
    private String mqPassword;

    public static final String JNDI_QUEUE = "jms/QueueFpsakOkonomiOppdragSend";

    private static final String UT_QUEUE_PREFIX = "fpsak_okonomi_oppdrag_send";
    private static final String KVITTERING_QUEUE_PREFIX = "fpsak_okonomi_oppdrag_mottak";

    @Inject
    public ØkonomioppdragJmsProducerKonfig(@KonfigVerdi("mq.username") String bruker,
                                           @KonfigVerdi("mq.password") String passord) {
        this();
        this.mqUsername = bruker;
        this.mqPassword = passord;
    }

    private ØkonomioppdragJmsProducerKonfig() {
        super(UT_QUEUE_PREFIX, KVITTERING_QUEUE_PREFIX);
    }

    @Override
    public String getQueueManagerUsername() {
        return ENV.isDev() ? mqUsername : "srvappserver"; // TODO - hent fra konfig når ny MQ-konfig innføres i august/september
    }

    @Override
    public String getQueueManagerPassword() {
        return ENV.isDev() ? mqPassword : null; // TODO - hent fra vault e.l. når ny MQ-konfig innføres i august/september
    }

}

