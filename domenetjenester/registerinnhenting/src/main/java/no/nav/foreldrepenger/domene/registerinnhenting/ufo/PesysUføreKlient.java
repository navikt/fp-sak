package no.nav.foreldrepenger.domene.registerinnhenting.ufo;

import java.time.LocalDate;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.UriBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

/*
 * Dokumentasjon Tjeneste for å hente informasjon om brukers uførehistorikk
 * Swagger https://pensjon-pen-q2.dev.adeo.no/pen/swagger/#/Hent%20tjenester%20fra%20Sak/hentUforeHistorikk
 */

@ApplicationScoped
@RestClientConfig(tokenConfig = TokenFlow.ADAPTIVE, endpointProperty = "ufore.rs.url", endpointDefault = "http://pensjon-pen.pensjondeployer/api/sak/harUforegrad",
    scopesProperty = "ufore.scopes", scopesDefault = "api://prod-fss.pensjondeployer.pensjon-pen/.default")
public class PesysUføreKlient {

    private static final Logger LOG = LoggerFactory.getLogger(PesysUføreKlient.class);

    private static final String HEADER_FNR = "fnr";

    private final RestClient restClient;
    private final RestConfig restConfig;

    public PesysUføreKlient() {
        this.restClient = RestClient.client();
        this.restConfig = RestConfig.forClient(PesysUføreKlient.class);
    }

    public Optional<Uføreperiode> hentUføreHistorikk(String fnr, LocalDate startDato) {
        var uføretyperParam = UforeTypeCode.UFORE.name() + "," + UforeTypeCode.UF_M_YRKE.name();
        var request = UriBuilder.fromUri(restConfig.endpoint())
            .queryParam("fom", startDato)
            .queryParam("tom", startDato.plusYears(3))
            .queryParam("uforeTyper", uføretyperParam)
            .build();
        var rrequest = RestRequest.newGET(request, restConfig)
            .header(HEADER_FNR, fnr);
        var response = restClient.sendReturnOptional(rrequest, HarUføreGrad.class);
        return response
            .filter(r -> r.harUforegrad() != null && r.harUforegrad())
            .map(Uføreperiode::new);
    }

}
