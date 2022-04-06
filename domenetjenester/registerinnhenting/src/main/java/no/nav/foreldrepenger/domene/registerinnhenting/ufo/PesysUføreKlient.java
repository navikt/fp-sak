package no.nav.foreldrepenger.domene.registerinnhenting.ufo;

import java.net.URI;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.UriBuilder;

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
    private static final String DEFAULT_BASE_URI = "http://pensjon-pen.pensjondeployer";
    private static final String DEFAULT_PATH = "/pen/api/sak/harUforegrad";

    private static final boolean ER_PROD = Environment.current().isProd();

    private static final Set<UforeTypeCode> UFØRE_TYPER = Set.of(UforeTypeCode.UFORE, UforeTypeCode.UF_M_YRKE);

    private static final String HEADER_FNR = "fnr";

    private StsSystemRestKlient oidcRestClient;
    private URI endpoint;
    private URI pesysBaseUri;

    public PesysUføreKlient() {
    }

    @Inject
    public PesysUføreKlient(StsSystemRestKlient oidcRestClient,
                            @KonfigVerdi(value = ENDPOINT_KEY, defaultVerdi = DEFAULT_URI) URI endpoint,
                            @KonfigVerdi(value = "pesys.base.url", defaultVerdi = DEFAULT_BASE_URI) URI pesysBaseUri) {
        this.oidcRestClient = oidcRestClient;
        this.endpoint = endpoint;
        this.pesysBaseUri = pesysBaseUri;
    }

    public Optional<Uføreperiode> hentUføreHistorikk(String fnr, LocalDate startDato) {
        var response = this.oidcRestClient.get(endpoint, this.lagHeader(fnr), HentUforehistorikkResponseDto.class);
        var uføreperiode = Optional.ofNullable(response).map(HentUforehistorikkResponseDto::uforehistorikk)
            .map(UforehistorikkDto::uforeperioder).orElse(List.of()).stream()
            .filter(u -> u.uforetype() != null && UFØRE_TYPER.contains(u.uforetype().code()))
            .filter(u -> u.uforegrad() != null && u.uforegrad() > 0)
            .map(Uføreperiode::new)
            .filter(u -> u.ufgTom() == null || u.ufgTom().isAfter(startDato))
            .max(Comparator.comparing(Uføreperiode::virkningsdato));

        if (ER_PROD && uføreperiode.isPresent()) {
            try {
                var request = UriBuilder.fromUri(pesysBaseUri)
                    .queryParam("fom", startDato)
                    .queryParam("tom", startDato.plusYears(3))
                    .queryParam("uforeTyper", List.of(UforeTypeCode.UFORE, UforeTypeCode.UF_M_YRKE))
                    .path(DEFAULT_PATH)
                    .build();
                var nyResponse = this.oidcRestClient.get(request, this.lagHeader(fnr), HarUføreGrad.class);
                if (nyResponse == null) {
                    LOG.info("FPSAK PESYS ny klient svar er null");
                } else if (nyResponse.harUforegrad() == null || !nyResponse.harUforegrad()) {
                    LOG.info("FPSAK PESYS ny klient svar er non-null men harUforegrad er null/false {}", nyResponse);
                } else if (Objects.equals(nyResponse.datoUfor(), uføreperiode.get().uforetidspunkt())) {
                    if (Objects.equals(nyResponse.virkDato(), uføreperiode.get().virkningsdato())) {
                        LOG.info("FPSAK PESYS ny klient svar har lik ufore og virkdato {}", nyResponse);
                    } else {
                        LOG.info("FPSAK PESYS ny klient svar har lik ufore men ulik virkdato {}", nyResponse);
                    }
                } else {
                    LOG.info("FPSAK PESYS ny klient svar har ulik uforedato gammel {} ny {}", uføreperiode.get(), nyResponse);
                }
            } catch (Exception e) {
                LOG.info("FPSAK PESYS ny klient feil", e);
            }
        }

        return uføreperiode;
    }

    private Set<Header> lagHeader(String fnr) {
        return Set.of(new BasicHeader(HEADER_FNR, fnr));
    }
}
