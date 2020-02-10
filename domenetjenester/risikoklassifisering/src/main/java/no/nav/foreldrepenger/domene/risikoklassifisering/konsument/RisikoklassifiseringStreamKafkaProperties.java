package no.nav.foreldrepenger.domene.risikoklassifisering.konsument;

import no.nav.familie.topic.Topic;
import no.nav.familie.topic.TopicManifest;
import no.nav.vedtak.konfig.KonfigVerdi;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

@Dependent
public class RisikoklassifiseringStreamKafkaProperties {
    private final String bootstrapServers;
    private final Topic kontraktTopic;
    private final String username;
    private final String password;
    private final String topic;
    private String applicationId;


    @Inject
    public RisikoklassifiseringStreamKafkaProperties(@KonfigVerdi("bootstrap.servers") String bootstrapServers,
                                                     @KonfigVerdi("systembruker.username") String username,
                                                     @KonfigVerdi("systembruker.password") String password,
                                                     @KonfigVerdi("kafka.kontroll.resultat.topic") String topic) {
        this.topic = topic;
        this.bootstrapServers = bootstrapServers;
        this.username = username;
        this.password = password;
        this.applicationId = "ID-" + topic;
        this.kontraktTopic = TopicManifest.KONTROLL_RESULTAT;
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
