package no.nav.foreldrepenger.økonomistøtte.simulering.klient;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.UriBuilder;

import no.nav.foreldrepenger.økonomistøtte.simulering.kontrakt.SimulerOppdragDto;
import no.nav.foreldrepenger.økonomistøtte.simulering.kontrakt.SimuleringResultatDto;
import no.nav.vedtak.felles.integrasjon.rest.FpApplication;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

@ApplicationScoped
@RestClientConfig(tokenConfig = TokenFlow.ADAPTIVE, application = FpApplication.FPOPPDRAG, endpointProperty = "FPOPPDRAG_OVERRIDE_URL")  // Testformål
public class FpOppdragRestKlient {

    private static final String FPOPPDRAG_START_SIMULERING = "/api/simulering/start";
    private static final String FPOPPDRAG_HENT_RESULTAT = "/api/simulering/resultat";

    private RestClient restClient;
    private URI uriStartSimulering;
    private URI uriHentResultat;

    public FpOppdragRestKlient() {
        //for cdi proxy
    }

    @Inject
    public FpOppdragRestKlient(RestClient restClient) {
        this.restClient = restClient;
        var fpoppdragBaseUrl = RestConfig.contextPathFromAnnotation(FpOppdragRestKlient.class);
        this.uriStartSimulering = UriBuilder.fromUri(fpoppdragBaseUrl).path(FPOPPDRAG_START_SIMULERING).build();
        this.uriHentResultat = UriBuilder.fromUri(fpoppdragBaseUrl).path(FPOPPDRAG_HENT_RESULTAT).build();
    }

    /**
     * Starter en simulering for gitt behandling med oppdrag XMLer.
     * @param request med behandlingId og liste med oppdrag-XMLer
     */
    public void startSimulering(SimulerOppdragDto request) {
        var rrequest = RestRequest.newPOSTJson(request, uriStartSimulering, FpOppdragRestKlient.class).timeout(Duration.ofSeconds(30));
        restClient.sendReturnOptional(rrequest, String.class);
    }

    /**
     * Henter simuleringresultat for behandling hvis det finnes.
     * @param behandlingId
     * @return Optional med SimuleringResultatDto kan være tom
     */
    public Optional<SimuleringResultatDto> hentResultat(Long behandlingId) {
        var rrequest = RestRequest.newPOSTJson(new BehandlingIdDto(behandlingId), uriHentResultat, FpOppdragRestKlient.class);
        return restClient.sendReturnOptional(rrequest, SimuleringResultatDto.class);
    }

}
