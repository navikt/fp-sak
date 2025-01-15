package no.nav.foreldrepenger.domene.fpinntektsmelding;

import java.net.URI;
import java.util.Objects;

import jakarta.enterprise.context.Dependent;
import jakarta.ws.rs.core.UriBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.integrasjon.rest.FpApplication;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

@Dependent
@RestClientConfig(tokenConfig = TokenFlow.ADAPTIVE, application = FpApplication.FPINNTEKTSMELDING)
public class FpinntektsmeldingKlient {
    private static final Logger LOG = LoggerFactory.getLogger(FpinntektsmeldingKlient.class);

    private final RestClient restClient;
    private final RestConfig restConfig;
    private final URI uriOpprettForesporsel;
    private final URI uriLukkForesporsel;
    private final URI uriOverstyrInntektsmelding;
    private final URI uriSettForesporselTilUtgaatt;
    private final URI uriSendNyBeskjedPåForespørsel;


    public FpinntektsmeldingKlient() {
        this.restClient = RestClient.client();
        this.restConfig = RestConfig.forClient(FpinntektsmeldingKlient.class);
        this.uriOpprettForesporsel = toUri(restConfig.fpContextPath(), "/api/foresporsel/opprett");
        this.uriLukkForesporsel = toUri(restConfig.fpContextPath(), "/api/foresporsel/lukk");
        this.uriOverstyrInntektsmelding = toUri(restConfig.fpContextPath(), "/api/overstyring/inntektsmelding");
        this.uriSettForesporselTilUtgaatt = toUri(restConfig.fpContextPath(), "/api/foresporsel/sett-til-utgatt");
        this.uriSendNyBeskjedPåForespørsel = toUri(restConfig.fpContextPath(), "/api/foresporsel/ny-beskjed");
    }

    public OpprettForespørselResponsNy opprettForespørsel(OpprettForespørselRequest request) {
        Objects.requireNonNull(request, "request");
        try {
            LOG.info("Sender request til fpinntektsmelding for saksnummer {} ", request.fagsakSaksnummer().saksnr());
            var rrequest = RestRequest.newPOSTJson(request, uriOpprettForesporsel, restConfig);
           return restClient.send(rrequest, OpprettForespørselResponsNy.class);
        } catch (Exception e) {
            LOG.warn("Feil ved overstyring av inntektsmelding med request: {}", request);
            throw feilVedKallTilFpinntektsmelding(e.getMessage());
        }
    }

    public void overstyrInntektsmelding(OverstyrInntektsmeldingRequest request) {
        Objects.requireNonNull(request, "request");
        try {
            LOG.info("Overstyrer inntektsmelding for arbeidsgiver {}", request.arbeidsgiverIdent().ident());
            var rrequest = RestRequest.newPOSTJson(request, uriOverstyrInntektsmelding, restConfig);
            restClient.send(rrequest, String.class);
        } catch (Exception e) {
            LOG.warn("Feil ved overstyring av inntektsmelding med request: {}", request);
            throw feilVedKallTilFpinntektsmelding(e.getMessage());
        }
    }

    public void lukkForespørsel(LukkForespørselRequest request) {
        Objects.requireNonNull(request, "request");
        try {
            LOG.info("Sender lukk forespørsel request til fpinntektsmelding for saksnummer {} med organisasjonsnummer {}", request.fagsakSaksnummer().saksnr(), request.orgnummer());
            var restRequest = RestRequest.newPOSTJson(request, uriLukkForesporsel, restConfig);
            restClient.send(restRequest, String.class);
        } catch (Exception e) {
            skrivTilLogg(request);
            throw feilVedKallTilFpinntektsmelding(e.getMessage());
        }
    }

    public void settForespørselTilUtgått(LukkForespørselRequest request) {
        Objects.requireNonNull(request, "request");
        try {
            LOG.info("Sender Lukk forespørsel request til fpinntektsmelding for å sette forespørsel til utgått for saksnummer {} ", request.fagsakSaksnummer().saksnr());
            var restRequest = RestRequest.newPOSTJson(request, uriSettForesporselTilUtgaatt, restConfig);
            restClient.send(restRequest, String.class);
        } catch (Exception e) {
            skrivTilLogg(request);
            throw feilVedKallTilFpinntektsmelding(e.getMessage());
        }
    }

    public SendNyBeskjedResponse sendNyBeskjedPåForespørsel(NyBeskjedRequest request) {
        Objects.requireNonNull(request, "request");
        try {
            LOG.info("Sender ny beskjed request til fpinntektsmelding for å legge til ny beskjed på eksisterende forespørsel for saksnummer {} ", request.fagsakSaksnummer().saksnr());
            var restRequest = RestRequest.newPOSTJson(request, uriSendNyBeskjedPåForespørsel, restConfig);
            return restClient.send(restRequest, SendNyBeskjedResponse.class);
        } catch (Exception e) {
            throw feilVedKallTilFpinntektsmelding(e.getMessage());
        }
    }

    private static TekniskException feilVedKallTilFpinntektsmelding(String feilmelding) {
        return new TekniskException("FP-97215", "Feil ved kall til Fpinntektsmelding: " + feilmelding);
    }

    private URI toUri(URI endpointURI, String path) {
        try {
            return UriBuilder.fromUri(endpointURI).path(path).build();
        } catch (Exception e) {
            throw new IllegalArgumentException("Ugyldig uri: " + endpointURI + path, e);
        }
    }

    private static void skrivTilLogg(LukkForespørselRequest request) {
        LOG.warn("Feil ved oversending til fpinntektsmelding med lukk forespørsel request: {}", request);
    }
}

