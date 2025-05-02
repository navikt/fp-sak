package no.nav.foreldrepenger.domene.abakus;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

import no.nav.abakus.iaygrunnlag.JsonObjectMapper;
import no.nav.abakus.iaygrunnlag.UuidDto;
import no.nav.abakus.iaygrunnlag.arbeidsforhold.v1.ArbeidsforholdDto;
import no.nav.abakus.iaygrunnlag.inntektsmelding.v1.InntektsmeldingerDto;
import no.nav.abakus.iaygrunnlag.request.AktørDatoRequest;
import no.nav.abakus.iaygrunnlag.request.AvsluttKoblingRequest;
import no.nav.abakus.iaygrunnlag.request.InnhentRegisterdataRequest;
import no.nav.abakus.iaygrunnlag.request.InntektArbeidYtelseGrunnlagRequest;
import no.nav.abakus.iaygrunnlag.request.InntektsmeldingerMottattRequest;
import no.nav.abakus.iaygrunnlag.request.InntektsmeldingerRequest;
import no.nav.abakus.iaygrunnlag.request.KopierGrunnlagRequest;
import no.nav.abakus.iaygrunnlag.request.OppgittOpptjeningMottattRequest;
import no.nav.abakus.iaygrunnlag.request.OverstyrGrunnlagRequest;
import no.nav.abakus.iaygrunnlag.v1.InntektArbeidYtelseGrunnlagDto;
import no.nav.abakus.vedtak.ytelse.Aktør;
import no.nav.abakus.vedtak.ytelse.Periode;
import no.nav.abakus.vedtak.ytelse.Ytelse;
import no.nav.abakus.vedtak.ytelse.Ytelser;
import no.nav.abakus.vedtak.ytelse.request.VedtakForPeriodeRequest;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.integrasjon.rest.FpApplication;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;
import no.nav.vedtak.konfig.Tid;

