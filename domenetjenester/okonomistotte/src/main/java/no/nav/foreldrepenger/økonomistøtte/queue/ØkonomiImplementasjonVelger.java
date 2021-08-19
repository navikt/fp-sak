package no.nav.foreldrepenger.økonomistøtte.queue;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.økonomistøtte.queue.producer.ØkonomioppdragJmsProducer;
import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.foreldrepenger.konfig.Environment;


/**
 * Denne klassen bytter mellom følgende implementasjoner for grensesnitt mot Oppdragsystemet:
 *
 * <li>MQ-integrasjon mot Oppdragssytemet (brukes i miljøer+prod). Denne er standard.</li>
 * <li>mock-implementasjon av grensesnittet. Denne velges ved å sette konfigurasjonsverdien test.only.disable.mq=true.
 * Denne implementasjonen finnes for å støtte verdikjedetest</li>
 */
@ApplicationScoped
public class ØkonomiImplementasjonVelger {

    private static final Logger LOG = LoggerFactory.getLogger(ØkonomiImplementasjonVelger.class);

    private ØkonomioppdragJmsProducer økonomioppdragJmsProducer;

    ØkonomiImplementasjonVelger() {
        //for CDI proxy
    }

    @Inject
    public ØkonomiImplementasjonVelger(@KonfigVerdi(value = "test.only.disable.mq", defaultVerdi = "false") Boolean disableMq,
                                       @Mq ØkonomioppdragJmsProducer mqØkonomiProducer, @TestOnlyMqDisabled ØkonomioppdragJmsProducer vtpØkonomiProducer) {
        if (disableMq) {
            if (Environment.current().isProd()) {
                throw new IllegalStateException("Skal ikke bruke test.only.disable.mq i produksjon. MQ er påkrevet i miljøet.");
            }
            LOG.warn("Bruker mock-implementasjon for integrasjon mot MQ siden flagget test.only.disable.mq er satt");
            økonomioppdragJmsProducer = vtpØkonomiProducer;
        } else {
            økonomioppdragJmsProducer = mqØkonomiProducer;
        }
    }

    @Produces
    public ØkonomioppdragJmsProducer getØkonomiProducer() {
        return økonomioppdragJmsProducer;
    }

}
