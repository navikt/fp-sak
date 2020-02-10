package no.nav.foreldrepenger.domene.vedtak.task;

import java.util.Properties;
import java.util.UUID;
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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.vedtak.konfig.KonfigVerdi;
import no.nav.vedtak.util.StringUtils;

@ApplicationScoped
public class TilkjentYtelseMeldingProducer {

    private static final ObjectMapper OM;

    private static final Logger log = LoggerFactory.getLogger(TilkjentYtelseMeldingProducer.class);

    static {
        OM = new ObjectMapper();
        OM.registerModule(new JavaTimeModule());
        OM.registerModule(new Jdk8Module());
        OM.registerModule(new SimpleModule());
    }

    private Producer<String, String> producer;
    private String topic;

    TilkjentYtelseMeldingProducer() {
        // for CDI proxy
    }

    @Inject
    TilkjentYtelseMeldingProducer(@KonfigVerdi("fp.tilkjentytelse.v1.topic.url") String topic,
                                  @KonfigVerdi("bootstrap.servers") String bootstrapServers,
                                  @KonfigVerdi("application.name") String clientId,
                                  @KonfigVerdi("kafka.aksjonspunkthendelse.schema.registry.url") String schemaRegistryUrl,
                                  @KonfigVerdi("systembruker.username") String username,
                                  @KonfigVerdi("systembruker.password") String password) {
        Properties properties = new Properties();

        properties.setProperty("bootstrap.servers", bootstrapServers);
        properties.setProperty("schema.registry.url", schemaRegistryUrl);
        properties.setProperty("client.id", clientId);

        setSecurity(username, properties);
        setUsernameAndPassword(username, password, properties);

        this.producer = createProducer(properties);
        this.topic = topic;
    }

    void setUsernameAndPassword(String username, String password, Properties properties) {
        if (!StringUtils.nullOrEmpty(username) && !StringUtils.nullOrEmpty(password)) {
            String jaasTemplate = "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"%s\" password=\"%s\";";
            String jaasCfg = String.format(jaasTemplate, username, password);
            properties.setProperty("sasl.jaas.config", jaasCfg);
        }
    }

    Producer<String, String> createProducer(Properties properties) {
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        return new KafkaProducer<>(properties);
    }

    void setSecurity(String username, Properties properties) {
        if (!StringUtils.nullOrEmpty(username)) {
            properties.setProperty("security.protocol", "SASL_SSL");
            properties.setProperty("sasl.mechanism", "PLAIN");
        }
    }

    public void flush() {
        producer.flush();
    }

    void runProducerWithSingleJson(ProducerRecord<String, String> record) {
        try {
            @SuppressWarnings("unused")
            var recordMetadata = producer.send(record).get(); // NOSONAR
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Uventet feil ved sending til Kafka, topic:" + topic, e);
        } catch (ExecutionException e) {
            log.warn("Uventet feil ved sending til Kafka, topic:" + topic, e);
        } catch (AuthenticationException | AuthorizationException e) {
            log.warn("Feil i pålogging mot Kafka, topic:" + topic, e);
        } catch (RetriableException e) {
            log.warn("Fikk transient feil mot Kafka, kan prøve igjen, topic:" + topic, e);
        } catch (KafkaException e) {
            log.warn("Fikk feil mot Kafka, topic:" + topic, e);
        }
    }

    public void sendJsonMedNøkkel(String nøkkel, String json) {
        runProducerWithSingleJson(new ProducerRecord<>(topic, nøkkel, json));
    }

    public void sendTilkjentYtelse(Behandling behandling) {
        String fagsakYtelseTypeKode = behandling.getFagsakYtelseType().getKode();
        Saksnummer saksnummer = behandling.getFagsak().getSaksnummer();
        AktørId aktørId = behandling.getAktørId();
        UUID uuid = behandling.getUuid();
        Long behandlingId = behandling.getId();
        sendTilkjentYtelse(fagsakYtelseTypeKode, saksnummer, aktørId, behandlingId, uuid);
    }

    public void sendTilkjentYtelse(String fagsakYtelseTypeKode, Saksnummer saksnummer, AktørId aktørId, long behandlingId, UUID behandlingUuid) {
        TilkjentYtelseMelding verdi = new TilkjentYtelseMelding(fagsakYtelseTypeKode, saksnummer.getVerdi(), aktørId.getId(), behandlingId, behandlingUuid);

        try {
            String verdiSomJson = OM.writeValueAsString(verdi);
            sendJsonMedNøkkel(aktørId.getId(), verdiSomJson);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Kunne ikke konvertere til json for behandling " + behandlingId, e);
        }
    }

    public static class TilkjentYtelseMelding {
        @JsonProperty("fagsakYtelseType")
        private String fagsakYtelseType;
        @JsonProperty("gsakSaksnummer")
        private String saksnummer;
        @JsonProperty("aktoerId")
        private String aktørId;
        @JsonProperty("behandlingId")
        private Long behandlingId;
        @JsonProperty("behandlingUuid")
        private UUID behandlingUuid;
        @JsonProperty("ivSystem")
        private String iverksettingSystem = "fpsak";

        public TilkjentYtelseMelding(String fagsakYtelseType, String saksnummer, String aktørId, Long behandlingId, UUID behandlingUuid) {
            this.fagsakYtelseType = fagsakYtelseType;
            this.saksnummer = saksnummer;
            this.aktørId = aktørId;
            this.behandlingId = behandlingId;
            this.behandlingUuid = behandlingUuid;
        }

        public String getFagsakYtelseType() {
            return fagsakYtelseType;
        }

        public String getSaksnummer() {
            return saksnummer;
        }

        public String getAktørId() {
            return aktørId;
        }

        public Long getBehandlingId() {
            return behandlingId;
        }

        public UUID getBehandlingUuid() {
            return behandlingUuid;
        }

        public String getIverksettingSystem() {
            return iverksettingSystem;
        }
    }

}
