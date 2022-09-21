package no.nav.foreldrepenger.domene.person.dkif;

import java.net.URI;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriBuilderException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.domene.person.krr.KrrSpråkKlient;
import no.nav.vedtak.felles.integrasjon.rest.NavHeaders;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

/*
 * Dokumentasjon Tjeneste for å hente digital kontaktinformasjon (mobil, epost, sdp og språkkode)
 * Swagger https://dkif.nais.preprod.local/swagger-ui.html#/Digital%20kontaktinformasjon/digitalKontaktinformasjonUsingGET
 */

@ApplicationScoped
@RestClientConfig(tokenConfig = TokenFlow.STS_CC, endpointProperty = "dkif.rs.url", endpointDefault = "http://dkif.default/api/v1/personer/kontaktinformasjon")
public class DkifSpråkKlient {

    private static final Logger LOG = LoggerFactory.getLogger(DkifSpråkKlient.class);

    public static final String HEADER_NAV_PERSONIDENT = "Nav-Personidenter";
    private KrrSpråkKlient krrSpråkKlient;

    private RestClient restClient;
    private URI endpoint;

    public DkifSpråkKlient() {
    }

    @Inject
    public DkifSpråkKlient(RestClient restClient,
                           KrrSpråkKlient krrSpråkKlient) {
        this.restClient = restClient;
        this.endpoint = UriBuilder.fromUri(RestConfig.endpointFromAnnotation(DkifSpråkKlient.class))
            .queryParam("inkluderSikkerDigitalPost", "false")
            .build();
        this.krrSpråkKlient = krrSpråkKlient;
    }

    public Språkkode finnSpråkkodeForBruker(String fnr) {
        try {
            var request = RestRequest.newGET(endpoint, DkifSpråkKlient.class)
                .header(NavHeaders.HEADER_NAV_PERSONIDENTER, fnr);
            var match = restClient.sendReturnOptional(request, DigitalKontaktinfo.class);
            var språkkode = match.flatMap(m -> m.getSpraak(fnr)).map(String::toUpperCase).map(Språkkode::defaultNorsk).orElse(Språkkode.NB);
            sammenlikneMedKrrKlient(språkkode, fnr);
            return språkkode;
        } catch (UriBuilderException|IllegalArgumentException e) {
            throw new IllegalArgumentException("Utviklerfeil syntax-exception for finnSpråkkodeForBruker");
        }
    }

    private void sammenlikneMedKrrKlient(Språkkode språkkode, String fnr) {
        try {
            var krrSpråkkode = krrSpråkKlient.finnSpråkkodeForBruker(fnr);
            if (krrSpråkkode == språkkode) {
                LOG.info("DkifSpråkKlient: sammenlikning dkif og krr gir likt resultat.");
            } else {
                LOG.info("DkifSpråkKlient: sammenlikning dkif og krr gir ulikt resultat.");
            }
        } catch (Exception e) {
            LOG.info("DkifSpråkKlient: krr gir exception", e);
        }
    }
}
