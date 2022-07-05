package no.nav.foreldrepenger.domene.abakus;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

import no.nav.abakus.iaygrunnlag.AktørIdPersonident;
import no.nav.abakus.iaygrunnlag.IayGrunnlagJsonMapper;
import no.nav.abakus.iaygrunnlag.UuidDto;
import no.nav.abakus.iaygrunnlag.arbeidsforhold.v1.ArbeidsforholdDto;
import no.nav.abakus.iaygrunnlag.inntektsmelding.v1.InntektsmeldingerDto;
import no.nav.abakus.iaygrunnlag.inntektsmelding.v1.RefusjonskravDatoerDto;
import no.nav.abakus.iaygrunnlag.kodeverk.YtelseType;
import no.nav.abakus.iaygrunnlag.request.AktørDatoRequest;
import no.nav.abakus.iaygrunnlag.request.InnhentRegisterdataRequest;
import no.nav.abakus.iaygrunnlag.request.InntektArbeidYtelseGrunnlagRequest;
import no.nav.abakus.iaygrunnlag.request.InntektsmeldingDiffRequest;
import no.nav.abakus.iaygrunnlag.request.InntektsmeldingerMottattRequest;
import no.nav.abakus.iaygrunnlag.request.InntektsmeldingerRequest;
import no.nav.abakus.iaygrunnlag.request.KopierGrunnlagRequest;
import no.nav.abakus.iaygrunnlag.request.OppgittOpptjeningMottattRequest;
import no.nav.abakus.iaygrunnlag.v1.InntektArbeidYtelseGrunnlagDto;
import no.nav.abakus.iaygrunnlag.v1.InntektArbeidYtelseGrunnlagSakSnapshotDto;
import no.nav.abakus.iaygrunnlag.v1.OverstyrtInntektArbeidYtelseDto;
import no.nav.abakus.vedtak.ytelse.Ytelse;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.integrasjon.rest.OidcRestClient;
import no.nav.vedtak.felles.integrasjon.rest.OidcRestClientResponseHandler.ObjectReaderResponseHandler;

