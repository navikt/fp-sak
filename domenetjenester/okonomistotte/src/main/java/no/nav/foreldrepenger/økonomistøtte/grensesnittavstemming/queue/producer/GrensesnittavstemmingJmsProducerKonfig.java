package no.nav.foreldrepenger.økonomistøtte.grensesnittavstemming.queue.producer;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.vedtak.felles.integrasjon.jms.BaseJmsKonfig;

@Named("grensesnittavstemmingjmsproducerkonfig")
@ApplicationScoped
public class GrensesnittavstemmingJmsProducerKonfig extends BaseJmsKonfig {

    private static final Environment ENV = Environment.current();

    private String mqUsername;
    private String mqPassword;

    public static final String JNDI_QUEUE = "jms/QueueFpsakGrensesnittavstemmingSend";

    private static final String UT_QUEUE_PREFIX = "RAY.AVSTEM_DATA";

    @Inject
    public GrensesnittavstemmingJmsProducerKonfig(@KonfigVerdi("mq.username") String bruker,
                                                  @KonfigVerdi("mq.password") String passord) {
        this();
        this.mqUsername = bruker;
        this.mqPassword = passord;
    }

    private GrensesnittavstemmingJmsProducerKonfig() {
        super(UT_QUEUE_PREFIX);
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
