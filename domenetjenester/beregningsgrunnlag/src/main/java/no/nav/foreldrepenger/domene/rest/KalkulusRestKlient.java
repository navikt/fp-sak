package no.nav.foreldrepenger.domene.rest;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.jboss.weld.exceptions.IllegalStateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

import no.nav.folketrygdloven.kalkulus.mappers.JsonMapper;
import no.nav.folketrygdloven.kalkulus.request.v1.BeregnListeRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.BeregningsgrunnlagListeRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.HentBeregningsgrunnlagDtoListeForGUIRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.HentBeregningsgrunnlagListeRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.HåndterBeregningListeRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.KopierBeregningListeRequest;
import no.nav.folketrygdloven.kalkulus.response.v1.KopiResponse;
import no.nav.folketrygdloven.kalkulus.response.v1.TilstandListeResponse;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.detaljert.BeregningsgrunnlagGrunnlagDto;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.gui.BeregningsgrunnlagListe;
import no.nav.folketrygdloven.kalkulus.response.v1.håndtering.OppdateringListeRespons;
import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.vedtak.exception.VLException;
import no.nav.vedtak.felles.integrasjon.rest.OidcRestClient;
import no.nav.vedtak.felles.integrasjon.rest.OidcRestClientResponseHandler;

@ApplicationScoped
public class KalkulusRestKlient {

    private static final Logger log = LoggerFactory.getLogger(KalkulusRestKlient.class);
    private final ObjectMapper kalkulusMapper = JsonMapper.getMapper();
    private final ObjectWriter kalkulusJsonWriter = kalkulusMapper.writerWithDefaultPrettyPrinter();
    private final ObjectReader tilstandReader = kalkulusMapper.readerFor(TilstandListeResponse.class);
    private final ObjectReader oppdaterListeReader = kalkulusMapper.readerFor(OppdateringListeRespons.class);
    private final ObjectReader dtoListeReader = kalkulusMapper.readerFor(BeregningsgrunnlagListe.class);
    private final ObjectReader grunnlagListReader = kalkulusMapper.readerFor(new TypeReference<List<BeregningsgrunnlagGrunnlagDto>>() {
    });
    private final ObjectReader kopierReader = kalkulusMapper.readerFor(new TypeReference<List<KopiResponse>>() {});


    private CloseableHttpClient restClient;
    private URI kalkulusEndpoint;
    private URI beregnEndpoint;
    private URI oppdaterListeEndpoint;
    private URI beregningsgrunnlagListeDtoEndpoint;
    private URI beregningsgrunnlagGrunnlagBolkEndpoint;
    private URI deaktiverBeregningsgrunnlag;
    private URI kopierEndpoint;



    protected KalkulusRestKlient() {
        // cdi
    }

    @Inject
    public KalkulusRestKlient(OidcRestClient restClient, @KonfigVerdi(value = "ftkalkulus.url") URI endpoint) {
        this(endpoint);
        this.restClient = restClient;
    }

    private KalkulusRestKlient(URI endpoint) {
        this.kalkulusEndpoint = endpoint;
        this.beregnEndpoint = toUri("/api/kalkulus/v1/beregn/bolk");
        this.deaktiverBeregningsgrunnlag = toUri("/api/kalkulus/v1/deaktiver/bolk");
        this.kopierEndpoint = toUri("/api/kalkulus/v1/kopier/bolk");
        this.oppdaterListeEndpoint = toUri("/api/kalkulus/v1/oppdaterListe");
        this.beregningsgrunnlagListeDtoEndpoint = toUri("/api/kalkulus/v1/beregningsgrunnlagListe");
        this.beregningsgrunnlagGrunnlagBolkEndpoint = toUri("/api/kalkulus/v1/grunnlag/bolk");
    }

    public TilstandListeResponse beregn(BeregnListeRequest request) {
        var endpoint = beregnEndpoint;

        try {
            return getResponse(endpoint, kalkulusJsonWriter.writeValueAsString(request), tilstandReader);
        } catch (JsonProcessingException e) {
            throw feilVedParsingAvJson(endpoint, e.getMessage());
        }
    }


    public OppdateringListeRespons oppdaterBeregningListe(HåndterBeregningListeRequest request) {
        try {
            return getResponse(oppdaterListeEndpoint, kalkulusJsonWriter.writeValueAsString(request), oppdaterListeReader);
        } catch (JsonProcessingException e) {
            throw feilVedParsingAvJson(oppdaterListeEndpoint, e.getMessage());
        }
    }

    public void deaktiverBeregningsgrunnlag(BeregningsgrunnlagListeRequest request) {
        var endpoint = deaktiverBeregningsgrunnlag;
        try {
            deaktiver(endpoint, kalkulusJsonWriter.writeValueAsString(request));
        } catch (JsonProcessingException e) {
            throw feilVedParsingAvJson(endpoint, e.getMessage());
        }
    }

    public List<KopiResponse> kopierBeregning(KopierBeregningListeRequest request) {
        var endpoint = kopierEndpoint;

        try {
            return getResponse(endpoint, kalkulusJsonWriter.writeValueAsString(request), kopierReader);
        } catch (JsonProcessingException e) {
            throw feilVedParsingAvJson(endpoint, e.getMessage());
        }
    }


