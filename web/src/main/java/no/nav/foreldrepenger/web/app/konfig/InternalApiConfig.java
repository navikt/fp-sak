package no.nav.foreldrepenger.web.app.konfig;


import java.util.Set;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

import no.nav.foreldrepenger.web.app.healthchecks.HealthCheckRestService;
import no.nav.foreldrepenger.web.app.metrics.PrometheusRestService;
import no.nav.vedtak.server.rest.jackson.Jackson2MapperFeature;

/**
 * Konfigurer Prometheus og Healthchecks
 */
@ApplicationPath(InternalApiConfig.API_URI)
public class InternalApiConfig extends Application {

    public static final String API_URI = "/internal";

    @Override
    public Set<Class<?>> getClasses() {
        return Set.of(HealthCheckRestService.class, PrometheusRestService.class, Jackson2MapperFeature.class);
    }
}
