package no.nav.foreldrepenger.produksjonsstyring.sakogbehandling.kafka;

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
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import contract.sob.dto.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.vedtak.konfig.KonfigVerdi;

@ApplicationScoped
public class SakOgBehandlingHendelseProducer {

    private static final Logger LOG = LoggerFactory.getLogger(SakOgBehandlingHendelseProducer.class);

    private static final String CALLID_NAME = "Nav-CallId";
    private String topic;
    private Producer<String, String> producer;

    SakOgBehandlingHendelseProducer() {
        // cdi
    }

    @Inject
    public SakOgBehandlingHendelseProducer(@KonfigVerdi("kafka.sakogbehandling.topic") String topicName,
                                           @KonfigVerdi("bootstrap.servers") String bootstrapServers,
                                           @KonfigVerdi("schema.registry.url") String schemaRegistryUrl,
                                           @KonfigVerdi("systembruker.username") String username,
                                           @KonfigVerdi("systembruker.password") String password) {
        Properties properties = KafkaPropertiesUtil.opprettProperties(bootstrapServers, schemaRegistryUrl, getProducerClientId(topicName), username, password);
        this.topic = topicName;
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
            throw SakOgBehandlingHendelseProducerFeil.FACTORY.uventetFeil(topic, e).toException();
        } catch (ExecutionException e) {
            throw SakOgBehandlingHendelseProducerFeil.FACTORY.uventetFeil(topic, e).toException();
        } catch (AuthenticationException | AuthorizationException e) {
            throw SakOgBehandlingHendelseProducerFeil.FACTORY.feilIPålogging(topic, e).toException();
        } catch (RetriableException e) {
            throw SakOgBehandlingHendelseProducerFeil.FACTORY.retriableExceptionMotKaka(topic, e).toException();
        } catch (KafkaException e) {
            throw SakOgBehandlingHendelseProducerFeil.FACTORY.annenExceptionMotKafka(topic, e).toException();
        }
    }


    private Producer<String, String> createProducer(Properties properties) {
        LOG.info("Oppretter producer for topic='{}', keyClass='{}', valueClass='{}'", topic, StringSerializer.class.getName(), StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        return new KafkaProducer<>(properties);
    }

/*
    public void sendJsonMedNøkkel(String nøkkel, String json) {
        String callId = MDCOperations.getCallId() != null ? MDCOperations.getCallId() : MDCOperations.generateCallId();
        runProducerWithSingleJson(new ProducerRecord<>(topic, null, nøkkel, json, new RecordHeaders().add(CALLID_NAME, callId.getBytes()))); // NOSONAR
    }
*/


    private String generatePayload(BehandlingStatus hendelse) {
        return JsonObjectMapper.toJson(hendelse, SakOgBehandlingHendelseProducerFeil.FACTORY::kanIkkeSerialisere);
    }


    public void sendJson(String json) {
        runProducerWithSingleJson(new ProducerRecord<>(topic, json));
    }

    public void sendJsonMedNøkkel(String nøkkel, String json) {
        runProducerWithSingleJson(new ProducerRecord<>(topic, nøkkel, json));
    }

    private String getProducerClientId(String topicName) {
        return "KP-" + topicName;
    }

    private String createUniqueBehandlingsId(String behandlingsId) {
        return String.format("%s_%s", Fagsystem.FPSAK.getOffisiellKode(), behandlingsId);
    }

    private String createUniqueKey(String behandlingsId, String event) {
        return String.format("%s_%s_%s", Fagsystem.FPSAK.getOffisiellKode(), behandlingsId, event);
    }


}
