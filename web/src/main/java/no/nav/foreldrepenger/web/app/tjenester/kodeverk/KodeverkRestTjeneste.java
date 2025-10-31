package no.nav.foreldrepenger.web.app.tjenester.kodeverk;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.annotations.Operation;
import no.nav.foreldrepenger.web.app.jackson.JacksonJsonConfig;
import no.nav.foreldrepenger.web.app.tjenester.kodeverk.app.HentKodeverkTjeneste;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

@Path("/kodeverk")
@ApplicationScoped
@Transactional
@Produces(MediaType.APPLICATION_JSON)
public class KodeverkRestTjeneste {

    public static final String KODERVERK_PATH = "/kodeverk";

    private HentKodeverkTjeneste hentKodeverkTjeneste;

    private final JacksonJsonConfig jsonMapper = new JacksonJsonConfig(true); // generere fulle kodeverdi-objekt

    private final ObjectMapper objectMapper = jsonMapper.getObjectMapper();

    private String kodelisteCache;

    @Inject
    public KodeverkRestTjeneste(HentKodeverkTjeneste hentKodeverkTjeneste) {
        this.hentKodeverkTjeneste = hentKodeverkTjeneste;
    }

    public KodeverkRestTjeneste() {
        // CDI
    }

    @GET
    @Operation(description = "Henter kodeliste", tags = "kodeverk")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.APPLIKASJON, sporingslogg = false)
    public Response hentGruppertKodeliste() throws IOException {
        var kodelisteJson = getKodeverkRawJson();
        var cc = new CacheControl();
        cc.setMaxAge(60 * 60); // tillater klient caching i 1 time
        return Response.ok()
                .entity(kodelisteJson)
                .type(MediaType.APPLICATION_JSON)
                .cacheControl(cc)
                .build();
    }

    private String getKodeverkRawJson() throws JsonProcessingException {
        if (kodelisteCache == null) {
            kodelisteCache = tilJson(this.hentGruppertKodelisteTilCache());
        }
        return kodelisteCache;
    }

    private String tilJson(Map<String, Object> kodeverk) throws JsonProcessingException {
        return objectMapper.writeValueAsString(kodeverk);
    }

    private synchronized Map<String, Object> hentGruppertKodelisteTilCache() {

        var grupperteKodelister = hentKodeverkTjeneste.hentGruppertKodeliste();
        return new HashMap<>(grupperteKodelister);
    }

}
