package no.nav.foreldrepenger.mottak.publiserer.producer;

import java.util.Properties;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.topic.Topic;
import no.nav.familie.topic.TopicManifest;
import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.vedtak.exception.IntegrasjonException;
import no.nav.vedtak.log.mdc.MDCOperations;

@ApplicationScoped
public class DialogHendelseProducer {

    private static final Logger LOG = LoggerFactory.getLogger(DialogHendelseProducer.class);

    private static final Topic topic = TopicManifest.BRUKERDIALOG_INNTEKTSMELDING;
    private static final String CALLID_NAME = "Nav-CallId";
    private Producer<String, String> producer;

    DialogHendelseProducer() {
        // cdi
    }

    @Inject
    public DialogHendelseProducer(@KonfigVerdi("kafka.bootstrap.servers") String bootstrapServers,
                                  @KonfigVerdi("kafka.schema.registry.url") String schemaRegistryUrl,
                                  @KonfigVerdi("systembruker.username") String username,
                                  @KonfigVerdi("systembruker.password") String password) {
        var properties = KafkaPropertiesUtil.opprettProperties(bootstrapServers, schemaRegistryUrl, topic.getProducerClientId(), username, password);

        this.producer = createProducer(properties);
    }

    public void sendJsonMedNøkkel(String nøkkel, String json) {
        var callId = MDCOperations.getCallId() != null ? MDCOperations.getCallId() : MDCOperations.generateCallId();
        runProducerWithSingleJson(new ProducerRecord<>(topic.getTopic(), null, nøkkel, json, new RecordHeaders().add(CALLID_NAME, callId.getBytes()))); // NOSONAR
    }

    private void runProducerWithSingleJson(ProducerRecord<String, String> record) {
        try {
            producer.send(record).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw kafkaPubliseringException(e);
        } catch (Exception e) {
            throw kafkaPubliseringException(e);
        }
    }

    private IntegrasjonException kafkaPubliseringException(Exception e) {
        return new IntegrasjonException("FP-HENDELSE-925475", "Uventet feil ved sending til Kafka, topic " + topic, e);
    }

    private Producer<String, String> createProducer(Properties properties) {
        LOG.info("Oppretter producer for topic='{}', keyClass='{}', valueClass='{}'", topic.getTopic(), topic.getSerdeKey(), topic.getSerdeValue());
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, topic.getSerdeKey().serializer().getClass().getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, topic.getSerdeValue().serializer().getClass().getName());
        return new KafkaProducer<>(properties);
    }


}
