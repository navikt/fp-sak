package no.nav.foreldrepenger.domene.vedtak.observer;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.vedtak.felles.integrasjon.kafka.KafkaSender;

@ApplicationScoped
public class VedtakHendelseKafkaProducer {

    private static final Logger LOG = LoggerFactory.getLogger(VedtakHendelseKafkaProducer.class);

    private KafkaSender producer;
    private String topicName;

    public VedtakHendelseKafkaProducer() {
    }

    @Inject
    public VedtakHendelseKafkaProducer(@KonfigVerdi("kafka.fattevedtak.topic") String topicName) {
        this.topicName = topicName;
        this.producer = new KafkaSender(topicName);
    }

    void sendJson(String nøkkel, String json) {
        LOG.info("Sender melding om vedtak med nøkkel {} på topic='{}'", nøkkel, topicName);
        var recordMetadata = producer.send(nøkkel, json);
        LOG.info("Sendte melding om vedtak til {}, partition {}, offset {}", recordMetadata.topic(), recordMetadata.partition(), recordMetadata.offset());
    }

}
