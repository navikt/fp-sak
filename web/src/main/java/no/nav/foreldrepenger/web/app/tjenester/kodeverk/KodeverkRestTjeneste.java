package no.nav.foreldrepenger.web.app.tjenester.kodeverk;

import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import no.nav.foreldrepenger.web.app.tjenester.kodeverk.app.HentKodeverdierTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.kodeverk.dto.KodeverdiMedNavnDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

@Path("/kodeverk")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
public class KodeverkRestTjeneste {

    public static final String KODERVERK_PATH = "/kodeverk";

    private static final Map<String, List<KodeverdiMedNavnDto>> KODEVERDIER = HentKodeverdierTjeneste.hentGruppertKodeliste();

    @GET
    @Operation(description = "Henter kodeliste", tags = "kodeverk")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.APPLIKASJON, sporingslogg = false)
    public Response hentGruppertKodelisteMedNavn() {
        var cc = new CacheControl();
        cc.setMaxAge(60 * 60); // tillater klient caching i 1 time
        return Response.ok()
            .entity(KODEVERDIER)
            .type(MediaType.APPLICATION_JSON)
            .cacheControl(cc)
            .build();
    }

    // Ved ønske om kodegenerering kan man legge på en ApiResponse 200 med content/schema/implementation = KodeverkResponse
    // class KodeverkResponse extends java.util.HashMap<String, List<KodeverdiMedNavnDto>>
    // Man får da en map av String/kodeverk til liste av objekt med kode+navn som string - ikke enum som i dagens frontend

}
