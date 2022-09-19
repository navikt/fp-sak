package no.nav.foreldrepenger.mottak.vedtak.rest;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.UriBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.vedtak.felles.integrasjon.infotrygd.grunnlag.v1.respons.Grunnlag;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

@ApplicationScoped
@RestClientConfig(tokenConfig = TokenFlow.CONTEXT, endpointProperty = "fpsak.it.fp.grunnlag.url", endpointDefault = "http://infotrygd-foreldrepenger.default/grunnlag")
public class InfotrygdFPGrunnlag  {

    private static final Logger LOG = LoggerFactory.getLogger(InfotrygdFPGrunnlag.class);

    private RestClient restClient;
    private URI uri;
    private String uriString;

    @Inject
    public InfotrygdFPGrunnlag(RestClient restClient) {
        this.restClient = restClient;
        this.uri = RestConfig.endpointFromAnnotation(InfotrygdFPGrunnlag.class);
        this.uriString = uri.toString();
    }

    public InfotrygdFPGrunnlag() {
        // CDI
    }


    public List<Grunnlag> hentGrunnlag(String fnr, LocalDate fom, LocalDate tom) {
        try {
            var request = UriBuilder.fromUri(uri)
                .queryParam("fnr", fnr)
                .queryParam("fom", konverter(fom))
                .queryParam("tom", konverter(tom)).build();
            var grunnlag = restClient.send(RestRequest.newGET(request, InfotrygdFPGrunnlag.class), Grunnlag[].class);
            return Arrays.asList(grunnlag);
        } catch (Exception e) {
            LOG.info("FPSAK Infotrygd Grunnlag FP - Feil ved oppslag mot {}, returnerer ingen grunnlag", uriString, e);
            //throw InfotrygdRestFeil.FACTORY.feilfratjeneste(uriString).toException();
            return Collections.emptyList();
        }
    }

    private static String konverter(LocalDate dato) {
        return dato.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }
}
