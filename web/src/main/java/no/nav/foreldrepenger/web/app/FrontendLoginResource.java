package no.nav.foreldrepenger.web.app;

import java.net.URI;

import javax.enterprise.context.RequestScoped;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import no.nav.foreldrepenger.konfig.Environment;

@Path("/login")
@RequestScoped
public class FrontendLoginResource {

    private static boolean IS_LOCAL = Environment.current().isLocal();

    @GET
    public Response login(@QueryParam("redirectTo") @DefaultValue("/fpsak/") String redirectTo) {
        if (IS_LOCAL) {
            return Response.temporaryRedirect(URI.create("http://localhost:9000/fpsak")).build();
        }
        return doLogin(redirectTo);
    }

    public Response doLogin(String redirectTo) {
        var uri = URI.create(redirectTo);
        var relativePath = "";
        if (uri.getPath() != null) {
            relativePath += uri.getPath();
        }
        if (uri.getQuery() != null) {
            relativePath += '?' + uri.getQuery();
        }
        if (uri.getFragment() != null) {
            relativePath += '#' + uri.getFragment();
        }
        if (!relativePath.startsWith("/")) {
            relativePath = "/" + relativePath;
        }
        //  når vi har kommet hit, er brukeren innlogget og har fått ID-token. Kan da gjøre redirect til hovedsiden for VL
        return Response.status(307).header(HttpHeaders.LOCATION, relativePath).build();
    }
}
