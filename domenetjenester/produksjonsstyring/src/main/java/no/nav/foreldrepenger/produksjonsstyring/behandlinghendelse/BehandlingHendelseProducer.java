package no.nav.foreldrepenger.produksjonsstyring.behandlinghendelse;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.vedtak.felles.integrasjon.kafka.KafkaSender;


@ApplicationScoped
public class BehandlingHendelseProducer {

    private static final Logger LOG = LoggerFactory.getLogger(BehandlingHendelseProducer.class);

    private KafkaSender producer;

    public BehandlingHendelseProducer() {
    }

    @Inject
    public BehandlingHendelseProducer(@KonfigVerdi(value = "kafka.behandlinghendelse.topic") String topicName) {
        this.producer = new KafkaSender(topicName);
    }

    public void sendJsonMedNøkkel(String nøkkel, String json) {
        LOG.info("Sender melding om behandlinghendelse med nøkkel {} på topic='{}'", nøkkel, producer.getTopicName());
        var recordMetadata = producer.send(nøkkel, json);
        LOG.info("Sendte melding om behandlinghendelse til {}, partition {}, offset {}", recordMetadata.topic(), recordMetadata.partition(),
            recordMetadata.offset());
    }


}
