package no.nav.foreldrepenger.web.app.konfig;


import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

import org.glassfish.jersey.server.ServerProperties;

import no.nav.foreldrepenger.web.app.tjenester.RestImplementationClasses;

/**
 * Konfigurer Prometheus og Healthchecks
 */
@ApplicationPath(ServiceApiConfig.SERVICE_URI)
public class ServiceApiConfig extends Application {

    public static final String SERVICE_URI = "/service/api";

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();
        // Grensesnitt for satellitt-appplikasjoner
        classes.addAll(RestImplementationClasses.getServiceClasses());
        // Applikasjonsoppsett
        classes.addAll(FellesConfigClasses.getFellesConfigClasses());

        return Collections.unmodifiableSet(classes);
    }

    @Override
    public Map<String, Object> getProperties() {
        Map<String, Object> properties = new HashMap<>();
        // Ref Jersey doc
        properties.put(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true);
        properties.put(ServerProperties.PROCESSING_RESPONSE_ERRORS_ENABLED, true);
        return properties;
    }
}
