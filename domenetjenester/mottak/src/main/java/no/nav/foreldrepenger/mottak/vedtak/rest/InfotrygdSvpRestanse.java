package no.nav.foreldrepenger.mottak.vedtak.rest;

import java.util.Arrays;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import no.nav.vedtak.felles.integrasjon.rest.NavHeaders;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

// Brukes ikke av mottak, men av egen innsynsløsning / ui.
@ApplicationScoped
@RestClientConfig(tokenConfig = TokenFlow.AZUREAD_CC, endpointProperty = "fpsak.it.sv.sak.url",
    endpointDefault = "http://fp-infotrygd-svangerskapspenger/sak",
    scopesProperty = "fpsak.it.sv.scopes", scopesDefault = "api://prod-fss.teamforeldrepenger.fp-infotrygd-svangerskapspenger/.default")
public class InfotrygdSvpRestanse {

    private final RestClient restClient;
    private final RestConfig restConfig;

    public InfotrygdSvpRestanse() {
        this(RestClient.client());
    }

    private InfotrygdSvpRestanse(RestClient client) {
        this.restClient = client;
        this.restConfig = RestConfig.forClient(this.getClass());
    }

    public List<InfotrygdRestanseDto> getRestanse() {
        var request = RestRequest.newGET(restConfig.endpoint(), restConfig)
            .otherCallId(NavHeaders.HEADER_NAV_CALL_ID);
        var match = restClient.send(request, InfotrygdRestanseDto[].class);
        return Arrays.asList(match);
    }
}
