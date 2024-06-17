package no.nav.foreldrepenger.web.app.tjenester.forvaltning.fpoversikt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.vedtak.felles.integrasjon.kafka.KafkaSender;


@ApplicationScoped
public class FpoversiktHendelseProducer {

    private static final Logger LOG = LoggerFactory.getLogger(FpoversiktHendelseProducer.class);

    private KafkaSender producer;

    public FpoversiktHendelseProducer() {
    }

    @Inject
    public FpoversiktHendelseProducer(@KonfigVerdi(value = "kafka.fpoversikt.migrering.topic") String topicName) {
        this.producer = new KafkaSender(topicName);
    }

    public void sendJsonMedNøkkel(String nøkkel, String json) {
        LOG.info("Sender melding om oversikt-migrering med nøkkel {} på topic='{}'", nøkkel, producer.getTopicName());
        var recordMetadata = producer.send(nøkkel, json);
        LOG.info("Sendte melding om oversikt-migrering til {}, partition {}, offset {}", recordMetadata.topic(), recordMetadata.partition(),
            recordMetadata.offset());
    }

}