@ApplicationScoped
@RestClientConfig(tokenConfig = TokenFlow.ADAPTIVE, application = FpApplication.FPABAKUS, scopesProperty = "abakus.scopes")
public class AbakusTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(AbakusTjeneste.class);
    private final ObjectMapper iayMapper = JsonObjectMapper.getMapper();
    private final ObjectWriter iayJsonWriter = iayMapper.writerWithDefaultPrettyPrinter();
    private final ObjectReader iayGrunnlagReader = iayMapper.readerFor(InntektArbeidYtelseGrunnlagDto.class);
    private final ObjectReader arbeidsforholdReader = iayMapper.readerFor(ArbeidsforholdDto[].class);
    private final ObjectReader uuidReader = iayMapper.readerFor(UuidDto.class);
    private final ObjectReader inntektsmeldingerReader = iayMapper.readerFor(InntektsmeldingerDto.class);
    private final URI innhentRegisterdata;
    private final RestClient restClient;
    private final RestConfig restConfig;
    private final URI endpointGrunnlag;
    private final URI endpointMottaInntektsmeldinger;
    private final URI endpointMottaOppgittOpptjening;
    private final URI endpointKopierGrunnlag;
    private final URI endpointKopierGrunnlagBeholdIM;
    private final URI endpointInntektsmeldinger;
    private final URI endpointYtelser;
    private final URI endpointOverstyring;
    private final URI endpointLagreOverstyrtOppgittOpptjening;
    private final URI endpointLagreOppgittOpptjeningNullstillOverstyring;
    private final URI endpointArbeidsforholdMedPermisjonerIPeriode;
    private final URI endpointAvsluttKobling;

    @Inject
    public AbakusTjeneste() {
        this.restClient = RestClient.client();
        this.restConfig = RestConfig.forClient(AbakusTjeneste.class);
        this.innhentRegisterdata = toUri("/api/registerdata/v1/innhent/async");
        this.endpointGrunnlag = toUri("/api/iay/grunnlag/v1/");
        this.endpointKopierGrunnlag = toUri("/api/iay/grunnlag/v1/kopier");
        this.endpointKopierGrunnlagBeholdIM = toUri("/api/iay/grunnlag/v1/kopier-behold-im");
        this.endpointOverstyring = toUri("/api/iay/grunnlag/v1/overstyr-grunnlag");
        this.endpointMottaInntektsmeldinger = toUri("/api/iay/inntektsmeldinger/v1/motta");
        this.endpointInntektsmeldinger = toUri("/api/iay/inntektsmeldinger/v1/hentAlle");
        this.endpointMottaOppgittOpptjening = toUri("/api/iay/oppgitt/v1/motta");
        this.endpointLagreOppgittOpptjeningNullstillOverstyring = toUri("/api/iay/oppgitt/v1/motta-og-nullstill-overstyring");
        this.endpointLagreOverstyrtOppgittOpptjening = toUri("/api/iay/oppgitt/v1/overstyr");
        this.endpointArbeidsforholdMedPermisjonerIPeriode = toUri("/api/arbeidsforhold/v1/arbeidstakerMedPermisjoner");
        this.endpointAvsluttKobling = toUri("/api/kobling/v1/avslutt");
        this.endpointYtelser = toUri("/api/ytelse/v1/hent-vedtak-ytelse");
    }

    private URI toUri(String relativeUri) {
        var uri = restConfig.fpContextPath().toString() + relativeUri;
        try {
            return new URI(uri);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Ugyldig uri: " + uri, e);
        }
    }

    public UuidDto innhentRegisterdata(InnhentRegisterdataRequest request) {
        var endpoint = innhentRegisterdata;

        var responseHandler = new AbakusResponseHandler<UuidDto>(uuidReader);
        try {
            var json = iayJsonWriter.writeValueAsString(request);

            return hentFraAbakus(endpoint, responseHandler, json);
        } catch (JsonProcessingException e) {
            throw feilVedJsonParsing(e.getMessage());
        } catch (IOException e) {
            throw feilVedKallTilAbakus(e.getMessage());
        }
    }

    public InntektArbeidYtelseGrunnlagDto hentGrunnlag(InntektArbeidYtelseGrunnlagRequest request) throws IOException {
        var endpoint = endpointGrunnlag;
        var responseHandler = new AbakusResponseHandler<InntektArbeidYtelseGrunnlagDto>(iayGrunnlagReader);
        var json = iayJsonWriter.writeValueAsString(request);

        return hentFraAbakus(endpoint, responseHandler, json);
    }

    public List<ArbeidsforholdDto> hentArbeidsforholdIPeriodenMedAvtalerOgPermisjoner(AktørDatoRequest request) {
        return hentArbeidsforholdFraEndepunkt(request, endpointArbeidsforholdMedPermisjonerIPeriode);
    }

    private List<ArbeidsforholdDto> hentArbeidsforholdFraEndepunkt(AktørDatoRequest request, URI endpoint) {
        var responseHandler = new AbakusResponseHandler<ArbeidsforholdDto[]>(arbeidsforholdReader);
        try {
            var json = iayJsonWriter.writeValueAsString(request);
            var arbeidsforhold = hentFraAbakus(endpoint, responseHandler, json);
            if (arbeidsforhold == null) {
                return Collections.emptyList();
            }
            return Arrays.asList(arbeidsforhold);
        } catch (JsonProcessingException e) {
            throw feilVedJsonParsing(e.getMessage());
        } catch (IOException e) {
            throw feilVedKallTilAbakus(e.getMessage());
        }
    }

    public InntektsmeldingerDto hentUnikeUnntektsmeldinger(InntektsmeldingerRequest request) throws IOException {
        var endpoint = endpointInntektsmeldinger;
        var responseHandler = new AbakusResponseHandler<InntektsmeldingerDto>(inntektsmeldingerReader);
        var json = iayJsonWriter.writeValueAsString(request);

        return hentFraAbakus(endpoint, responseHandler, json);
    }

    public static VedtakForPeriodeRequest lagRequestForHentVedtakFom(AktørId aktørId, LocalDate fom, Set<Ytelser> ytelser) {
        var aktør = new Aktør();
        aktør.setVerdi(aktørId.getId());
        var periode = new Periode();
        periode.setFom(fom);
        periode.setTom(Tid.TIDENES_ENDE);
        return new VedtakForPeriodeRequest(aktør, periode, ytelser);
    }

    public List<Ytelse> hentVedtakForAktørId(VedtakForPeriodeRequest request) {
        var endpoint = endpointYtelser;
        var reader = iayMapper.readerFor(Ytelse[].class);
        var responseHandler = new AbakusResponseHandler<Ytelse[]>(reader);

        try {
            var json = iayJsonWriter.writeValueAsString(request);
            var ytelser = hentFraAbakus(endpoint, responseHandler, json);
            if (ytelser == null) {
                return Collections.emptyList();
            }
            return Arrays.asList(ytelser);
        } catch (JsonProcessingException e) {
            throw feilVedJsonParsing(e.getMessage());
        } catch (IOException e) {
            throw feilVedKallTilAbakus(e.getMessage());
        }
    }

    private <T> T hentFraAbakus(URI endpoint, AbakusResponseHandler<T> responseHandler, String json) throws IOException {
        var method = new RestRequest.Method(RestRequest.WebMethod.POST, HttpRequest.BodyPublishers.ofString(json));
        var request = RestRequest.newRequest(method, endpoint, restConfig);

        try {
            var rawResponse = restClient.sendReturnUnhandled(request);
            var responseCode = rawResponse.statusCode();
            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                return responseHandler.handleResponse(rawResponse);
            }
            if (Set.of(HttpURLConnection.HTTP_NOT_MODIFIED, HttpURLConnection.HTTP_NO_CONTENT, HttpURLConnection.HTTP_ACCEPTED).contains(responseCode)) {
                return null;
            }
            var responseBody = rawResponse.body();
            var feilmelding = String.format("Kunne ikke hente grunnlag fra abakus: %s, HTTP status=%s. HTTP Errormessage=%s",
                endpoint, rawResponse.statusCode(), responseBody);
            if (responseCode == HttpURLConnection.HTTP_BAD_REQUEST) {
                throw feilKallTilAbakus(feilmelding);
            }
            throw feilVedKallTilAbakus(feilmelding);
        } catch (RuntimeException re) {
            LOG.warn("Feil ved henting av data fra abakus: endpoint={}", endpoint, re);
            throw re;
        }
    }

    private static final class AbakusResponseHandler<T>  {

        private final ObjectReader reader;

        public AbakusResponseHandler(ObjectReader reader) {
            this.reader = reader;
        }

        public T handleResponse(final HttpResponse<String> response) throws IOException {
            var body = response.body();
            return body != null ? reader.readValue(body) : null;
        }
    }

    public void lagreOverstyrtGrunnlag(OverstyrGrunnlagRequest overstyrtDto) throws IOException {
        var json = iayJsonWriter.writeValueAsString(overstyrtDto);

        var method = new RestRequest.Method(RestRequest.WebMethod.PUT, HttpRequest.BodyPublishers.ofString(json));
        var request = RestRequest.newRequest(method, endpointOverstyring, restConfig);

        LOG.info("Lagre IAY grunnlag (behandlingUUID={}) i Abakus", overstyrtDto.getKoblingReferanse());
        var rawResponse = restClient.sendReturnUnhandled(request);
        var responseCode = rawResponse.statusCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            var responseBody = rawResponse.body();
            var feilmelding = String.format("Kunne ikke lagre overstyrt IAY grunnlag: %s til abakus: %s, HTTP status=%s. HTTP Errormessage=%s",
                overstyrtDto.getGrunnlagReferanse(), endpointOverstyring, responseCode, responseBody);

            if (responseCode == HttpURLConnection.HTTP_BAD_REQUEST) {
                throw feilKallTilAbakus(feilmelding);
            }
            throw feilVedKallTilAbakus(feilmelding);
        }

    }

    public void lagreInntektsmeldinger(InntektsmeldingerMottattRequest dto) throws IOException {

        var json = iayJsonWriter.writeValueAsString(dto);

        var method = new RestRequest.Method(RestRequest.WebMethod.POST, HttpRequest.BodyPublishers.ofString(json));
        var request = RestRequest.newRequest(method, endpointMottaInntektsmeldinger, restConfig).timeout(Duration.ofSeconds(30));

        LOG.info("Lagre mottatte inntektsmeldinger (behandlingUUID={}) i Abakus", dto.getKoblingReferanse());
        var rawResponse = restClient.sendReturnUnhandled(request);
        var responseCode = rawResponse.statusCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            var responseBody = rawResponse.body();
            var feilmelding = String.format("Kunne ikke lagre mottatte inntektsmeldinger for behandling: %s til abakus: %s, HTTP status=%s. HTTP Errormessage=%s",
                dto.getKoblingReferanse(), endpointMottaInntektsmeldinger, responseCode, responseBody);
            if (responseCode == HttpURLConnection.HTTP_BAD_REQUEST) {
                throw feilKallTilAbakus(feilmelding);
            }
            throw feilVedKallTilAbakus(feilmelding);
        }
    }

    public void lagreOppgittOpptjening(OppgittOpptjeningMottattRequest dto) throws IOException {
        var json = iayJsonWriter.writeValueAsString(dto);

        var method = new RestRequest.Method(RestRequest.WebMethod.POST, HttpRequest.BodyPublishers.ofString(json));
        var request = RestRequest.newRequest(method, endpointMottaOppgittOpptjening, restConfig).timeout(Duration.ofSeconds(30));

        LOG.info("Lagre oppgitt opptjening (behandlingUUID={}) i Abakus", dto.getKoblingReferanse());
        var rawResponse = restClient.sendReturnUnhandled(request);
        var responseCode = rawResponse.statusCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            var responseBody = rawResponse.body();
            var feilmelding = String.format("Kunne ikke lagre oppgitt opptjening for behandling: %s til abakus: %s, HTTP status=%s. HTTP Errormessage=%s",
                dto.getKoblingReferanse(), endpointMottaOppgittOpptjening, responseCode, responseBody);
            if (responseCode == HttpURLConnection.HTTP_BAD_REQUEST) {
                throw feilKallTilAbakus(feilmelding);
            }
            throw feilVedKallTilAbakus(feilmelding);
        }
    }

    public void lagreOverstyrtOppgittOpptjening(OppgittOpptjeningMottattRequest dto) throws IOException {
        var json = iayJsonWriter.writeValueAsString(dto);

        var method = new RestRequest.Method(RestRequest.WebMethod.POST, HttpRequest.BodyPublishers.ofString(json));
        var request = RestRequest.newRequest(method, endpointLagreOverstyrtOppgittOpptjening, restConfig).timeout(Duration.ofSeconds(30));

        LOG.info("Lagre overstyrt oppgitt opptjening (behandlingUUID={}) i Abakus", dto.getKoblingReferanse());
        var rawResponse = restClient.sendReturnUnhandled(request);
        var responseCode = rawResponse.statusCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            var responseBody = rawResponse.body();
            var feilmelding = String.format("Kunne ikke lagre overstyrt oppgitt opptjening for behandling: %s til abakus: %s, HTTP status=%s. HTTP Errormessage=%s",
                dto.getKoblingReferanse(), endpointLagreOverstyrtOppgittOpptjening, responseCode, responseBody);
            if (responseCode == HttpURLConnection.HTTP_BAD_REQUEST) {
                throw feilKallTilAbakus(feilmelding);
            }
            throw feilVedKallTilAbakus(feilmelding);
        }
    }

    public void lagreOppgittOpptjeningNullstillOverstyring(OppgittOpptjeningMottattRequest dto) throws IOException {
        var json = iayJsonWriter.writeValueAsString(dto);

        var method = new RestRequest.Method(RestRequest.WebMethod.POST, HttpRequest.BodyPublishers.ofString(json));
        var request = RestRequest.newRequest(method, endpointLagreOppgittOpptjeningNullstillOverstyring, restConfig).timeout(Duration.ofSeconds(30));

        LOG.info("Lagre oppgitt opptjening og nullstill overstyrt (behandlingUUID={}) i Abakus", dto.getKoblingReferanse());
        var rawResponse = restClient.sendReturnUnhandled(request);
        var responseCode = rawResponse.statusCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            var responseBody = rawResponse.body();
            var feilmelding = String.format("Kunne ikke lagre oppgitt opptjening og nullstille overstyrt for behandling: %s til abakus: %s, HTTP status=%s. HTTP Errormessage=%s",
                dto.getKoblingReferanse(), endpointLagreOppgittOpptjeningNullstillOverstyring, responseCode, responseBody);
            if (responseCode == HttpURLConnection.HTTP_BAD_REQUEST) {
                throw feilKallTilAbakus(feilmelding);
            }
            throw feilVedKallTilAbakus(feilmelding);
        }
    }


    public void kopierGrunnlag(KopierGrunnlagRequest dto) throws IOException {
        var json = iayJsonWriter.writeValueAsString(dto);

        var method = new RestRequest.Method(RestRequest.WebMethod.POST, HttpRequest.BodyPublishers.ofString(json));
        var request = RestRequest.newRequest(method, endpointKopierGrunnlag, restConfig);

        LOG.info("Kopierer grunnlag fra (behandlingUUID={}) til (behandlingUUID={}) i Abakus", dto.getGammelReferanse(), dto.getNyReferanse());
        var rawResponse = restClient.sendReturnUnhandled(request);
        var responseCode = rawResponse.statusCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            var responseBody = rawResponse.body();
            var feilmelding = String.format("Feilet med å kopiere grunnlag fra (behandlingUUID=%s) til (behandlingUUID=%s) i Abakus: %s, HTTP status=%s. HTTP Errormessage=%s",
                dto.getGammelReferanse(), dto.getNyReferanse(), endpointKopierGrunnlag, responseCode, responseBody);
            if (responseCode == HttpURLConnection.HTTP_BAD_REQUEST) {
                throw feilKallTilAbakus(feilmelding);
            }
            throw feilVedKallTilAbakus(feilmelding);
        }

    }

    public void kopierGrunnlagUtenNyeInntektsmeldinger(KopierGrunnlagRequest dto) throws IOException {
        var json = iayJsonWriter.writeValueAsString(dto);
        var method = new RestRequest.Method(RestRequest.WebMethod.POST, HttpRequest.BodyPublishers.ofString(json));

        var request = RestRequest.newRequest(method, endpointKopierGrunnlagBeholdIM, restConfig);

        LOG.info("Kopierer grunnlag fra (behandlingUUID={}) til (behandlingUUID={}) i Abakus. Uten nye inntektsmeldinger", dto.getGammelReferanse(), dto.getNyReferanse());
        var rawResponse = restClient.sendReturnUnhandled(request);
        var responseCode = rawResponse.statusCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            var responseBody = rawResponse.body();
            var feilmelding = String.format("Feilet med å kopiere grunnlag fra (behandlingUUID=%s) til (behandlingUUID=%s) i Abakus: %s, HTTP status=%s. HTTP Errormessage=%s",
                dto.getGammelReferanse(), dto.getNyReferanse(), endpointKopierGrunnlagBeholdIM, responseCode, responseBody);
            if (responseCode == HttpURLConnection.HTTP_BAD_REQUEST) {
                throw feilKallTilAbakus(feilmelding);
            }
            throw feilVedKallTilAbakus(feilmelding);
        }
    }

    public void avsluttKobling(AvsluttKoblingRequest dto) throws IOException {
        var json = iayJsonWriter.writeValueAsString(dto);

        var method = new RestRequest.Method(RestRequest.WebMethod.POST, HttpRequest.BodyPublishers.ofString(json));
        var request = RestRequest.newRequest(method, endpointAvsluttKobling, restConfig).timeout(Duration.ofSeconds(30));

        LOG.info("Avslutter kobling (behandlingUUID={}) i Abakus", dto.getReferanse());
        var rawResponse = restClient.sendReturnUnhandled(request);
        var responseCode = rawResponse.statusCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            if (responseCode == HttpURLConnection.HTTP_NO_CONTENT) {
                LOG.info("Fant ikke kobling for referanse: {} i Abakus", dto.getReferanse());
                return;
            }
            var responseBody = rawResponse.body();
            var feilmelding = String.format("Kunne ikke avslutte kobling for behandling: %s til abakus: %s, HTTP status=%s. HTTP Errormessage=%s",
                dto.getReferanse(), endpointAvsluttKobling, responseCode, responseBody);
            if (responseCode == HttpURLConnection.HTTP_BAD_REQUEST) {
                throw feilKallTilAbakus(feilmelding);
            }
            throw feilVedKallTilAbakus(feilmelding);
        }
    }


    private static TekniskException feilVedKallTilAbakus(String feilmelding) {
        return new TekniskException("FP-018669", "Feil ved kall til Abakus: " + feilmelding);
    }

    private static TekniskException feilKallTilAbakus(String feilmelding) {
        return new TekniskException("FP-918669", "[400]: Feil ved kall til Abakus: " + feilmelding);
    }

    private static TekniskException feilVedJsonParsing(String feilmelding) {
        return new TekniskException("FP-851387", "Feil ved kall til Abakus: " + feilmelding);
    }
}
