package no.nav.foreldrepenger.web.app.tjenester.batch;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.foreldrepenger.batch.BatchSupportTjeneste;
import no.nav.foreldrepenger.batch.feil.BatchFeil;
import no.nav.foreldrepenger.web.app.tjenester.batch.args.BatchArgumentsDto;
import no.nav.vedtak.log.util.LoggerUtils;
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

    private static String retrieveBatchServiceFrom(String executionId) {
        if (executionId != null && executionId.contains("-")) {
            return executionId.substring(0, executionId.indexOf('-'));
        }
        throw new IllegalStateException();
    }

    /**
     * Kalles på for å logge brukeren inn i løsningen. Dette for å ha minimalt med
     * innloggingslogikk i bash-scriptet
     *
     * @return alltid 200 - OK
     */
    @GET
    @Path("/init")
    @Operation(description = "Init", tags = "batch")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.DRIFT, sporingslogg = false)
    public Response init() {
        return Response.ok().build();
    }

    @POST
    @Path("/launch")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(description = "Start batchjob", tags = "batch", responses = {
            @ApiResponse(responseCode = "200", description = "Starter batch og returnerer executionId", content = @Content(mediaType = MediaType.TEXT_PLAIN, schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "400", description = "Ukjent batch forespurt"),
            @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil")
    })
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.BATCH)
    public Response startBatch(@NotNull @QueryParam("batchName") @Valid BatchNameDto batchName, @Valid BatchArgumentsDto args) {
        var name = batchName.getVerdi();
        final var batchTjeneste = batchSupportTjeneste.finnBatchTjenesteForNavn(name);
        if (batchTjeneste != null) {
            final var arguments = batchTjeneste.createArguments(args.getArguments());
            if (arguments.isValid()) {
                LOG.info("Starter batch {}", LoggerUtils.removeLineBreaks(name)); // NOSONAR
                return Response.ok(batchTjeneste.launch(arguments)).build();
            }
            throw BatchFeil.ugyldigeJobParametere(arguments);
        }
        LOG.warn("Ugyldig job-navn " + name);
        return Response.status(Response.Status.BAD_REQUEST).build();
    }

    @POST
    @Path("/autorun")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(description = "Start task for å kjøre batchjobs", tags = "batch", responses = {
            @ApiResponse(responseCode = "200", description = "Starter batch-scheduler"),
            @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil")
    })
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.BATCH)
    public Response autoRunBatch() {
        batchSupportTjeneste.startBatchSchedulerTask();
        return Response.ok().build();
    }

}
