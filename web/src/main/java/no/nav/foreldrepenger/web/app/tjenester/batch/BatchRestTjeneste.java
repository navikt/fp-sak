package no.nav.foreldrepenger.web.app.tjenester.batch;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.foreldrepenger.batch.BatchSupportTjeneste;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

@Path("/batch")
@ApplicationScoped
@Transactional
public class BatchRestTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(BatchRestTjeneste.class);

    private BatchSupportTjeneste batchSupportTjeneste;

    public BatchRestTjeneste() {
        // For CDI
    }

    @Inject
    public BatchRestTjeneste(BatchSupportTjeneste batchSupportTjeneste) {
        this.batchSupportTjeneste = batchSupportTjeneste;
    }

    @POST
    @Path("/autorun")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(description = "Start task for å kjøre batchjobs", tags = "batch", responses = {
            @ApiResponse(responseCode = "200", description = "Starter batch-scheduler"),
            @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil")
    })
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.DRIFT, sporingslogg = false)
    public Response autoRunBatch() {
        batchSupportTjeneste.startBatchSchedulerTask();
        return Response.ok().build();
    }

}
