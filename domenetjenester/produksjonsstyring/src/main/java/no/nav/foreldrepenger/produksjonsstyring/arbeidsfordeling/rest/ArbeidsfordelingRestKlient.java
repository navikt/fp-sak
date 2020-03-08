package no.nav.foreldrepenger.produksjonsstyring.arbeidsfordeling.rest;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.LogLevel;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.IntegrasjonFeil;
import no.nav.vedtak.felles.integrasjon.rest.OidcRestClient;
import no.nav.vedtak.konfig.KonfigVerdi;

@ApplicationScoped
public class ArbeidsfordelingRestKlient {

    private static final String DEFAULT_URI = "https://app.adeo.no/norg2/api/v1/arbeidsfordeling/enheter";
    private static final String BEST_MATCH = "/bestmatch";

    private static final Logger LOG = LoggerFactory.getLogger(ArbeidsfordelingRestKlient.class);

    private OidcRestClient restClient;
    private URI alleEnheterUri;
    private URI besteEnhetUri;
    private String uriString;

    @Inject
    public ArbeidsfordelingRestKlient(OidcRestClient restClient,
                                      @KonfigVerdi(value = "arbeidsfordeling.rs.url", defaultVerdi = DEFAULT_URI) URI uri) {
        this.restClient = restClient;
        this.alleEnheterUri = uri;
        this.besteEnhetUri = URI.create(uri + BEST_MATCH);
        this.uriString = uri.toString();
    }

    ArbeidsfordelingRestKlient() {
    }

    public List<ArbeidsfordelingResponse> hentAlleAktiveEnheter(ArbeidsfordelingRequest request) {
        return hentEnheterFor(request, alleEnheterUri, "alle");
    }

    public List<ArbeidsfordelingResponse> finnEnhet(ArbeidsfordelingRequest request) {
        return hentEnheterFor(request, besteEnhetUri, "beste");
    }


    private List<ArbeidsfordelingResponse> hentEnheterFor(ArbeidsfordelingRequest request, URI uri, String logPart) {
        try {
            LOG.info("N2REST henter {} med {}", logPart, request);
            var respons = restClient.post(uri, request, ArbeidsfordelingResponse[].class);
            var resultat = Arrays.stream(respons)
                .filter(response -> "AKTIV".equalsIgnoreCase(response.getStatus()))
                .collect(Collectors.toList());
            LOG.info("N2REST henter {} respons {}", logPart, resultat);
            return resultat;
        } catch (Exception e) {
            LOG.info("N2REST - Feil ved oppslag mot {}, returnerer ingen enheter", uriString, e);
            //throw ArbeidsfordelingRestKlientFeil.FACTORY.feilfratjeneste(uriString).toException();
            return Collections.emptyList();
        }
    }

    interface ArbeidsfordelingRestKlientFeil extends DeklarerteFeil {
        ArbeidsfordelingRestKlientFeil FACTORY = FeilFactory.create(ArbeidsfordelingRestKlientFeil.class);

        @IntegrasjonFeil(feilkode = "F-016912", feilmelding = "Arbeidsfordeling feil ved oppslag mot %s", logLevel = LogLevel.WARN)
        Feil feilfratjeneste(String var1);
    }

}
