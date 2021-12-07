package no.nav.foreldrepenger.domene.registerinnhenting.ufo;

import java.net.URI;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.http.Header;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.vedtak.felles.integrasjon.rest.StsAccessTokenConfig;
import no.nav.vedtak.felles.integrasjon.rest.SystemStsRestClient;

/*
 * Dokumentasjon Tjeneste for å hente informasjon om brukers uførehistorikk
 * Swagger https://pensjon-pen-q2.dev.adeo.no/pen/swagger/#/Hent%20tjenester%20fra%20Sak/hentUforeHistorikk
 */

@ApplicationScoped
public class PesysUføreKlient {

    private static final Logger LOG = LoggerFactory.getLogger(PesysUføreKlient.class);

    private static final String ENDPOINT_KEY = "pesys.ufo.rs.url";
    private static final String DEFAULT_URI = "http://pensjon-pen.pensjondeployer/pen/api/sak/uforehistorikk";

    private static final String HEADER_FNR = "fnr";

    private static final boolean ER_PROD = Environment.current().isProd();


    private SystemStsRestClient oidcRestClient;
    private URI endpoint;

    public PesysUføreKlient() {
    }

    @Inject
    public PesysUføreKlient(StsAccessTokenConfig config,
                            @KonfigVerdi(value = ENDPOINT_KEY, defaultVerdi = DEFAULT_URI) URI endpoint) {
        this.oidcRestClient = new SystemStsRestClient(config);
        this.endpoint = endpoint;
    }

    public void hentUføreHistorikk(String fnr) {
        if (!ER_PROD) return;
        try {
            var request = new URIBuilder(endpoint).build();
            var response = this.oidcRestClient.get(request, this.lagHeader(fnr), HentUforehistorikkResponseDto.class);
            LOG.info("Innhent UFO: {}", response);
        } catch (Exception e) {
            throw new IllegalArgumentException("Innhent UFO: feilet ", e);
        }
    }

    private Set<Header> lagHeader(String fnr) {
        return Set.of(new BasicHeader(HEADER_FNR, fnr));
    }
}
