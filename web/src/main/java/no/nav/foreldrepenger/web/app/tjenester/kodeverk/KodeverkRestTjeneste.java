package no.nav.foreldrepenger.web.app.tjenester.kodeverk;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.annotations.Operation;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
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
    public static final String ENHETER_PATH = KODERVERK_PATH + "/behandlende-enheter";

    private HentKodeverkTjeneste hentKodeverkTjeneste; // NOSONAR

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

    @GET
    @Path("/behandlende-enheter")
    @Operation(description = "Henter liste over behandlende enheter", tags = "kodeverk")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK)
    public List<OrganisasjonsEnhet> hentBehandlendeEnheter() {
        return hentKodeverkTjeneste.hentBehandlendeEnheter();
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
        Map<String, Object> kodelisterGruppertPåType = new HashMap<>();

        var grupperteKodelister = hentKodeverkTjeneste.hentGruppertKodeliste();
        grupperteKodelister.entrySet().forEach(e -> kodelisterGruppertPåType.put(e.getKey(), e.getValue()));

        var avslagårsakerGruppertPåVilkårType = VilkårType.finnAvslagårsakerGruppertPåVilkårType()
                .entrySet().stream().collect(Collectors.toMap(e -> e.getKey().getKode(), Map.Entry::getValue));
        kodelisterGruppertPåType.put(Avslagsårsak.class.getSimpleName(), avslagårsakerGruppertPåVilkårType);
        return kodelisterGruppertPåType;
    }

}
