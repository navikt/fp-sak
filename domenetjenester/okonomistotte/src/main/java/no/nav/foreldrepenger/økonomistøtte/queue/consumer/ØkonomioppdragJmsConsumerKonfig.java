package no.nav.foreldrepenger.økonomistøtte.queue.consumer;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.vedtak.felles.integrasjon.jms.BaseJmsKonfig;

@Named("økonomioppdragjmsconsumerkonfig")
@ApplicationScoped
public class ØkonomioppdragJmsConsumerKonfig extends BaseJmsKonfig {

    private String mqUsername;
    private String mqPassword;

    public static final String JNDI_QUEUE = "jms/QueueFpsakOkonomiOppdragMotta";
    private static final String INN_QUEUE_PREFIX = "fpsak_okonomi_oppdrag_mottak";

    @Inject
    public ØkonomioppdragJmsConsumerKonfig(@KonfigVerdi("mq.username") String bruker,
                                           @KonfigVerdi("mq.password") String passord) {
        this();
        this.mqUsername = bruker;
        this.mqPassword = passord;
    }

    private ØkonomioppdragJmsConsumerKonfig() {
        super(INN_QUEUE_PREFIX);
    }

    @Override
    public String getQueueManagerUsername() {
        return mqUsername;
    }

    @Override
    public String getQueueManagerPassword() {
        return mqPassword;
    }

}
