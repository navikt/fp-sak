package no.nav.foreldrepenger.historikk.kafka;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.familie.topic.Topic;
import no.nav.familie.topic.TopicManifest;
import no.nav.vedtak.konfig.KonfigVerdi;

@Dependent
public class HistorikkStreamKafkaProperties {

    private final String bootstrapServers;
    private final Topic kontraktTopic;
    private final String username;
    private final String password;
    private final String topic;
    private String applicationId;

    @Inject
    HistorikkStreamKafkaProperties(@KonfigVerdi("bootstrap.servers") String bootstrapServers,
                                   @KonfigVerdi("systembruker.username") String username,
                                   @KonfigVerdi("systembruker.password") String password,
                                   @KonfigVerdi("kafka.historikkinnslag.topic") String topic) {
        this.topic = topic;
        this.bootstrapServers = bootstrapServers;
        this.username = username;
        this.password = password;
        this.applicationId = "ID-" + topic;
        this.kontraktTopic = TopicManifest.HISTORIKK_HENDELSE;
    }

    String getBootstrapServers() {
        return bootstrapServers;
    }

    String getUsername() {
        return username;
    }

    String getPassword() {
        return password;
    }

    String getTopic() {
        return topic;
    }

    boolean harSattBrukernavn() {
        return username != null && !username.isEmpty();
    }

    String getClientId() {
        return "KC-" + topic;
    }

    Class<?> getKeyClass() {
        return kontraktTopic.getSerdeKey().getClass();
    }

    Class<?> getValueClass() {
        return kontraktTopic.getSerdeValue().getClass();
    }

    String getApplicationId() {
        return applicationId;
    }
}
