package no.nav.foreldrepenger.domene.person.dkif;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.http.Header;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicHeader;

import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.vedtak.felles.integrasjon.rest.OidcRestClient;
import no.nav.vedtak.konfig.KonfigVerdi;

/*
 * Dokumentasjon Tjeneste for å hente digital kontaktinformasjon (mobil, epost, sdp og språkkode)
 * Swagger https://dkif.nais.preprod.local/swagger-ui.html#/Digital%20kontaktinformasjon/digitalKontaktinformasjonUsingGET
 */

@ApplicationScoped
public class DkifSpråkKlient {

    private static final String ENDPOINT_KEY = "dkif.rs.url";
    private static final String DEFAULT_URI = "http://dkif.default/api/v1/personer/kontaktinformasjon";

    public static final String HEADER_NAV_PERSONIDENT = "Nav-Personidenter";

    private OidcRestClient oidcRestClient;
    private URI endpoint;

    public DkifSpråkKlient() {
    }

    @Inject
    public DkifSpråkKlient(OidcRestClient oidcRestClient,
                           @KonfigVerdi(value = ENDPOINT_KEY, defaultVerdi = DEFAULT_URI) URI endpoint) {
        this.oidcRestClient = oidcRestClient;
        this.endpoint = endpoint;
    }

    public Språkkode finnSpråkkodeForBruker(String fnr) {
        try {
            var request = new URIBuilder(endpoint)
                    .addParameter("inkluderSikkerDigitalPost", "false")
                    .build();
            DigitalKontaktinfo match = this.oidcRestClient.get(request, this.lagHeader(fnr), DigitalKontaktinfo.class);
            return Optional.ofNullable(match).flatMap(m -> m.getSpraak(fnr)).map(String::toUpperCase).map(Språkkode::defaultNorsk).orElse(Språkkode.NB);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Utviklerfeil syntax-exception for finnArbeidsforholdForArbeidstaker");
        }
    }

    private Set<Header> lagHeader(String fnr) {
        return Set.of(new BasicHeader(HEADER_NAV_PERSONIDENT, "List [ \"" + fnr + "\" ]"));
    }
}
