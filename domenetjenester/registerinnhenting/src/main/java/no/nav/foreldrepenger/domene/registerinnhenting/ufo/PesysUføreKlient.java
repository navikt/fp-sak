package no.nav.foreldrepenger.domene.registerinnhenting.ufo;

import java.net.URI;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.konfig.Environment;
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
    private static final String DEFAULT_URI = "http://pensjon-pen.pensjondeployer/pen/api/sak/uforehistorikk";

    private static final Set<UforeTypeCode> UFØRE_TYPER = Set.of(UforeTypeCode.UFORE, UforeTypeCode.UF_M_YRKE);

    private static final String HEADER_FNR = "fnr";

    private static final boolean SKAL_KALLE = Environment.current().isProd() || Environment.current().isDev();


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
        if (!SKAL_KALLE) return Optional.empty();

        var response = this.oidcRestClient.get(endpoint, this.lagHeader(fnr), HentUforehistorikkResponseDto.class);

        var uføreperiode = Optional.ofNullable(response).map(HentUforehistorikkResponseDto::uforehistorikk)
            .map(UforehistorikkDto::uforeperioder).orElse(List.of()).stream()
            .filter(u -> u.uforetype() != null && UFØRE_TYPER.contains(u.uforetype().code()))
            .filter(u -> u.uforegrad() != null && u.uforegrad() > 0)
            .map(Uføreperiode::new)
            .filter(u -> u.ufgTom() == null || u.ufgTom().isAfter(startDato))
            .max(Comparator.comparing(Uføreperiode::virkningsdato));

        return uføreperiode;
    }

    private Set<Header> lagHeader(String fnr) {
        return Set.of(new BasicHeader(HEADER_FNR, fnr));
    }
}
