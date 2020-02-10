package no.nav.foreldrepenger.domene.risikoklassifisering.kafka.config;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.vedtak.konfig.KonfigVerdi;

@ApplicationScoped
class RisikoklassifiseringMeldingProducer extends GenerellMeldingProducer {

    public RisikoklassifiseringMeldingProducer() {
        // for CDI proxy
    }

    @Inject
    public RisikoklassifiseringMeldingProducer(@KonfigVerdi("kafka.risikoklassifisering.topic") String topic,
                                               @KonfigVerdi("bootstrap.servers") String bootstrapServers,
                                               @KonfigVerdi("kafka.risikoklassifisering.schema.registry.url") String schemaRegistryUrl,
                                               @KonfigVerdi("kafka.risikoklassifisering.client.id") String clientId,
                                               @KonfigVerdi("systembruker.username") String username,
                                               @KonfigVerdi("systembruker.password") String password) {
        super(topic, bootstrapServers, schemaRegistryUrl, clientId, username, password);
    }
}
