package no.nav.foreldrepenger.økonomistøtte.queue;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.økonomistøtte.queue.producer.ØkonomioppdragJmsProducer;


/**
 * Denne klassen bytter mellom følgende implementasjoner for grensesnitt mot Oppdragsystemet:
 *
 * <li>MQ-integrasjon mot Oppdragssytemet (brukes i miljøer+prod). Denne er standard.</li>
 * <li>mock-implementasjon av grensesnittet. Denne implementasjonen finnes for å støtte verdikjedetest</li>
 */
@ApplicationScoped
public class ØkonomiImplementasjonVelger {

    private ØkonomioppdragJmsProducer økonomioppdragJmsProducer;

    ØkonomiImplementasjonVelger() {
        //for CDI proxy
    }

    @Inject
    public ØkonomiImplementasjonVelger(@Mq ØkonomioppdragJmsProducer mqØkonomiProducer, @TestOnlyMqDisabled ØkonomioppdragJmsProducer vtpØkonomiProducer) {
        var miljøToggle = new FellesJmsToggle();
        if (miljøToggle.isDisabled()) {
            if (Environment.current().isProd()) {
                throw new IllegalStateException("Skal bruke normal MQ-producer i produksjon.");
            }
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
