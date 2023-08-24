package no.nav.foreldrepenger.økonomistøtte.simulering.klient;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.UriBuilder;
import no.nav.foreldrepenger.kontrakter.fpwsproxy.error.FeilDto;
import no.nav.foreldrepenger.kontrakter.fpwsproxy.error.FeilType;
import no.nav.foreldrepenger.kontrakter.fpwsproxy.simulering.request.OppdragskontrollDto;
import no.nav.foreldrepenger.økonomistøtte.simulering.kontrakt.SimuleringResultatDto;
import no.nav.vedtak.exception.IntegrasjonException;
import no.nav.vedtak.exception.ManglerTilgangException;
import no.nav.vedtak.felles.integrasjon.rest.*;

import java.net.URI;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

import static java.net.HttpURLConnection.*;
import static no.nav.vedtak.mapper.json.DefaultJsonMapper.fromJson;

@ApplicationScoped
@RestClientConfig(tokenConfig = TokenFlow.ADAPTIVE, application = FpApplication.FPOPPDRAG)
public class FpOppdragRestKlient {

    private static final String FPOPPDRAG_HENT_RESULTAT = "/api/simulering/resultat";
    private static final String FPOPPDRAG_START_SIMULERING = "/api/simulering/start";

    private final RestClient restClient;
    private final RestConfig restConfig;
    private final URI uriStartSimulering;
    private final URI uriHentResultat;

    public FpOppdragRestKlient() {
        this.restClient = RestClient.client();
        this.restConfig = RestConfig.forClient(this.getClass());
        this.uriStartSimulering = UriBuilder.fromUri(restConfig.fpContextPath()).path(FPOPPDRAG_START_SIMULERING).build();
        this.uriHentResultat = UriBuilder.fromUri(restConfig.fpContextPath()).path(FPOPPDRAG_HENT_RESULTAT).build();
    }

    /**
     * Henter simuleringresultat for behandling hvis det finnes.
     * @param behandlingId
     * @return Optional med SimuleringResultatDto kan være tom
     */
    public Optional<SimuleringResultatDto> hentResultat(Long behandlingId) {
        var rrequest = RestRequest.newPOSTJson(new BehandlingIdDto(behandlingId), uriHentResultat, restConfig);
        return restClient.sendReturnOptional(rrequest, SimuleringResultatDto.class);
    }

    /**
     * Starter en simulering for gitt behandling med oppdrag fra oppdragskontroll
     * @param oppdragskontrollDto med OppdragskontrollDto
     */
    public void startSimulering(OppdragskontrollDto oppdragskontrollDto) {
        var request = RestRequest.newPOSTJson(oppdragskontrollDto, uriStartSimulering, restConfig).timeout(Duration.ofSeconds(30));
        handleResponse(restClient.sendReturnUnhandled(request));
    }

    private static void handleResponse(HttpResponse<String> response) {
        var status = response.statusCode();
        if (status >= HTTP_OK && status < HTTP_MULT_CHOICE) {
            return;
        }

        if (status == HTTP_FORBIDDEN) {
            throw new ManglerTilgangException("F-468816", "Mangler tilgang. Fikk http-kode 403 fra server");
        } else {
            if (status == HTTP_UNAVAILABLE && erDetForventetNedetid(response.body())) { // 503 med OPPDRAG_FORVENTET_NEDETID propages ved nedetid
                throw new OppdragForventetNedetidException();
            }
            throw new IntegrasjonException("F-468817", String.format("Uventet respons %s fra FpWsProxy. Sjekk loggen til fpwsproxy for mer info.", status));
        }
    }

    private static boolean erDetForventetNedetid(String body) {
        try {
            return FeilType.OPPDRAG_FORVENTET_NEDETID.equals(fromJson(body, FeilDto.class).type());
        } catch (Exception e) {
            return false;
        }
    }
}