@ApplicationScoped
public class AbakusTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(AbakusTjeneste.class);
    private final ObjectMapper iayMapper = IayGrunnlagJsonMapper.getMapper();
    private final ObjectWriter iayJsonWriter = iayMapper.writerWithDefaultPrettyPrinter();
    private final ObjectReader iayGrunnlagReader = iayMapper.readerFor(InntektArbeidYtelseGrunnlagDto.class);
    private final ObjectReader arbeidsforholdReader = iayMapper.readerFor(ArbeidsforholdDto[].class);
    private final ObjectReader uuidReader = iayMapper.readerFor(UuidDto.class);
    private final ObjectReader iayGrunnlagSnapshotReader = iayMapper.readerFor(InntektArbeidYtelseGrunnlagSakSnapshotDto.class);
    private final ObjectReader inntektsmeldingerReader = iayMapper.readerFor(InntektsmeldingerDto.class);
    private URI innhentRegisterdata;
    private OidcRestClient oidcRestClient;
    private URI abakusEndpoint;
    private URI callbackUrl;
    private URI endpointArbeidsforholdIPeriode;
    private URI endpointGrunnlag;
    private URI endpointMottaInntektsmeldinger;
    private URI endpointMottaOppgittOpptjening;
    private URI endpointKopierGrunnlag;
    private URI endpointGrunnlagSnapshot;
    private URI endpointInntektsmeldinger;
    private URI endpointYtelser;
    private URI endpointOverstyring;
    private URI endpointLagreYtelse;

    AbakusTjeneste() {
        // for CDI
    }

    @Inject
    public AbakusTjeneste(OidcRestClient oidcRestClient,
                          @KonfigVerdi(value = "fpabakus.url") URI endpoint,
                          @KonfigVerdi(value = "abakus.callback.url") URI callbackUrl) {
        this.oidcRestClient = oidcRestClient;
        this.abakusEndpoint = endpoint;
        this.callbackUrl = callbackUrl;
        this.endpointArbeidsforholdIPeriode = toUri("/api/arbeidsforhold/v1/arbeidstaker");
        this.endpointGrunnlag = toUri("/api/iay/grunnlag/v1/");
        this.endpointMottaInntektsmeldinger = toUri("/api/iay/inntektsmeldinger/v1/motta");
        this.endpointMottaOppgittOpptjening = toUri("/api/iay/oppgitt/v1/motta");
        this.endpointGrunnlagSnapshot = toUri("/api/iay/grunnlag/v1/snapshot");
        this.endpointKopierGrunnlag = toUri("/api/iay/grunnlag/v1/kopier");
        this.innhentRegisterdata = toUri("/api/registerdata/v1/innhent/async");
        this.endpointInntektsmeldinger = toUri("/api/iay/inntektsmeldinger/v1/hentAlle");
        this.endpointYtelser = toUri("/api/ytelse/v1/hentVedtakForAktoer");
        this.endpointLagreYtelse = toUri("/api/ytelse/v1/vedtatt");
        this.endpointOverstyring = toUri("/api/iay/grunnlag/v1/overstyrt");
    }

    private URI toUri(String relativeUri) {
        var uri = abakusEndpoint.toString() + relativeUri;
        try {
            return new URI(uri);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Ugyldig uri: " + uri, e);
        }
    }

    public UuidDto innhentRegisterdata(InnhentRegisterdataRequest request) {
        var endpoint = innhentRegisterdata;

        var responseHandler = new ObjectReaderResponseHandler<UuidDto>(endpoint, uuidReader);
        try {
            var json = iayJsonWriter.writeValueAsString(request);

            return hentFraAbakus(endpoint, responseHandler, json);
        } catch (JsonProcessingException e) {
            throw feilVedJsonParsing(e.getMessage());
        } catch (IOException e) {
            throw feilVedKallTilAbakus(e.getMessage());
        }
    }

    public String getCallbackUrl() {
        return callbackUrl.toString();
    }

    public InntektArbeidYtelseGrunnlagDto hentGrunnlag(InntektArbeidYtelseGrunnlagRequest request) throws IOException {
        var endpoint = endpointGrunnlag;
        var responseHandler = new ObjectReaderResponseHandler<InntektArbeidYtelseGrunnlagDto>(endpoint,
            iayGrunnlagReader);
        var json = iayJsonWriter.writeValueAsString(request);

        return hentFraAbakus(endpoint, responseHandler, json);
    }

    public List<ArbeidsforholdDto> hentArbeidsforholdIPerioden(AktørDatoRequest request) {
        var endpoint = endpointArbeidsforholdIPeriode;
        var responseHandler = new ObjectReaderResponseHandler<ArbeidsforholdDto[]>(endpoint, arbeidsforholdReader);
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
        var responseHandler = new ObjectReaderResponseHandler<InntektsmeldingerDto>(endpoint, inntektsmeldingerReader);
        var json = iayJsonWriter.writeValueAsString(request);

        return hentFraAbakus(endpoint, responseHandler, json);
    }

    public InntektArbeidYtelseGrunnlagSakSnapshotDto hentGrunnlagSnapshot(InntektArbeidYtelseGrunnlagRequest request) throws IOException {
        var endpoint = endpointGrunnlagSnapshot;
        var responseHandler = new ObjectReaderResponseHandler<InntektArbeidYtelseGrunnlagSakSnapshotDto>(endpoint,
            iayGrunnlagSnapshotReader);
        var json = iayJsonWriter.writeValueAsString(request);

        return hentFraAbakus(endpoint, responseHandler, json);
    }

    public static AktørDatoRequest lagRequestForHentVedtakFom(AktørId aktørId, LocalDate fom) {
        return new AktørDatoRequest(new AktørIdPersonident(aktørId.getId()), fom, YtelseType.FORELDREPENGER);
    }

    public List<Ytelse> hentVedtakForAktørId(AktørDatoRequest request) {
        var endpoint = endpointYtelser;
        var reader = iayMapper.readerFor(Ytelse[].class);
        var responseHandler = new ObjectReaderResponseHandler<Ytelse[]>(endpoint, reader);

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

    private <T> T hentFraAbakus(URI endpoint, ObjectReaderResponseHandler<T> responseHandler, String json) throws IOException {
        var httpPost = new HttpPost(endpoint); // NOSONAR håndterer i responseHandler
        httpPost.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));

        try (var httpResponse = oidcRestClient.execute(httpPost)) {
            var responseCode = httpResponse.getStatusLine().getStatusCode();
            if (responseCode == HttpStatus.SC_OK || responseCode == HttpStatus.SC_CREATED) {
                return responseHandler.handleResponse(httpResponse);
            }
            if (responseCode == HttpStatus.SC_NOT_MODIFIED) {
                return null;
            }
            if (responseCode == HttpStatus.SC_NO_CONTENT) {
                return null;
            }
            if (responseCode == HttpStatus.SC_ACCEPTED) {
                return null;
            }
            var responseBody = EntityUtils.toString(httpResponse.getEntity());
            var feilmelding = "Kunne ikke hente grunnlag fra abakus: " + httpPost.getURI()
                    + ", HTTP status=" + httpResponse.getStatusLine() + ". HTTP Errormessage=" + responseBody;
            if (responseCode == HttpStatus.SC_BAD_REQUEST) {
                throw feilKallTilAbakus(feilmelding);
            }
            throw feilVedKallTilAbakus(feilmelding);
        } catch (RuntimeException re) {
            LOG.warn("Feil ved henting av data fra abakus: endpoint={}", endpoint, re);
            throw re;
        }
    }

    @Deprecated
    public void lagreGrunnlag(InntektArbeidYtelseGrunnlagDto dto) throws IOException {

        var json = iayJsonWriter.writeValueAsString(dto);

        var httpPut = new HttpPut(endpointGrunnlag);
        httpPut.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));

        LOG.info("Lagre IAY grunnlag (behandlingUUID={}) i Abakus", dto.getKoblingReferanse());
        try (var httpResponse = oidcRestClient.execute(httpPut)) {
            var responseCode = httpResponse.getStatusLine().getStatusCode();
            if (responseCode != HttpStatus.SC_OK) {
                var responseBody = EntityUtils.toString(httpResponse.getEntity());
                var feilmelding = "Kunne ikke lagre IAY grunnlag: " + dto.getGrunnlagReferanse() + " til abakus: " + httpPut.getURI()
                        + ", HTTP status=" + httpResponse.getStatusLine() + ". HTTP Errormessage=" + responseBody;

                if (responseCode == HttpStatus.SC_BAD_REQUEST) {
                    throw feilKallTilAbakus(feilmelding);
                }
                throw feilVedKallTilAbakus(feilmelding);
            }
        }
    }

    public void lagreOverstyrtGrunnlag(OverstyrtInntektArbeidYtelseDto overstyrtDto) throws IOException {
        var json = iayJsonWriter.writeValueAsString(overstyrtDto);

        var httpPut = new HttpPut(endpointOverstyring);
        httpPut.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));

        LOG.info("Lagre IAY grunnlag (behandlingUUID={}) i Abakus", overstyrtDto.getKoblingReferanse());
        try (var httpResponse = oidcRestClient.execute(httpPut)) {
            var responseCode = httpResponse.getStatusLine().getStatusCode();
            if (responseCode != HttpStatus.SC_OK) {
                var responseBody = EntityUtils.toString(httpResponse.getEntity());
                var feilmelding = "Kunne ikke lagre overstyrt IAY grunnlag: " + overstyrtDto.getGrunnlagReferanse() + " til abakus: " + httpPut.getURI()
                    + ", HTTP status=" + httpResponse.getStatusLine() + ". HTTP Errormessage=" + responseBody;

                if (responseCode == HttpStatus.SC_BAD_REQUEST) {
                    throw feilKallTilAbakus(feilmelding);
                }
                throw feilVedKallTilAbakus(feilmelding);
            }
        }
    }

    public void lagreInntektsmeldinger(InntektsmeldingerMottattRequest dto) throws IOException {

        var json = iayJsonWriter.writeValueAsString(dto);

        var httpPost = new HttpPost(endpointMottaInntektsmeldinger);
        httpPost.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));

        LOG.info("Lagre mottatte inntektsmeldinger (behandlingUUID={}) i Abakus", dto.getKoblingReferanse());
        try (var httpResponse = oidcRestClient.execute(httpPost)) {
            var responseCode = httpResponse.getStatusLine().getStatusCode();
            if (responseCode != HttpStatus.SC_OK) {
                var responseBody = EntityUtils.toString(httpResponse.getEntity());
                var feilmelding = "Kunne ikke lagre mottatte inntektsmeldinger for behandling: " + dto.getKoblingReferanse() + " til abakus: "
                        + httpPost.getURI()
                        + ", HTTP status=" + httpResponse.getStatusLine() + ". HTTP Errormessage=" + responseBody;

                if (responseCode == HttpStatus.SC_BAD_REQUEST) {
                    throw feilKallTilAbakus(feilmelding);
                }
                throw feilVedKallTilAbakus(feilmelding);
            }
        }
    }

    public void lagreOppgittOpptjening(OppgittOpptjeningMottattRequest request) throws IOException {
        var json = iayJsonWriter.writeValueAsString(request);

        var httpPost = new HttpPost(endpointMottaOppgittOpptjening);
        httpPost.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));

        LOG.info("Lagre oppgitt opptjening (behandlingUUID={}) i Abakus", request.getKoblingReferanse());
        try (var httpResponse = oidcRestClient.execute(httpPost)) {
            var responseCode = httpResponse.getStatusLine().getStatusCode();
            if (responseCode != HttpStatus.SC_OK) {
                var responseBody = EntityUtils.toString(httpResponse.getEntity());
                var feilmelding = "Kunne ikke lagre oppgitt opptjening for behandling: " + request.getKoblingReferanse() + " til abakus: "
                        + httpPost.getURI()
                        + ", HTTP status=" + httpResponse.getStatusLine() + ". HTTP Errormessage=" + responseBody;

                if (responseCode == HttpStatus.SC_BAD_REQUEST) {
                    throw feilKallTilAbakus(feilmelding);
                }
                throw feilVedKallTilAbakus(feilmelding);
            }
        }
    }

    public void kopierGrunnlag(KopierGrunnlagRequest request) throws IOException {
        var json = iayJsonWriter.writeValueAsString(request);

        var httpPost = new HttpPost(endpointKopierGrunnlag);
        httpPost.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));

        LOG.info("Kopierer grunnlag fra (behandlingUUID={}) til (behandlingUUID={}) i Abakus", request.getGammelReferanse(),
                request.getNyReferanse());
        try (var httpResponse = oidcRestClient.execute(httpPost)) {
            var responseCode = httpResponse.getStatusLine().getStatusCode();
            if (responseCode != HttpStatus.SC_OK) {
                var responseBody = EntityUtils.toString(httpResponse.getEntity());
                var feilmelding = "Feilet med å kopiere grunnlag fra (behandlingUUID=" + request.getGammelReferanse() + ") til (behandlingUUID="
                        + request.getNyReferanse() + ") i Abakus: " + httpPost.getURI()
                        + ", HTTP status=" + httpResponse.getStatusLine() + ". HTTP Errormessage=" + responseBody;

                if (responseCode == HttpStatus.SC_BAD_REQUEST) {
                    throw feilKallTilAbakus(feilmelding);
                }
                throw feilVedKallTilAbakus(feilmelding);
            }
        }
    }

    public void lagreYtelse(Ytelse request) throws IOException {
        var json = iayJsonWriter.writeValueAsString(request);

        var httpPost = new HttpPost(endpointLagreYtelse);
        httpPost.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));

        try (var httpResponse = oidcRestClient.execute(httpPost)) {
            var responseCode = httpResponse.getStatusLine().getStatusCode();
            if (responseCode != HttpStatus.SC_OK) {
                var responseBody = EntityUtils.toString(httpResponse.getEntity());
                var feilmelding = "Kunne ikke lagre vedtak for sak: " + request.getSaksnummer() + " til abakus: "
                    + httpPost.getURI()
                    + ", HTTP status=" + httpResponse.getStatusLine() + ". HTTP Errormessage=" + responseBody;

                if (responseCode == HttpStatus.SC_BAD_REQUEST) {
                    throw feilKallTilAbakus(feilmelding);
                }
                throw feilVedKallTilAbakus(feilmelding);
            }
        }
    }

    private static TekniskException feilVedKallTilAbakus(String feilmelding) {
        return new TekniskException("FP-018669", "Feil ved kall til Abakus: " + feilmelding);
    }

    private static TekniskException feilKallTilAbakus(String feilmelding) {
        return new TekniskException("FP-918669", "Feil ved kall til Abakus: " + feilmelding);
    }

    private static TekniskException feilVedJsonParsing(String feilmelding) {
        return new TekniskException("FP-851387", "Feil ved kall til Abakus: " + feilmelding);
    }
}
