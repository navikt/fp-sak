package no.nav.foreldrepenger.poststed;

import java.net.URI;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.UriBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

@ApplicationScoped
@RestClientConfig(tokenConfig = TokenFlow.CONTEXT, endpointProperty = "kodeverk.base.url", endpointDefault = "http://kodeverk.org/")
public class KodeverkTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(KodeverkTjeneste.class);

    private RestClient restClient;
    private URI kodeverkBaseEndpoint;

    private static final String SERVICE_PATH = "/api/v1/kodeverk";
    private static final String CONTENT_PATH = "/koder/betydninger";
    private static final String LANG_PARAM = "spraak";
    private static final String NORSK_BOKMÅL = "nb";

    KodeverkTjeneste() {
        // for CDI proxy
    }

    @Inject
    public KodeverkTjeneste(RestClient restClient) {
        this.restClient = restClient;
        this.kodeverkBaseEndpoint = RestConfig.endpointFromAnnotation(KodeverkTjeneste.class);
    }

    public Map<String, KodeverkBetydning> hentKodeverkBetydninger(String kodeverk) {
        Map<String, KodeverkBetydning> resultatMap = new LinkedHashMap<>();
        var uri = UriBuilder.fromUri(kodeverkBaseEndpoint)
            .path(SERVICE_PATH).path(kodeverk).path(CONTENT_PATH)
            .queryParam(LANG_PARAM, NORSK_BOKMÅL)
            .build();
        try {
            var request = RestRequest.newGET(uri, KodeverkTjeneste.class);
            var response = restClient.sendReturnOptional(request, KodeverkBetydninger.class);

            response.map(KodeverkBetydninger::betydninger).orElse(Map.of())
                .forEach((key, value) -> {
                    var ferskest = value.size() == 1 ? Optional.of(value.get(0)) :
                        value.stream().max(Comparator.comparing(KodeInnslag::gyldigTil).thenComparing(KodeInnslag::gyldigFra));
                    ferskest
                        .filter(f -> Optional.ofNullable(f.beskrivelser()).map(b -> b.get(NORSK_BOKMÅL)).map(TermTekst::term).isPresent())
                        .ifPresent(f -> resultatMap.put(key, new KodeverkBetydning(f.gyldigFra(), f.gyldigTil(), f.beskrivelser().get(NORSK_BOKMÅL).term())));
                });
        } catch (Exception e) {
            LOG.warn("Kunne ikke synkronisere kodeverk {}", kodeverk, e);
        }
        return resultatMap;
    }

    private static record TermTekst(String term) {}
    private static record KodeInnslag(LocalDate gyldigFra, LocalDate gyldigTil, Map<String, TermTekst> beskrivelser) {}
    private static record KodeverkBetydninger(Map<String, List<KodeInnslag>> betydninger) {}

    public static record KodeverkBetydning(LocalDate gyldigFra, LocalDate gyldigTil, String term) {}
}
