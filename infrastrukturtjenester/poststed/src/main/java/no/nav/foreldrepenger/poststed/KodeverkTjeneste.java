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

import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.vedtak.felles.integrasjon.rest.OidcRestClient;

@ApplicationScoped
public class KodeverkTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(KodeverkTjeneste.class);
    private static final String KODEVERK_URL = "http://kodeverk.org/";

    private OidcRestClient restClient;
    private URI kodeverkBaseEndpoint;

    private static final String SERVICE_PATH = "/api/v1/kodeverk";
    private static final String CONTENT_PATH = "/koder/betydninger";
    private static final String LANG_PARAM = "spraak";
    private static final String NORSK_BOKMÅL = "nb";

    KodeverkTjeneste() {
        // for CDI proxy
    }

    @Inject
    public KodeverkTjeneste(OidcRestClient restClient, @KonfigVerdi(value = "kodeverk.base.url", defaultVerdi = KODEVERK_URL) URI kodeverkUri) {
        this.kodeverkBaseEndpoint = kodeverkUri;
        this.restClient = restClient;
    }

    public Map<String, KodeverkBetydning> hentKodeverkBetydninger(String kodeverk) {
        Map<String, KodeverkBetydning> resultatMap = new LinkedHashMap<>();
        var request = UriBuilder.fromUri(kodeverkBaseEndpoint)
            .path(SERVICE_PATH).path(kodeverk).path(CONTENT_PATH)
            .queryParam(LANG_PARAM, NORSK_BOKMÅL)
            .build();
        try {
            var response = restClient.get(request, KodeverkBetydninger.class);

            if (response != null) {
                response.betydninger().forEach((key, value) -> {
                    var ferskest = value.size() == 1 ? Optional.of(value.get(0)) :
                        value.stream().max(Comparator.comparing(KodeInnslag::gyldigTil).thenComparing(KodeInnslag::gyldigFra));
                    ferskest
                        .filter(f -> Optional.ofNullable(f.beskrivelser()).map(b -> b.get(NORSK_BOKMÅL)).map(TermTekst::term).isPresent())
                        .ifPresent(f -> resultatMap.put(key, new KodeverkBetydning(f.gyldigFra(), f.gyldigTil(), f.beskrivelser().get(NORSK_BOKMÅL).term())));
                });
            }
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
