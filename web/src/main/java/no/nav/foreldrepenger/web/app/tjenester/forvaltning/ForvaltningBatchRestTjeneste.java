package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.foreldrepenger.batch.BatchSupportTjeneste;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

@Path("/batch")
@ApplicationScoped
@Transactional
public class ForvaltningBatchRestTjeneste {

    private BatchSupportTjeneste batchSupportTjeneste;

    public ForvaltningBatchRestTjeneste() {
        // For CDI
    }

    @Inject
    public ForvaltningBatchRestTjeneste(BatchSupportTjeneste batchSupportTjeneste) {
        this.batchSupportTjeneste = batchSupportTjeneste;
    }

    @POST
    @Path("/autorun")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(description = "Start task for å kjøre batchjobs", tags = "FORVALTNING-batch", responses = {
            @ApiResponse(responseCode = "200", description = "Starter batch-scheduler"),
            @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil")
    })
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.DRIFT, sporingslogg = false)
    public Response autoRunBatch() {
        batchSupportTjeneste.startBatchSchedulerTask();
        return Response.ok().build();
    }

}
