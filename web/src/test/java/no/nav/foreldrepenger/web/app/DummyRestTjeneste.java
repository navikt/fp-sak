package no.nav.foreldrepenger.web.app;

import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;

@Path("/dummy/{var:.+}")
public class DummyRestTjeneste {

    public DummyRestTjeneste() {

    }

    @GET
    @Operation(hidden = true)
    public Response get() {
        return Response.ok().build();
    }

    @POST
    @Operation(hidden = true)
    public Response post() {
        return Response.ok().build();
    }

    @OPTIONS
    @Operation(hidden = true)
    public Response options() {
        return Response.ok().build();
    }
}
