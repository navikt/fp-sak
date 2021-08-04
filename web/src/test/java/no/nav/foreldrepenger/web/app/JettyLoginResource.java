package no.nav.foreldrepenger.web.app;

import java.net.URI;

import javax.enterprise.context.RequestScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;


/**
 * Innlogging ved kjøring lokalt.
 * <p>
 * Se utviklerhåndbok for hvordan dette fungerer.
 */
@Path("/login")
@RequestScoped
public class JettyLoginResource {

    @GET
    @Operation(description = "Innlogging ved kjøring lokalt", hidden = true)
    public Response login() {
        //  når vi har kommet hit, er brukeren innlogget og har fått ID-token. Kan da gjøre redirect til hovedsiden for VL
        return Response.temporaryRedirect(URI.create("http://localhost:9000/")).build();
    }
}
