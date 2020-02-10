package no.nav.foreldrepenger.web.app.tjenester.kodeverk;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursResourceAttributt.APPLIKASJON;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursResourceAttributt.FAGSAK;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
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
import no.nav.vedtak.felles.jpa.Transaction;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.util.LRUCache;

@Path("/kodeverk")
@ApplicationScoped
@Transaction
@Produces(MediaType.APPLICATION_JSON)
public class KodeverkRestTjeneste {

    private HentKodeverkTjeneste hentKodeverkTjeneste; // NOSONAR

    private final JacksonJsonConfig jsonMapper = new JacksonJsonConfig(true); // generere kodeverk med navn

    private final ObjectMapper objectMapper = jsonMapper.getObjectMapper();

    private static final long CACHE_ELEMENT_LIVE_TIME_MS = TimeUnit.MILLISECONDS.convert(60, TimeUnit.MINUTES);
    private LRUCache<String, String> kodelisteCache = new LRUCache<>(10, CACHE_ELEMENT_LIVE_TIME_MS);

    @Inject
    public KodeverkRestTjeneste(HentKodeverkTjeneste hentKodeverkTjeneste) {
        this.hentKodeverkTjeneste = hentKodeverkTjeneste;
    }

    public KodeverkRestTjeneste() {
        // for resteasy
    }

    @GET
    @Operation(description = "Henter kodeliste", tags = "kodeverk")
    @BeskyttetRessurs(action = READ, ressurs = APPLIKASJON, sporingslogg = false)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response hentGruppertKodeliste() throws IOException {
        String kodelisteJson = getKodeverkRawJson();
        CacheControl cc = new CacheControl();
        cc.setMaxAge(1 * 60); // tillater klient caching i 1 minutt
        return Response.ok()
            .entity(kodelisteJson)
            .type(MediaType.APPLICATION_JSON)
            .cacheControl(cc)
            .build();
    }

    @GET
    @Path("/behandlende-enheter")
    @Operation(description = "Henter liste over behandlende enheter", tags = "kodeverk")
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public List<OrganisasjonsEnhet> hentBehandlendeEnheter() {
        return hentKodeverkTjeneste.hentBehandlendeEnheter();
    }

    private String getKodeverkRawJson() throws JsonProcessingException {
        if (kodelisteCache.get("alle") == null) {
            kodelisteCache.put("alle", tilJson(this.hentGruppertKodelisteTilCache()));
        }
        String kodelisteJson = kodelisteCache.get("alle");
        return kodelisteJson;
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
