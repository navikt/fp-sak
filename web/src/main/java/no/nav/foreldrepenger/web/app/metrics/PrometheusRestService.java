package no.nav.foreldrepenger.web.app.metrics;

import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static no.nav.vedtak.log.metrics.MetricsUtil.REGISTRY;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import io.swagger.v3.oas.annotations.Operation;

@Path("/metrics")
@Produces(TEXT_PLAIN)
@ApplicationScoped

public class PrometheusRestService {

    @GET
    @Operation(hidden = true)
    @Path("/prometheus")
    public String prometheus() {
        return REGISTRY.scrape();
    }
}
