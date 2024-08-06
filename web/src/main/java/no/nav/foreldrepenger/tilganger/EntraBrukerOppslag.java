package no.nav.foreldrepenger.tilganger;

import java.net.URI;
import java.time.LocalDateTime;

import jakarta.ws.rs.core.UriBuilder;

import no.nav.vedtak.felles.integrasjon.rest.FpApplication;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

@RestClientConfig(tokenConfig = TokenFlow.ADAPTIVE, application = FpApplication.FPTILGANG)
class EntraBrukerOppslag {

    private final RestClient restClient;
    private final RestConfig restConfig;

    private final URI uri;

    EntraBrukerOppslag() {
        this.restClient = RestClient.client();
        this.restConfig = RestConfig.forClient(this.getClass());
        this.uri = UriBuilder.fromUri(restConfig.fpContextPath()).path("/api/bruker/profil/utvidet").build();
    }

    BrukerInfoResponseDto brukerInfo() {
        var request = RestRequest.newGET(UriBuilder.fromUri(uri).build(), restConfig);
        return restClient.send(request, BrukerInfoResponseDto.class);
    }

    record BrukerInfoResponseDto(String brukernavn,
                                        String fornavnEtternavn,
                                        boolean kanSaksbehandle,
                                        boolean kanVeilede,
                                        boolean kanOverstyre,
                                        boolean kanOppgavestyre,
                                        boolean kanBehandleKode6,
                                        LocalDateTime funksjonellTid) {
    }
}
