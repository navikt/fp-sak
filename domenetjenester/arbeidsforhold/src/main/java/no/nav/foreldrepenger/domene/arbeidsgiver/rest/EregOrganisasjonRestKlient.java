package no.nav.foreldrepenger.domene.arbeidsgiver.rest;

import java.net.URI;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.http.Header;
import org.apache.http.message.BasicHeader;

import no.nav.vedtak.felles.integrasjon.rest.OidcRestClient;
import no.nav.vedtak.konfig.KonfigVerdi;
import no.nav.vedtak.log.mdc.MDCOperations;

/*
 * Dokumentasjon https://confluence.adeo.no/display/FEL/EREG+-+Tjeneste+REST+ereg.api
 * Swagger https://modapp-q1.adeo.no/ereg/api/swagger-ui.html#/
 */

@ApplicationScoped
public class EregOrganisasjonRestKlient {

    private static final String ENDPOINT_KEY = "ereg.org.rs.url";
    private static final String DEFAULT_URI = "https://modapp.adeo.no/ereg/api/v1/organisasjon";

    public static final String HEADER_NAV_CALL_ID = "Nav-Call-Id";

    private OidcRestClient oidcRestClient;
    private URI endpoint;

    public EregOrganisasjonRestKlient() {
    }

    @Inject
    public EregOrganisasjonRestKlient(OidcRestClient oidcRestClient,
                                      @KonfigVerdi(value = ENDPOINT_KEY, defaultVerdi = DEFAULT_URI) URI endpoint) {
        this.oidcRestClient = oidcRestClient ;
        this.endpoint = endpoint;
    }

    public Organisasjon hentOrganisasjon(String orgnummer)  {
        var request = URI.create(endpoint.toString() + "/" + orgnummer);
        return oidcRestClient.get(request, lagHeader(), Organisasjon.class);
    }

    private Set<Header> lagHeader() {
        return Set.of(new BasicHeader(HEADER_NAV_CALL_ID, MDCOperations.getCallId()));
    }


}
