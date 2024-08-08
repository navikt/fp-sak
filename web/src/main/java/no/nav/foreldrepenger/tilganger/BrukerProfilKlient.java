package no.nav.foreldrepenger.tilganger;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.UriBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.vedtak.felles.integrasjon.rest.FpApplication;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;
import no.nav.vedtak.util.LRUCache;

@ApplicationScoped
@RestClientConfig(tokenConfig = TokenFlow.ADAPTIVE, application = FpApplication.FPTILGANG)
public class BrukerProfilKlient {
    private static final Logger LOG = LoggerFactory.getLogger(BrukerProfilKlient.class);

    private static final long CACHE_ELEMENT_LIVE_TIME_MS = TimeUnit.MILLISECONDS.convert(24, TimeUnit.HOURS);
    private static final LRUCache <String, InnloggetNavAnsattDto> AZURE_CACHE = new LRUCache<>(1000, CACHE_ELEMENT_LIVE_TIME_MS);


    private final RestClient restClient;
    private final RestConfig restConfig;
    private final URI uri;

    public BrukerProfilKlient() {
        this.restClient = RestClient.client();
        this.restConfig = RestConfig.forClient(this.getClass());
        this.uri = UriBuilder.fromUri(restConfig.fpContextPath()).path("/api/bruker/profil/utvidet").build();
    }

    public InnloggetNavAnsattDto innloggetBruker() {
        var ident = KontekstHolder.getKontekst().getUid();
        // Less intrusive - kall til fptilgang krever OBO-veksling på 200-400ms en gang i timen.
        if (AZURE_CACHE.get(ident) == null) {
            LOG.info("PROFIL Azure. Henter fra azure.");
            var før = System.currentTimeMillis();
            var azureBrukerInfo = mapTilDomene(brukerInfo());
            LOG.info("Azure bruker profil oppslag: {}ms. ", System.currentTimeMillis() - før);
            AZURE_CACHE.put(ident, azureBrukerInfo);
        }
        return AZURE_CACHE.get(ident);
    }

    private BrukerProfilKlient.BrukerInfoResponseDto brukerInfo() {
        var request = RestRequest.newGET(UriBuilder.fromUri(uri).build(), restConfig);
        return restClient.send(request, BrukerProfilKlient.BrukerInfoResponseDto.class);
    }

    static InnloggetNavAnsattDto mapTilDomene(BrukerInfoResponseDto brukerInfo) {
        return new InnloggetNavAnsattDto.Builder(brukerInfo.brukernavn(), brukerInfo.fornavnEtternavn())
            .kanSaksbehandle(brukerInfo.kanSaksbehandle())
            .kanVeilede(brukerInfo.kanVeilede())
            .kanOverstyre(brukerInfo.kanOverstyre())
            .kanOppgavestyre(brukerInfo.kanOppgavestyre())
            .kanBehandleKode6(brukerInfo.kanBehandleKode6())
            .build();
    }

    record BrukerInfoResponseDto(String brukernavn,
                                        String fornavnEtternavn,
                                        boolean kanSaksbehandle,
                                        boolean kanVeilede,
                                        boolean kanOverstyre,
                                        boolean kanOppgavestyre,
                                        boolean kanBehandleKode6,
                                        LocalDateTime funksjonellTid) {
    }
}