    public List<BeregningsgrunnlagGrunnlagDto> hentBeregningsgrunnlagGrunnlag(HentBeregningsgrunnlagListeRequest req) {
        var endpoint = beregningsgrunnlagGrunnlagBolkEndpoint;

        try {
            return getResponse(endpoint, kalkulusJsonWriter.writeValueAsString(req), grunnlagListReader);
        } catch (JsonProcessingException e) {
            throw feilVedParsingAvJson(endpoint, e.getMessage());
        }
    }

    public BeregningsgrunnlagListe hentBeregningsgrunnlagDto(HentBeregningsgrunnlagDtoListeForGUIRequest request) {
        var endpoint = beregningsgrunnlagListeDtoEndpoint;

        try {
            return getResponse(endpoint, kalkulusJsonWriter.writeValueAsString(request), dtoListeReader);
        } catch (JsonProcessingException e) {
            throw feilVedParsingAvJson(endpoint, e.getMessage());
        }
    }


    private <T> T getResponse(URI endpoint, String json, ObjectReader reader) {
        try {
            return utførOgHent(endpoint, json, new OidcRestClientResponseHandler.ObjectReaderResponseHandler<>(endpoint, reader));
        } catch (IOException e) {
            throw feilVedKallTil(endpoint, e.getMessage());
        }
    }

    private void deaktiver(URI endpoint, String json) {
        try {
            utfør(endpoint, json);
        } catch (IOException e) {
            throw feilVedKallTil(endpoint, e.getMessage());
        }
    }

    private void utfør(URI endpoint, String json) throws IOException {
        var httpPost = new HttpPost(endpoint); // NOSONAR håndterer i responseHandler
        httpPost.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
        try (var httpResponse = restClient.execute(httpPost)) {
            int responseCode = httpResponse.getStatusLine().getStatusCode();
            if (!isOk(responseCode)) {
                if (responseCode == HttpStatus.SC_NOT_MODIFIED) {
                    log.warn("Kall til deaktiver gjorde ingen endring på beregningsgrunnlag");
                } else if (responseCode != HttpStatus.SC_NO_CONTENT && responseCode != HttpStatus.SC_ACCEPTED) {
                    String responseBody = EntityUtils.toString(httpResponse.getEntity());
                    String feilmelding =
                        "Kunne ikke utføre kall til kalkulus," + " endpoint=" + httpPost.getURI() + ", HTTP status=" + httpResponse.getStatusLine()
                            + ". HTTP Errormessage=" + responseBody;
                    throw feilVedKallTil(endpoint, feilmelding);
                }
            }
        } catch (VLException e) {
            throw e; // rethrow
        } catch (RuntimeException re) {
            log.warn("Feil ved henting av data. uri=" + endpoint, re);
            throw re;
        }
    }

    private <T> T utførOgHent(URI endpoint, String json, OidcRestClientResponseHandler<T> responseHandler) throws IOException {
        var httpPost = new HttpPost(endpoint); // NOSONAR håndterer i responseHandler
        httpPost.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));

        try (var httpResponse = restClient.execute(httpPost)) {
            int responseCode = httpResponse.getStatusLine().getStatusCode();
            if (isOk(responseCode)) {
                return responseHandler.handleResponse(httpResponse);
            } else {
                if (responseCode == HttpStatus.SC_NOT_MODIFIED) {
                    return null;
                }
                if (responseCode == HttpStatus.SC_NO_CONTENT) {
                    return null;
                }
                if (responseCode == HttpStatus.SC_ACCEPTED) {
                    return null;
                }
                String responseBody = EntityUtils.toString(httpResponse.getEntity());
                String feilmelding =
                    "Kunne ikke hente utføre kall til kalkulus," + " endpoint=" + httpPost.getURI() + ", HTTP status=" + httpResponse.getStatusLine()
                        + ". HTTP Errormessage=" + responseBody;
                if (responseCode == HttpStatus.SC_BAD_REQUEST) {
                    throw feilVedKallTil(endpoint, feilmelding);
                } else {
                    throw feilVedKallTil(endpoint, feilmelding);
                }
            }
        } catch (VLException e) {
            throw e; // retrhow
        } catch (RuntimeException re) {
            log.warn("Feil ved henting av data. uri=" + endpoint, re);
            throw re;
        }
    }

    private IllegalStateException feilVedKallTil(URI endpoint, String feilmelding) {
        return new IllegalStateException(String.format("Feil ved kall til %s: %s", endpoint, feilmelding));
    }

    private IllegalStateException feilVedParsingAvJson(URI endpoint, String feilmelding) {
        return new IllegalStateException(String.format("Feil ved parsing av json ved kall til %s: %s", endpoint, feilmelding));
    }

    private boolean isOk(int responseCode) {
        return responseCode == HttpStatus.SC_OK || responseCode == HttpStatus.SC_CREATED;
    }


    private URI toUri(String relativeUri) {
        String uri = kalkulusEndpoint.toString() + relativeUri;
        try {
            return new URI(uri);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Ugyldig uri: " + uri, e);
        }
    }


}
