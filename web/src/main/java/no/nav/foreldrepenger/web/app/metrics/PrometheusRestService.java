package no.nav.foreldrepenger.web.app.metrics;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import io.prometheus.client.CollectorRegistry;
import io.swagger.v3.oas.annotations.Operation;
import static no.nav.vedtak.log.metrics.REGISTRY;

@Path("/metrics")
@ApplicationScoped

public class PrometheusRestService {

    @GET
    @Operation(hidden = true)
    @Path("/prometheus")
    public String prometheus() {
       return REGISTRY.scrape();
      
    }
}
