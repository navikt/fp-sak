package no.nav.foreldrepenger.datavarehus.v2;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.vedtak.felles.integrasjon.kafka.KafkaSender;

@ApplicationScoped
public class StønadsstatistikkKafkaProducer {

    private static final Logger LOG = LoggerFactory.getLogger(StønadsstatistikkKafkaProducer.class);

    private KafkaSender producer;

    @Inject
    StønadsstatistikkKafkaProducer(@KonfigVerdi(value = "kafka.stonadsstatistikk.topic") String topicName) {
        this.producer = new KafkaSender(topicName);
    }

    StønadsstatistikkKafkaProducer() {
    }

    public void sendJson(KafkaSender.KafkaHeader header, String nøkkel, String json) {
        LOG.info("Sender melding om stønadsstatistikkvedtak med nøkkel {} på topic='{}'", nøkkel, producer.getTopicName());
        var recordMetadata = producer.send(header, nøkkel, json);
        LOG.info("Sendte melding om stønadsstatistikkvedtak til {}, partition {}, offset {}", recordMetadata.topic(), recordMetadata.partition(),
            recordMetadata.offset());

    }

}
