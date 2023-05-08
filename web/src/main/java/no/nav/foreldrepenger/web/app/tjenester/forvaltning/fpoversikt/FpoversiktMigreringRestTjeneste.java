package no.nav.foreldrepenger.web.app.tjenester.forvaltning.fpoversikt;

import static no.nav.foreldrepenger.web.app.tjenester.forvaltning.fpoversikt.FpoversiktMigeringBehandlingHendelseTask.migreringTaskData;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

@Path("/fpoversiktMigrering")
@ApplicationScoped
@Transactional
public class FpoversiktMigreringRestTjeneste {

    private ProsessTaskTjeneste taskTjeneste;

    public FpoversiktMigreringRestTjeneste() {
        // For CDI
    }

    @Inject
    public FpoversiktMigreringRestTjeneste(ProsessTaskTjeneste taskTjeneste) {
        this.taskTjeneste = taskTjeneste;
    }

    @POST
    @Operation(description = "Oppretter task for migrering", tags = "FORVALTNING-migrering")
    @Path("/opprettTask")
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.DRIFT, sporingslogg = false)
    public Response opprettTask() {
        var prosessTaskData = migreringTaskData();
        taskTjeneste.lagre(prosessTaskData);
        return Response.ok().build();
    }
}
