package no.nav.foreldrepenger.mottak.publiserer.producer;

import java.util.Properties;
import java.util.concurrent.ExecutionException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.errors.AuthenticationException;
import org.apache.kafka.common.errors.AuthorizationException;
import org.apache.kafka.common.errors.RetriableException;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.topic.Topic;
import no.nav.familie.topic.TopicManifest;
import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.vedtak.exception.IntegrasjonException;
import no.nav.vedtak.exception.ManglerTilgangException;
import no.nav.vedtak.exception.VLException;
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
    public DialogHendelseProducer(@KonfigVerdi("bootstrap.servers") String bootstrapServers,
                                  @KonfigVerdi("schema.registry.url") String schemaRegistryUrl,
                                  @KonfigVerdi("systembruker.username") String username,
                                  @KonfigVerdi("systembruker.password") String password) {
        var properties = KafkaPropertiesUtil.opprettProperties(bootstrapServers, schemaRegistryUrl, topic.getProducerClientId(), username, password);

        this.producer = createProducer(properties);
    }

    public void flush() {
        producer.flush();
    }

    private void runProducerWithSingleJson(ProducerRecord<String, String> record) {
        try {
            @SuppressWarnings("unused")
            var recordMetadata = producer.send(record).get(); //NOSONAR
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw uventetException(e);
        } catch (ExecutionException e) {
            throw uventetException(e);
        } catch (AuthenticationException | AuthorizationException e) {
            throw new ManglerTilgangException("FP-HENDELSE-821006", "Feil i pålogging mot Kafka, topic:" + topic.getTopic(), e);
        } catch (RetriableException e) {
            throw new IntegrasjonException("FP-HENDELSE-127609", "Fikk transient feil mot Kafka,"
                + " kan prøve igjen, topic: " + topic.getTopic(), e);
        } catch (KafkaException e) {
            throw new IntegrasjonException("FP-HENDELSE-811209", "Fikk feil mot Kafka, topic: " + topic.getTopic(), e);
        }
    }

    private VLException uventetException(Exception e) {
        return new IntegrasjonException("FP-HENDELSE-925470", "Uventet feil ved sending til Kafka, topic: "
            + topic.getTopic(), e);
    }

    private Producer<String, String> createProducer(Properties properties) {
        LOG.info("Oppretter producer for topic='{}', keyClass='{}', valueClass='{}'", topic.getTopic(), topic.getSerdeKey(), topic.getSerdeValue());
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, topic.getSerdeKey().serializer().getClass().getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, topic.getSerdeValue().serializer().getClass().getName());
        return new KafkaProducer<>(properties);
    }


    public void sendJsonMedNøkkel(String nøkkel, String json) {
        var callId = MDCOperations.getCallId() != null ? MDCOperations.getCallId() : MDCOperations.generateCallId();
        runProducerWithSingleJson(new ProducerRecord<>(topic.getTopic(), null, nøkkel, json, new RecordHeaders().add(CALLID_NAME, callId.getBytes()))); // NOSONAR
    }

}
