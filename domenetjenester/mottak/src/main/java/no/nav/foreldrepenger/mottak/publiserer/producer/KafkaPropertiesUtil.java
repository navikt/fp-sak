package no.nav.foreldrepenger.mottak.publiserer.producer;

import java.util.Properties;

class KafkaPropertiesUtil {

    private KafkaPropertiesUtil() {}

    static Properties opprettProperties(String bootstrapServers, String schemaRegistryUrl, String clientId, String username, String password) {
        Properties properties = new Properties();

        properties.setProperty("bootstrap.servers", bootstrapServers);
        properties.setProperty("schema.registry.url", schemaRegistryUrl);
        properties.setProperty("client.id", clientId);

        setSecurity(username, properties);
        setUsernameAndPassword(username, password, properties);
        return properties;
    }


    private static void setUsernameAndPassword(String username, String password, Properties properties) {
        if ((username != null && !username.isEmpty())
                && (password != null && !password.isEmpty())) {
            String jaasTemplate = "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"%s\" password=\"%s\";";
            String jaasCfg = String.format(jaasTemplate, username, password);
            properties.setProperty("sasl.jaas.config", jaasCfg);
        }
    }

    private static void setSecurity(String username, Properties properties) {
        if (username != null && !username.isEmpty()) {
            properties.setProperty("security.protocol", "SASL_SSL");
            properties.setProperty("sasl.mechanism", "PLAIN");
        }
    }
}
