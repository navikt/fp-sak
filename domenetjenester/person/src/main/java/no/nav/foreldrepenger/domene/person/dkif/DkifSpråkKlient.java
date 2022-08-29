package no.nav.foreldrepenger.domene.person.dkif;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.domene.person.krr.KrrSpråkKlient;

import org.apache.http.Header;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicHeader;

import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.vedtak.felles.integrasjon.rest.StsSystemRestKlient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Dokumentasjon Tjeneste for å hente digital kontaktinformasjon (mobil, epost, sdp og språkkode)
 * Swagger https://dkif.nais.preprod.local/swagger-ui.html#/Digital%20kontaktinformasjon/digitalKontaktinformasjonUsingGET
 */

@ApplicationScoped
public class DkifSpråkKlient {

    private static final Logger LOG = LoggerFactory.getLogger(DkifSpråkKlient.class);

    private static final String ENDPOINT_KEY = "dkif.rs.url";
    private static final String DEFAULT_URI = "http://dkif.default/api/v1/personer/kontaktinformasjon";

    public static final String HEADER_NAV_PERSONIDENT = "Nav-Personidenter";
    private KrrSpråkKlient krrSpråkKlient;

    private StsSystemRestKlient oidcRestClient;
    private URI endpoint;

    public DkifSpråkKlient() {
    }

    @Inject
    public DkifSpråkKlient(StsSystemRestKlient oidcRestClient,
                           @KonfigVerdi(value = ENDPOINT_KEY, defaultVerdi = DEFAULT_URI) URI endpoint,
                           KrrSpråkKlient krrSpråkKlient) {
        this.oidcRestClient = oidcRestClient;
        this.endpoint = endpoint;
        this.krrSpråkKlient = krrSpråkKlient;
    }

    public Språkkode finnSpråkkodeForBruker(String fnr) {
        try {
            var request = new URIBuilder(endpoint)
                    .addParameter("inkluderSikkerDigitalPost", "false")
                    .build();
            var match = this.oidcRestClient.get(request, this.lagHeader(fnr), DigitalKontaktinfo.class);
            var språkkode = Optional.ofNullable(match).flatMap(m -> m.getSpraak(fnr)).map(String::toUpperCase).map(Språkkode::defaultNorsk).orElse(Språkkode.NB);
            sammenlikneMedKrrKlient(språkkode, fnr);
            return språkkode;
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Utviklerfeil syntax-exception for finnSpråkkodeForBruker");
        }
    }

    private Set<Header> lagHeader(String fnr) {
        return Set.of(new BasicHeader(HEADER_NAV_PERSONIDENT, fnr));
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
