package no.nav.foreldrepenger.web.app.konfig;


import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import io.prometheus.client.hotspot.DefaultExports;
import no.nav.foreldrepenger.web.app.jackson.HealthCheckRestService;
import no.nav.foreldrepenger.web.app.metrics.PrometheusRestService;

/**
 * Konfigurer Prometheus og Healthchecks
 */
@ApplicationScoped
@ApplicationPath(InternalAppConfig.INTERNAL_URI)
public class InternalAppConfig extends Application {

    public static final String INTERNAL_URI = "/internal";

    public InternalAppConfig() {
        //HS QAD siden registry ikke er tilgjengelig når klassen instansieres...
        DefaultExports.initialize();
    }

    @Override
    public Set<Class<?>> getClasses() {
        return Set.of(PrometheusRestService.class, HealthCheckRestService.class);
    }
}
