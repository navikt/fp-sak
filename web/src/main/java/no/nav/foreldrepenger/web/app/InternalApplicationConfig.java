package no.nav.foreldrepenger.web.app;


import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import io.prometheus.client.hotspot.DefaultExports;
import no.nav.foreldrepenger.web.app.healthchecks.HealthCheckRestService;
import no.nav.foreldrepenger.web.app.metrics.PrometheusRestService;

/**
 * Konfigurer Prometheus og Healthchecks (healthchecks bruker Dropwizards interface)
 */
@ApplicationScoped
@ApplicationPath(InternalApplicationConfig.INTERNAL_URI)
public class InternalApplicationConfig extends Application {

    public static final String INTERNAL_URI = "/internal";

    public InternalApplicationConfig() {
        //HS QAD siden registry ikke er tilgjengelig når klassen instansieres...
        DefaultExports.initialize();
    }

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();
        classes.add(PrometheusRestService.class);
        classes.add(HealthCheckRestService.class);

        return Collections.unmodifiableSet(classes);
    }


}
