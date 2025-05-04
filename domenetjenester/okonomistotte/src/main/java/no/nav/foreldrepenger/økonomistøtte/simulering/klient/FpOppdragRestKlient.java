package no.nav.foreldrepenger.økonomistøtte.simulering.klient;

import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_MULT_CHOICE;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAVAILABLE;
import static no.nav.vedtak.mapper.json.DefaultJsonMapper.fromJson;

import java.net.URI;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.UriBuilder;

import no.nav.foreldrepenger.kontrakter.fpwsproxy.error.FeilDto;
import no.nav.foreldrepenger.kontrakter.fpwsproxy.error.FeilType;
import no.nav.foreldrepenger.kontrakter.fpwsproxy.simulering.request.OppdragskontrollDto;
import no.nav.foreldrepenger.kontrakter.fpwsproxy.simulering.request.StartSimuleringRequest;
import no.nav.foreldrepenger.kontrakter.simulering.resultat.request.SimuleringResultatRequest;
import no.nav.foreldrepenger.kontrakter.simulering.resultat.v1.SimuleringDto;
import no.nav.foreldrepenger.kontrakter.simulering.resultat.v1.SimuleringResultatDto;
import no.nav.vedtak.exception.IntegrasjonException;
import no.nav.vedtak.exception.ManglerTilgangException;
import no.nav.vedtak.felles.integrasjon.rest.FpApplication;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

@ApplicationScoped
@RestClientConfig(tokenConfig = TokenFlow.AZUREAD_CC, application = FpApplication.FPOPPDRAG) // AzureCC pga kun les, kun pdl.getIdent kalles ut.
public class FpOppdragRestKlient {

    private static final String FPOPPDRAG_HENT_RESULTAT = "/api/simulering/resultat";
    private static final String FPOPPDRAG_HENT_RESULTAT_GUI = "/api/simulering/resultat-uten-inntrekk";
    private static final String FPOPPDRAG_START_SIMULERING = "/api/simulering/start-v2";
    private static final String FPOPPDRAG_KANSELLER_SIMULERING = "/api/simulering/kanseller";

    private final RestClient restClient;
    private final RestConfig restConfig;
    private final URI uriStartSimulering;
    private final URI uriHentResultat;
    private final URI uriHentResultatGui;
    private final URI uriKansellerSimulering;

    public FpOppdragRestKlient() {
        this.restClient = RestClient.client();
        this.restConfig = RestConfig.forClient(this.getClass());
        this.uriStartSimulering = UriBuilder.fromUri(restConfig.fpContextPath()).path(FPOPPDRAG_START_SIMULERING).build();
        this.uriHentResultat = UriBuilder.fromUri(restConfig.fpContextPath()).path(FPOPPDRAG_HENT_RESULTAT).build();
        this.uriHentResultatGui = UriBuilder.fromUri(restConfig.fpContextPath()).path(FPOPPDRAG_HENT_RESULTAT_GUI).build();
        this.uriKansellerSimulering = UriBuilder.fromUri(restConfig.fpContextPath()).path(FPOPPDRAG_KANSELLER_SIMULERING).build();
    }

    /**
     * Henter simuleringresultat for behandling hvis det finnes.
     * @param behandlingId
     * @return Optional med SimuleringResultatDto kan være tom
     */
    public Optional<SimuleringResultatDto> hentResultat(Long behandlingId, UUID behandlingUuid, String saksnummer) {
        var rrequest = RestRequest.newPOSTJson(new SimuleringResultatRequest(behandlingId, behandlingUuid, saksnummer), uriHentResultat, restConfig);
        return restClient.sendReturnOptional(rrequest, SimuleringResultatDto.class);
    }

    public Optional<SimuleringDto> hentSimuleringResultatMedOgUtenInntrekk(Long behandlingId, UUID behandlingUuid, String saksnummer) {
        var rrequest = RestRequest.newPOSTJson(new SimuleringResultatRequest(behandlingId, behandlingUuid, saksnummer), uriHentResultatGui, restConfig);
        return restClient.sendReturnOptional(rrequest, SimuleringDto.class);
    }

    /**
     * Starter en simulering for gitt behandling med oppdrag fra oppdragskontroll
     * @param oppdragskontrollDto med OppdragskontrollDto
     */
    public void startSimulering(OppdragskontrollDto oppdragskontrollDto, UUID behandlingUuid, String saksnummer) {
        var utvidet = new StartSimuleringRequest(oppdragskontrollDto, behandlingUuid, saksnummer);
        var request = RestRequest.newPOSTJson(utvidet, uriStartSimulering, restConfig).timeout(Duration.ofSeconds(30));
        handleResponse(restClient.sendReturnUnhandled(request));
    }

    public void kansellerSimulering(Long behandlingId, UUID behandlingUuid, String saksnummer) {
        var request = RestRequest.newPOSTJson(new SimuleringResultatRequest(behandlingId, behandlingUuid, saksnummer), uriKansellerSimulering, restConfig);
        restClient.sendReturnOptional(request, String.class);
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
