package no.nav.foreldrepenger.domene.registerinnhenting.ufo;

import java.net.URI;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.UriBuilder;

import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.vedtak.felles.integrasjon.rest.StsSystemRestKlient;

/*
 * Dokumentasjon Tjeneste for å hente informasjon om brukers uførehistorikk
 * Swagger https://pensjon-pen-q2.dev.adeo.no/pen/swagger/#/Hent%20tjenester%20fra%20Sak/hentUforeHistorikk
 */

@ApplicationScoped
public class PesysUføreKlient {

    private static final Logger LOG = LoggerFactory.getLogger(PesysUføreKlient.class);

    private static final String ENDPOINT_KEY = "ufore.rs.url";
    private static final String DEFAULT_URI = "http://pensjon-pen.pensjondeployer/pen/springapi/sak/harUforegrad";

    private static final String HEADER_FNR = "fnr";

    private StsSystemRestKlient oidcRestClient;
    private URI endpoint;

    public PesysUføreKlient() {
    }

    @Inject
    public PesysUføreKlient(StsSystemRestKlient oidcRestClient,
                            @KonfigVerdi(value = ENDPOINT_KEY, defaultVerdi = DEFAULT_URI) URI endpoint) {
        this.oidcRestClient = oidcRestClient;
        this.endpoint = endpoint;
    }

    public Optional<Uføreperiode> hentUføreHistorikk(String fnr, LocalDate startDato) {
        var uføretyperParam = UforeTypeCode.UFORE.name() + "," + UforeTypeCode.UF_M_YRKE.name();
        var request = UriBuilder.fromUri(endpoint)
            .queryParam("fom", startDato)
            .queryParam("tom", startDato.plusYears(3))
            .queryParam("uforeTyper", uføretyperParam)
            .build();
        var response = this.oidcRestClient.get(request, this.lagHeader(fnr), HarUføreGrad.class);
        return Optional.ofNullable(response)
            .filter(r -> r.harUforegrad() != null && r.harUforegrad())
            .map(Uføreperiode::new);
    }

    private Set<Header> lagHeader(String fnr) {
        return Set.of(new BasicHeader(HEADER_FNR, fnr));
    }
}
