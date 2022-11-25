package no.nav.foreldrepenger.web.app.jackson;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import no.nav.foreldrepenger.web.app.healthchecks.Selftests;

@Path("/health")
@ApplicationScoped
public class HealthCheckRestService {

    private static final String RESPONSE_CACHE_KEY = "Cache-Control";
    private static final String RESPONSE_CACHE_VAL = "must-revalidate,no-cache,no-store";
    private static final String RESPONSE_OK = "OK";

    private Selftests selftests;

    private Boolean isContextStartupReady;

    @Inject
    public HealthCheckRestService(Selftests selftests) {
        this.selftests = selftests;
    }

    public HealthCheckRestService() {
    }

    /**
     * Bruk annet svar enn 200 dersom man ønsker trafikk dirigert vekk (eller få
     * nais til å oppskalere)
     */
    @GET
    @Operation(hidden = true)
    @Path("/isReady")
    public Response isReady() {
        if (isContextStartupReady && selftests.isReady()) {
            return Response
                    .ok(RESPONSE_OK, MediaType.TEXT_PLAIN_TYPE)
                    .header(RESPONSE_CACHE_KEY, RESPONSE_CACHE_VAL)
                    .build();
        }
        return Response
                .status(Response.Status.SERVICE_UNAVAILABLE)
                .header(RESPONSE_CACHE_KEY, RESPONSE_CACHE_VAL)
                .build();

    }

    /**
     * Bruk annet svar enn 200 kun dersom man ønsker at Nais skal restarte pod NB
     * ikke sjekk database her - vi ønsker ikke repeat restart pga timeslange
     * feilsituasjoner med Skatt.
     */
    @GET
    @Operation(hidden = true)
    @Path("/isAlive")
    public Response isAlive() {
        if (isContextStartupReady && selftests.isKafkaAlive()) {
            return Response
                    .ok(RESPONSE_OK, MediaType.TEXT_PLAIN_TYPE)
                    .header(RESPONSE_CACHE_KEY, RESPONSE_CACHE_VAL)
                    .build();
        }
        return Response
                .serverError()
                .header(RESPONSE_CACHE_KEY, RESPONSE_CACHE_VAL)
                .build();

    }

    /**
     * Settes av AppstartupServletContextListener ved contextInitialized
     *
     * @param isContextStartupReady
     */
    public void setIsContextStartupReady(Boolean isContextStartupReady) {
        this.isContextStartupReady = isContextStartupReady;
    }

}
