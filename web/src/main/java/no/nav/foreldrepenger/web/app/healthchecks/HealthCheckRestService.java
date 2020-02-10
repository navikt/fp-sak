package no.nav.foreldrepenger.web.app.healthchecks;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.health.HealthCheck;

import io.swagger.v3.oas.annotations.Operation;
import no.nav.foreldrepenger.web.app.healthchecks.checks.ExtHealthCheck;

@Path("/health")
@ApplicationScoped
public class HealthCheckRestService {

    private static CacheControl cacheControl = noCache();
    private static final Logger LOGGER = LoggerFactory.getLogger(HealthCheckRestService.class);

    private Boolean isReady;

    @Inject //NOSONAR slik at servlet container får sin 0-arg ctor
    private transient Selftests selftests; //NOSONAR

    // for enhetstester
    void setSelftests(Selftests selftests) {
        this.selftests = selftests; //NOSONAR
    }

    @GET
    @Operation(hidden = true)
    @Path("/selftest")
    public Response selftest() {
        Response.ResponseBuilder builder = Response.ok(hentResultatSomHTML(), MediaType.TEXT_HTML_TYPE);
        builder.cacheControl(cacheControl);
        return builder.build();
    }

    @GET
    @Operation(hidden = true)
    @Path("/isReady")
    public Response isReady() {
        Response.ResponseBuilder builder;
        if (isReady) {
            builder = Response.ok("OK", MediaType.TEXT_PLAIN_TYPE);
        } else {
            builder = Response.status(Response.Status.SERVICE_UNAVAILABLE);
        }
        builder.cacheControl(cacheControl);
        return builder.build();
    }

    /**
     *
     * Så lenge jetty klarer å svare 200 OK, og at applikasjonen er startet opp regner vi med att allt er i orden
     *
     * @return 200 OK if app is started
     */
    @GET
    @Operation(hidden = true)
    @Path("/isAlive")
    public Response isAlive() {
       return isReady();
    }

    /**
     * Settes av AppstartupServletContextListener ved contextInitialized
     *
     * @param isReady
     */
    public void setIsReady(Boolean isReady) {
        this.isReady = isReady;
    }

    private String hentResultatSomHTML() {
        SelftestsHtmlFormatter htmlFormatter = new SelftestsHtmlFormatter();

        SelftestResultat samletResultat = selftests.run(); //NOSONAR

        for (HealthCheck.Result result : samletResultat.getKritiskeResultater()) {
            if (!result.isHealthy()) {
                SelftestFeil.FACTORY.kritiskSelftestFeilet(
                    getDetailValue(result, ExtHealthCheck.DETAIL_DESCRIPTION),
                    getDetailValue(result, ExtHealthCheck.DETAIL_ENDPOINT),
                    getDetailValue(result, ExtHealthCheck.DETAIL_RESPONSE_TIME),
                    result.getMessage()
                ).toException().log(LOGGER);
            }
        }
        for (HealthCheck.Result result : samletResultat.getIkkeKritiskeResultater()) {
            if (!result.isHealthy()) {
                SelftestFeil.FACTORY.ikkeKritiskSelftestFeilet(
                    getDetailValue(result, ExtHealthCheck.DETAIL_DESCRIPTION),
                    getDetailValue(result, ExtHealthCheck.DETAIL_ENDPOINT),
                    getDetailValue(result, ExtHealthCheck.DETAIL_RESPONSE_TIME),
                    result.getMessage()
                ).toException().log(LOGGER);
            }
        }

       return htmlFormatter.format(samletResultat);
    }

    private String getDetailValue(HealthCheck.Result resultat, String key) {
        Map<String, Object> details = resultat.getDetails();
        if (details != null) {
            return (String) details.get(key);
        } else {
            return null;
        }
    }

    private static CacheControl noCache() {
        CacheControl cc = new CacheControl();
        cc.setMustRevalidate(true);
        cc.setPrivate(true);
        cc.setNoCache(true);
        cc.setNoStore(true);
        return cc;
    }
}
