package no.nav.foreldrepenger.mottak.vedtak.kafka;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.apache.kafka.common.serialization.Serdes;

import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.foreldrepenger.konfig.Environment;

@Dependent
class VedtakStreamKafkaProperties {
    private static final Environment ENV = Environment.current();
    private final String bootstrapServers;
    private final String schemaRegistryUrl;
    private final String username;
    private final String password;
    private final String applicationId;
    private final String trustStorePath;
    private final String trustStorePassword;
    private final String topicName;

    @Inject
    VedtakStreamKafkaProperties(@KonfigVerdi("kafka.fattevedtak.topic") String topicName,
                                @KonfigVerdi("kafka.bootstrap.servers") String bootstrapServers,
                                @KonfigVerdi("kafka.schema.registry.url") String schemaRegistryUrl,
                                @KonfigVerdi("systembruker.username") String username,
                                @KonfigVerdi("systembruker.password") String password,
                                @KonfigVerdi(value = "javax.net.ssl.trustStore", required = false) String trustStorePath,
                                @KonfigVerdi(value = "javax.net.ssl.trustStorePassword", required = false) String trustStorePassword) {
        this.trustStorePath = trustStorePath;
        this.trustStorePassword = trustStorePassword;
        this.applicationId = applicationId();
        this.bootstrapServers = bootstrapServers;
        this.schemaRegistryUrl = schemaRegistryUrl;
        this.username = username;
        this.password = password;
        this.topicName = topicName;
    }

    private static String applicationId() {
        var prefix = ENV.getProperty("nais.app.name", "fpsak");
        return ENV.isProd() ? prefix + "-default" : prefix;
    }

    String getBootstrapServers() {
        return bootstrapServers;
    }

    String getSchemaRegistryUrl() {
        return schemaRegistryUrl;
    }

    String getClientId() {
        return "KC-" + topicName;
    }

    String getUsername() {
        return username;
    }

    String getPassword() {
        return password;
    }

    String getTopic() {
        return topicName;
    }

    @SuppressWarnings("resource")
    Class<?> getKeyClass() {
        return Serdes.String().getClass();
    }

    @SuppressWarnings("resource")
    Class<?> getValueClass() {
        return Serdes.String().getClass();
    }

    boolean harSattBrukernavn() {
        return username != null && !username.isEmpty();
    }

    String getApplicationId() {
        return applicationId;
    }

    boolean harSattTrustStore() {
        return trustStorePath != null && !trustStorePath.isEmpty()
            && trustStorePassword != null && !trustStorePassword.isEmpty();
    }

    String getTrustStorePath() {
        return trustStorePath;
    }

    String getTrustStorePassword() {
        return trustStorePassword;
    }
}
