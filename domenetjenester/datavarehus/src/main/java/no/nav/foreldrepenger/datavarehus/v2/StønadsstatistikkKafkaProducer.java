package no.nav.foreldrepenger.datavarehus.v2;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.vedtak.felles.integrasjon.kafka.KafkaSender;

@ApplicationScoped
public class StønadsstatistikkKafkaProducer {

    private static final Logger LOG = LoggerFactory.getLogger(StønadsstatistikkKafkaProducer.class);
    private static final Environment ENV = Environment.current();

    private KafkaSender producer;
    private String topicName;

    @Inject
    StønadsstatistikkKafkaProducer(@KonfigVerdi(value = "kafka.stonadsstatistikk.topic") String topicName) {
        this.topicName = topicName;
        this.producer = new KafkaSender(topicName);
    }

    StønadsstatistikkKafkaProducer() {
    }

    public void sendJson(String nøkkel, String json) {
        if (ENV.isLocal() || ENV.isDev()) { // TODO fjerne denne. Topic skal finnes i autotest
            return;
        }
        LOG.info("Sender melding om stønadsstatistikkvedtak med nøkkel {} på topic='{}'", nøkkel, topicName);
        var recordMetadata = producer.send(nøkkel, json);
        LOG.info("Sendte melding om stønadsstatistikkvedtak til {}, partition {}, offset {}", recordMetadata.topic(), recordMetadata.partition(), recordMetadata.offset());

    }

}
