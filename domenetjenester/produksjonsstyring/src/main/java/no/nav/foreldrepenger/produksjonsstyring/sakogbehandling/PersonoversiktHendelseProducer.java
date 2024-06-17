package no.nav.foreldrepenger.produksjonsstyring.sakogbehandling;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.vedtak.felles.integrasjon.kafka.KafkaSender;


@ApplicationScoped
public class PersonoversiktHendelseProducer {

    private static final Logger LOG = LoggerFactory.getLogger(PersonoversiktHendelseProducer.class);

    private KafkaSender producer;

    public PersonoversiktHendelseProducer() {
    }

    @Inject
    public PersonoversiktHendelseProducer(@KonfigVerdi(value = "kafka.personoversikt.topic") String topicName) {
        this.producer = new KafkaSender(topicName);
    }

    public void sendJsonMedNøkkel(String nøkkel, String json) {
        LOG.info("Sender melding om personoversikt-hendelse med nøkkel {} på topic='{}'", nøkkel, producer.getTopicName());
        var recordMetadata = producer.send(nøkkel, json);
        LOG.info("Sendte melding om personoversikt-hendelse til {}, partition {}, offset {}", recordMetadata.topic(), recordMetadata.partition(),
            recordMetadata.offset());
    }
}
