package no.nav.foreldrepenger.domene.abakus;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
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

import no.nav.abakus.iaygrunnlag.IayGrunnlagJsonMapper;
import no.nav.abakus.iaygrunnlag.UuidDto;
import no.nav.abakus.iaygrunnlag.arbeidsforhold.v1.ArbeidsforholdDto;
import no.nav.abakus.iaygrunnlag.inntektsmelding.v1.InntektsmeldingerDto;
import no.nav.abakus.iaygrunnlag.inntektsmelding.v1.RefusjonskravDatoerDto;
import no.nav.abakus.iaygrunnlag.request.AktørDatoRequest;
import no.nav.abakus.iaygrunnlag.request.InnhentRegisterdataRequest;
import no.nav.abakus.iaygrunnlag.request.InntektArbeidYtelseGrunnlagRequest;
import no.nav.abakus.iaygrunnlag.request.InntektsmeldingerMottattRequest;
import no.nav.abakus.iaygrunnlag.request.InntektsmeldingerRequest;
import no.nav.abakus.iaygrunnlag.request.KopierGrunnlagRequest;
import no.nav.abakus.iaygrunnlag.request.OppgittOpptjeningMottattRequest;
import no.nav.abakus.iaygrunnlag.v1.InntektArbeidYtelseGrunnlagDto;
import no.nav.abakus.iaygrunnlag.v1.InntektArbeidYtelseGrunnlagSakSnapshotDto;
import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.LogLevel;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;
import no.nav.vedtak.felles.integrasjon.rest.OidcRestClient;
import no.nav.vedtak.felles.integrasjon.rest.OidcRestClientResponseHandler.ObjectReaderResponseHandler;
import no.nav.vedtak.konfig.KonfigVerdi;

@ApplicationScoped
public class AbakusTjeneste {

    private static final Logger log = LoggerFactory.getLogger(AbakusTjeneste.class);
    private final ObjectMapper iayMapper = IayGrunnlagJsonMapper.getMapper();
    private final ObjectWriter iayJsonWriter = iayMapper.writerWithDefaultPrettyPrinter();
    private final ObjectReader iayGrunnlagReader = iayMapper.readerFor(InntektArbeidYtelseGrunnlagDto.class);
    private final ObjectReader arbeidsforholdReader = iayMapper.readerFor(ArbeidsforholdDto[].class);
    private final ObjectReader uuidReader = iayMapper.readerFor(UuidDto.class);
    private final ObjectReader iayGrunnlagSnapshotReader = iayMapper.readerFor(InntektArbeidYtelseGrunnlagSakSnapshotDto.class);
    private final ObjectReader inntektsmeldingerReader = iayMapper.readerFor(InntektsmeldingerDto.class);
    private final ObjectReader refusjonskravDatoerReader = iayMapper.readerFor(RefusjonskravDatoerDto.class);
    private URI innhentRegisterdata;
    private OidcRestClient oidcRestClient;
    private URI abakusEndpoint;
    private URI endpointArbeidsforholdIPeriode;
    private URI endpointGrunnlag;
    private URI endpointMottaInntektsmeldinger;
    private URI endpointMottaOppgittOpptjening;
    private URI endpointKopierGrunnlag;
    private URI endpointGrunnlagSnapshot;
    private URI endpointInntektsmeldinger;
    private URI endpointRefusjonskravdatoer;


    AbakusTjeneste() {
        // for CDI
    }

