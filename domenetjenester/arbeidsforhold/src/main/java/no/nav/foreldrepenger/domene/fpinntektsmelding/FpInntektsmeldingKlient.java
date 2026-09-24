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
public class FpInntektsmeldingKlient {
    private static final Logger LOG = LoggerFactory.getLogger(FpInntektsmeldingKlient.class);

    private static final String REQUEST = "request";

    private final RestClient restClient;
    private final RestConfig restConfig;
    private final URI uriOpprettForesporsel;
    private final URI uriOpprettForesporselKomplett;
    private final URI uriOpprettSpesifikkForesporsel;
    private final URI uriLukkForesporsel;
    private final URI uriOverstyrInntektsmelding;
    private final URI uriSettForesporselTilUtgaatt;
    private final URI uriSendNyBeskjedPåForespørsel;


    public FpInntektsmeldingKlient() {
        this.restClient = RestClient.client();
        this.restConfig = RestConfig.forClient(FpInntektsmeldingKlient.class);
        this.uriOpprettForesporsel = toUri(restConfig.fpContextPath(), "/api/foresporsel/opprett");
        this.uriOpprettForesporselKomplett = toUri(restConfig.fpContextPath(), "/api/foresporsel/opprett-komplett");
        this.uriOpprettSpesifikkForesporsel = toUri(restConfig.fpContextPath(), "/api/foresporsel/opprett-en");
        this.uriLukkForesporsel = toUri(restConfig.fpContextPath(), "/api/foresporsel/lukk");
        this.uriOverstyrInntektsmelding = toUri(restConfig.fpContextPath(), "/api/overstyring/inntektsmelding");
        this.uriSettForesporselTilUtgaatt = toUri(restConfig.fpContextPath(), "/api/foresporsel/sett-til-utgatt");
        this.uriSendNyBeskjedPåForespørsel = toUri(restConfig.fpContextPath(), "/api/foresporsel/ny-beskjed");
    }

    public OpprettForespørselRespons opprettForespørsel(OpprettForespørselRequest opprettForespørselRequest) {
        Objects.requireNonNull(opprettForespørselRequest, REQUEST);
        try {
            LOG.info("Sender request til fpinntektsmelding for saksnummer {} ", opprettForespørselRequest.fagsakSaksnummer().saksnr());
            var request = RestRequest.newPOSTJson(opprettForespørselRequest, uriOpprettForesporsel, restConfig);
           return restClient.send(request, OpprettForespørselRespons.class);
        } catch (Exception e) {
            LOG.warn("Feil ved opprettelse av inntektsmelding med request: {}", opprettForespørselRequest);
            throw feilVedKallTilFpinntektsmelding(e.getMessage());
        }
    }

    public OpprettForespørselRespons opprettForespørselKomplett(OpprettKomplettForespørslerRequest opprettForespørselRequest) {
        Objects.requireNonNull(opprettForespørselRequest, REQUEST);
        try {
            LOG.info("Sender request med komplett liste over forespørsler til fpinntektsmelding for saksnummer {} ", opprettForespørselRequest.fagsakSaksnummer().saksnr());
            var request = RestRequest.newPOSTJson(opprettForespørselRequest, uriOpprettForesporselKomplett, restConfig);
            return restClient.send(request, OpprettForespørselRespons.class);
        } catch (Exception e) {
            LOG.warn("Feil ved opprettelse av inntektsmelding med request: {}", opprettForespørselRequest);
            throw feilVedKallTilFpinntektsmelding(e.getMessage());
        }
    }

    public OpprettForespørselRespons.OrganisasjonsnummerMedStatus opprettSpesifikkForespørsel(OpprettEnForespørselRequest opprettForespørselRequest) {
        Objects.requireNonNull(opprettForespørselRequest, REQUEST);
        try {
            LOG.info("Sender request med enkel bestilling av forespørsel til fpinntektsmelding for saksnummer {} ", opprettForespørselRequest.fagsakSaksnummer().saksnr());
            var request = RestRequest.newPOSTJson(opprettForespørselRequest, uriOpprettSpesifikkForesporsel, restConfig);
            return restClient.send(request, OpprettForespørselRespons.OrganisasjonsnummerMedStatus.class);
        } catch (Exception e) {
            LOG.warn("Feil ved opprettelse av inntektsmelding med request: {}", opprettForespørselRequest);
            throw feilVedKallTilFpinntektsmelding(e.getMessage());
        }
    }

    public void overstyrInntektsmelding(OverstyrInntektsmeldingRequest overstyrInntektsmeldingRequest) {
        Objects.requireNonNull(overstyrInntektsmeldingRequest, REQUEST);
        try {
            var request = RestRequest.newPOSTJson(overstyrInntektsmeldingRequest, uriOverstyrInntektsmelding, restConfig);
            restClient.send(request, String.class);
        } catch (Exception e) {
            LOG.warn("Feil ved overstyring av inntektsmelding med request: {}", overstyrInntektsmeldingRequest);
            throw feilVedKallTilFpinntektsmelding(e.getMessage());
        }
    }

    public void lukkForespørsel(LukkForespørselRequest lukkForespørselRequest) {
        Objects.requireNonNull(lukkForespørselRequest, REQUEST);
        try {
            LOG.info("Sender lukk forespørsel request til fpinntektsmelding for saksnummer {} med organisasjonsnummer {}", lukkForespørselRequest.fagsakSaksnummer().saksnr(), lukkForespørselRequest.orgnummer());
            var request = RestRequest.newPOSTJson(lukkForespørselRequest, uriLukkForesporsel, restConfig);
            restClient.send(request, String.class);
        } catch (Exception e) {
            skrivTilLogg(lukkForespørselRequest);
            throw feilVedKallTilFpinntektsmelding(e.getMessage());
        }
    }

    public void settForespørselTilUtgått(LukkForespørselRequest lukkForespørselRequest) {
        Objects.requireNonNull(lukkForespørselRequest, REQUEST);
        try {
            LOG.info("Sender Lukk forespørsel request til fpinntektsmelding for å sette forespørsel til utgått for saksnummer {} ", lukkForespørselRequest.fagsakSaksnummer().saksnr());
            var request = RestRequest.newPOSTJson(lukkForespørselRequest, uriSettForesporselTilUtgaatt, restConfig);
            restClient.send(request, String.class);
        } catch (Exception e) {
            skrivTilLogg(lukkForespørselRequest);
            throw feilVedKallTilFpinntektsmelding(e.getMessage());
        }
    }

    public SendNyBeskjedResponse sendNyBeskjedPåForespørsel(NyBeskjedRequest nyBeskjedRequest) {
        Objects.requireNonNull(nyBeskjedRequest, REQUEST);
        try {
            LOG.info("Sender ny beskjed request til fpinntektsmelding for å legge til ny beskjed på eksisterende forespørsel for saksnummer {} ", nyBeskjedRequest.fagsakSaksnummer().saksnr());
            var request = RestRequest.newPOSTJson(nyBeskjedRequest, uriSendNyBeskjedPåForespørsel, restConfig);
            return restClient.send(request, SendNyBeskjedResponse.class);
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

    private static void skrivTilLogg(LukkForespørselRequest lukkForespørselRequest) {
        LOG.warn("Feil ved oversending til fpinntektsmelding med lukk forespørsel request: {}", lukkForespørselRequest);
    }
}