    @Inject
    public AbakusTjeneste(OidcRestClient oidcRestClient,
                          @KonfigVerdi(value = "fpabakus.url") URI endpoint) {
        this.oidcRestClient = oidcRestClient;
        this.abakusEndpoint = endpoint;

        this.endpointArbeidsforholdIPeriode = toUri("/api/arbeidsforhold/v1/arbeidstaker");
        this.endpointGrunnlag = toUri("/api/iay/grunnlag/v1/");
        this.endpointMottaInntektsmeldinger = toUri("/api/iay/inntektsmeldinger/v1/motta");
        this.endpointMottaOppgittOpptjening = toUri("/api/iay/oppgitt/v1/motta");
        this.endpointGrunnlagSnapshot = toUri("/api/iay/grunnlag/v1/snapshot");
        this.endpointKopierGrunnlag = toUri("/api/iay/grunnlag/v1/kopier");
        this.innhentRegisterdata = toUri("/api/registerdata/v1/innhent/sync");
        this.endpointInntektsmeldinger = toUri("/api/iay/inntektsmeldinger/v1/hentAlle");
        this.endpointRefusjonskravdatoer = toUri("/api/iay/inntektsmeldinger/v1/hentRefusjonskravDatoer");


    }

    private URI toUri(String relativeUri) {
        String uri = abakusEndpoint.toString() + relativeUri;
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
            throw AbakusTjenesteFeil.FEIL.feilVedJsonParsing(e.getMessage()).toException();
        } catch (IOException e) {
            throw AbakusTjenesteFeil.FEIL.feilVedKallTilAbakus(e.getMessage()).toException();
        }
    }

    public InntektArbeidYtelseGrunnlagDto hentGrunnlag(InntektArbeidYtelseGrunnlagRequest request) throws IOException {
        var endpoint = endpointGrunnlag;
        var reader = iayGrunnlagReader;
        var responseHandler = new ObjectReaderResponseHandler<InntektArbeidYtelseGrunnlagDto>(endpoint, reader);
        var json = iayJsonWriter.writeValueAsString(request);

        return hentFraAbakus(endpoint, responseHandler, json);
    }

    public List<ArbeidsforholdDto> hentArbeidsforholdIPerioden(AktørDatoRequest request) {
        var endpoint = endpointArbeidsforholdIPeriode;
        var reader = arbeidsforholdReader;
        var responseHandler = new ObjectReaderResponseHandler<ArbeidsforholdDto[]>(endpoint, reader);
        try {
            var json = iayJsonWriter.writeValueAsString(request);
            ArbeidsforholdDto[] arbeidsforhold = hentFraAbakus(endpoint, responseHandler, json);
            if (arbeidsforhold == null) {
                return Collections.emptyList();
            }
            return Arrays.asList(arbeidsforhold);
        } catch (JsonProcessingException e) {
            throw AbakusTjenesteFeil.FEIL.feilVedJsonParsing(e.getMessage()).toException();
        } catch (IOException e) {
            throw AbakusTjenesteFeil.FEIL.feilVedKallTilAbakus(e.getMessage()).toException();
        }
    }

    public InntektsmeldingerDto hentUnikeUnntektsmeldinger(InntektsmeldingerRequest request) throws IOException {
        var endpoint = endpointInntektsmeldinger;
        var reader = inntektsmeldingerReader;
        var responseHandler = new ObjectReaderResponseHandler<InntektsmeldingerDto>(endpoint, reader);
        var json = iayJsonWriter.writeValueAsString(request);

        return hentFraAbakus(endpoint, responseHandler, json);
    }

    public RefusjonskravDatoerDto hentRefusjonskravDatoer(InntektsmeldingerRequest request) throws IOException {
        var endpoint = endpointRefusjonskravdatoer;
        var reader = refusjonskravDatoerReader;
        var responseHandler = new ObjectReaderResponseHandler<RefusjonskravDatoerDto>(endpoint, reader);
        var json = iayJsonWriter.writeValueAsString(request);

        return hentFraAbakus(endpoint, responseHandler, json);
    }

    public InntektArbeidYtelseGrunnlagSakSnapshotDto hentGrunnlagSnapshot(InntektArbeidYtelseGrunnlagRequest request) throws IOException {
        var endpoint = endpointGrunnlagSnapshot;
        var reader = iayGrunnlagSnapshotReader;
        var responseHandler = new ObjectReaderResponseHandler<InntektArbeidYtelseGrunnlagSakSnapshotDto>(endpoint, reader);
        var json = iayJsonWriter.writeValueAsString(request);

        return hentFraAbakus(endpoint, responseHandler, json);
    }

    private <T> T hentFraAbakus(URI endpoint, ObjectReaderResponseHandler<T> responseHandler, String json) throws IOException {
        var httpPost = new HttpPost(endpoint); // NOSONAR håndterer i responseHandler
        httpPost.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));

        try (var httpResponse = oidcRestClient.execute(httpPost)) {
            int responseCode = httpResponse.getStatusLine().getStatusCode();
            if (responseCode == HttpStatus.SC_OK) {
                return responseHandler.handleResponse(httpResponse);
            } else {
                if (responseCode == HttpStatus.SC_NOT_MODIFIED) {
                    return null;
                }
                if (responseCode == HttpStatus.SC_NO_CONTENT) {
                    return null;
                }
                String responseBody = EntityUtils.toString(httpResponse.getEntity());
                String feilmelding = "Kunne ikke hente grunnlag fra abakus: " + httpPost.getURI()
                    + ", HTTP status=" + httpResponse.getStatusLine() + ". HTTP Errormessage=" + responseBody;
                if (responseCode == HttpStatus.SC_BAD_REQUEST) {
                    throw AbakusTjenesteFeil.FEIL.feilKallTilAbakus(feilmelding).toException();
                } else {
                    throw AbakusTjenesteFeil.FEIL.feilVedKallTilAbakus(feilmelding).toException();
                }
            }
        } catch (RuntimeException re) {
            log.warn("Feil ved henting av data fra abakus: endpoint=" + endpoint, re);
            throw re;
        }
    }

    public void lagreGrunnlag(InntektArbeidYtelseGrunnlagDto dto) throws IOException {

        var json = iayJsonWriter.writeValueAsString(dto);

        HttpPut httpPut = new HttpPut(endpointGrunnlag);
        httpPut.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));

        log.info("Lagre IAY grunnlag (behandlingUUID={}) i Abakus", dto.getKoblingReferanse());
        try (var httpResponse = oidcRestClient.execute(httpPut)) {
            int responseCode = httpResponse.getStatusLine().getStatusCode();
            if (responseCode != HttpStatus.SC_OK) {
                String responseBody = EntityUtils.toString(httpResponse.getEntity());
                String feilmelding = "Kunne ikke lagre IAY grunnlag: " + dto.getGrunnlagReferanse() + " til abakus: " + httpPut.getURI()
                    + ", HTTP status=" + httpResponse.getStatusLine() + ". HTTP Errormessage=" + responseBody;

                if (responseCode == HttpStatus.SC_BAD_REQUEST) {
                    throw AbakusTjenesteFeil.FEIL.feilKallTilAbakus(feilmelding).toException();
                } else {
                    throw AbakusTjenesteFeil.FEIL.feilVedKallTilAbakus(feilmelding).toException();
                }
            }
        }
    }

    public void lagreInntektsmeldinger(InntektsmeldingerMottattRequest dto) throws IOException {

        var json = iayJsonWriter.writeValueAsString(dto);

        HttpPost httpPost = new HttpPost(endpointMottaInntektsmeldinger);
        httpPost.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));

        log.info("Lagre mottatte inntektsmeldinger (behandlingUUID={}) i Abakus", dto.getKoblingReferanse());
        try (var httpResponse = oidcRestClient.execute(httpPost)) {
            int responseCode = httpResponse.getStatusLine().getStatusCode();
            if (responseCode != HttpStatus.SC_OK) {
                String responseBody = EntityUtils.toString(httpResponse.getEntity());
                String feilmelding = "Kunne ikke lagre mottatte inntektsmeldinger for behandling: " + dto.getKoblingReferanse() + " til abakus: " + httpPost.getURI()
                    + ", HTTP status=" + httpResponse.getStatusLine() + ". HTTP Errormessage=" + responseBody;

                if (responseCode == HttpStatus.SC_BAD_REQUEST) {
                    throw AbakusTjenesteFeil.FEIL.feilKallTilAbakus(feilmelding).toException();
                } else {
                    throw AbakusTjenesteFeil.FEIL.feilVedKallTilAbakus(feilmelding).toException();
                }
            }
        }
    }

    public void lagreOppgittOpptjening(OppgittOpptjeningMottattRequest request) throws IOException {
        var json = iayJsonWriter.writeValueAsString(request);

        HttpPost httpPost = new HttpPost(endpointMottaOppgittOpptjening);
        httpPost.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));

        log.info("Lagre oppgitt opptjening (behandlingUUID={}) i Abakus", request.getKoblingReferanse());
        try (var httpResponse = oidcRestClient.execute(httpPost)) {
            int responseCode = httpResponse.getStatusLine().getStatusCode();
            if (responseCode != HttpStatus.SC_OK) {
                String responseBody = EntityUtils.toString(httpResponse.getEntity());
                String feilmelding = "Kunne ikke lagre oppgitt opptjening for behandling: " + request.getKoblingReferanse() + " til abakus: " + httpPost.getURI()
                    + ", HTTP status=" + httpResponse.getStatusLine() + ". HTTP Errormessage=" + responseBody;

                if (responseCode == HttpStatus.SC_BAD_REQUEST) {
                    throw AbakusTjenesteFeil.FEIL.feilKallTilAbakus(feilmelding).toException();
                } else {
                    throw AbakusTjenesteFeil.FEIL.feilVedKallTilAbakus(feilmelding).toException();
                }
            }
        }
    }

    public void kopierGrunnlag(KopierGrunnlagRequest request) throws IOException {
        var json = iayJsonWriter.writeValueAsString(request);

        HttpPost httpPost = new HttpPost(endpointKopierGrunnlag);
        httpPost.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));

        log.info("Kopierer grunnlag fra (behandlingUUID={}) til (behandlingUUID={}) i Abakus", request.getGammelReferanse(), request.getNyReferanse());
        try (var httpResponse = oidcRestClient.execute(httpPost)) {
            int responseCode = httpResponse.getStatusLine().getStatusCode();
            if (responseCode != HttpStatus.SC_OK) {
                String responseBody = EntityUtils.toString(httpResponse.getEntity());
                String feilmelding = "Feilet med å kopiere grunnlag fra (behandlingUUID=" + request.getGammelReferanse() + ") til (behandlingUUID=" + request.getNyReferanse() + ") i Abakus: " + httpPost.getURI()
                    + ", HTTP status=" + httpResponse.getStatusLine() + ". HTTP Errormessage=" + responseBody;

                if (responseCode == HttpStatus.SC_BAD_REQUEST) {
                    throw AbakusTjenesteFeil.FEIL.feilKallTilAbakus(feilmelding).toException();
                } else {
                    throw AbakusTjenesteFeil.FEIL.feilVedKallTilAbakus(feilmelding).toException();
                }
            }
        }
    }

    public interface AbakusTjenesteFeil extends DeklarerteFeil {
        AbakusTjenesteFeil FEIL = FeilFactory.create(AbakusTjenesteFeil.class);

        @TekniskFeil(feilkode = "FP-018669", feilmelding = "Feil ved kall til Abakus: %s", logLevel = LogLevel.ERROR)
        Feil feilVedKallTilAbakus(String feilmelding);

        @TekniskFeil(feilkode = "FP-918669", feilmelding = "Feil ved kall til Abakus: %s", logLevel = LogLevel.WARN)
        Feil feilKallTilAbakus(String feilmelding);

        @TekniskFeil(feilkode = "FP-851387", feilmelding = "Feil ved kall til Abakus: %s", logLevel = LogLevel.WARN)
        Feil feilVedJsonParsing(String feilmelding);

    }

}
