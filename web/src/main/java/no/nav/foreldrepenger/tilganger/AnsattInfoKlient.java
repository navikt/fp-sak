package no.nav.foreldrepenger.tilganger;

import java.net.URI;
import java.util.Optional;
import java.util.Set;
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
import no.nav.vedtak.sikkerhet.kontekst.AnsattGruppe;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;
import no.nav.vedtak.util.LRUCache;

@ApplicationScoped
@RestClientConfig(tokenConfig = TokenFlow.ADAPTIVE, application = FpApplication.FPTILGANG)
public class AnsattInfoKlient {
    private static final Logger LOG = LoggerFactory.getLogger(AnsattInfoKlient.class);

    private static final long CACHE_ELEMENT_LIVE_TIME_MS = TimeUnit.MILLISECONDS.convert(24, TimeUnit.HOURS);
    private static final LRUCache <String, InnloggetNavAnsatt> AZURE_CACHE = new LRUCache<>(1000, CACHE_ELEMENT_LIVE_TIME_MS);


    private final RestClient restClient;
    private final RestConfig restConfig;
    private final URI uri;

    public AnsattInfoKlient() {
        this.restClient = RestClient.client();
        this.restConfig = RestConfig.forClient(this.getClass());
        this.uri = UriBuilder.fromUri(restConfig.fpContextPath()).path("/api/ansatt/utvidet/kontekst").build();
    }

    public InnloggetNavAnsattDto innloggetBruker() {
        var ansatt = innloggetNavAnsatt();
        return mapTilDomene(ansatt);
    }

    public InnloggetNavAnsatt innloggetNavAnsatt() {
        var ident = KontekstHolder.getKontekst().getUid();
        // Less intrusive - kall til fptilgang krever OBO-veksling på 200-400ms en gang i timen.
        if (AZURE_CACHE.get(ident) == null) {
            LOG.info("PROFIL Azure. Henter fra azure.");
            var før = System.currentTimeMillis();
            var ansattInfo = hentAnsattInfo();
            var navAnsatt = new InnloggetNavAnsatt(ansattInfo.brukernavn(), ansattInfo.navn(),
                Optional.ofNullable(ansattInfo.ansattGrupper()).orElseGet(Set::of));
            LOG.info("Azure bruker profil oppslag: {}ms. ", System.currentTimeMillis() - før);
            AZURE_CACHE.put(ident, navAnsatt);
        }
        return AZURE_CACHE.get(ident);
    }

    private AnsattInfoKlient.BrukerInfoResponseDto hentAnsattInfo() {
        var request = RestRequest.newGET(UriBuilder.fromUri(uri).build(), restConfig);
        return restClient.send(request, AnsattInfoKlient.BrukerInfoResponseDto.class);
    }

    static InnloggetNavAnsattDto mapTilDomene(InnloggetNavAnsatt ansatt) {
        var grupper = ansatt.ansattGrupper();
        return new InnloggetNavAnsattDto.Builder(ansatt.brukernavn(), ansatt.navn())
            .kanSaksbehandle(grupper.contains(AnsattGruppe.SAKSBEHANDLER))
            .kanVeilede(grupper.contains(AnsattGruppe.VEILEDER))
            .kanOverstyre(grupper.contains(AnsattGruppe.OVERSTYRER))
            .kanOppgavestyre(grupper.contains(AnsattGruppe.OPPGAVESTYRER))
            .kanBehandleKode6(grupper.contains(AnsattGruppe.STRENGTFORTROLIG))
            .build();
    }

    record BrukerInfoResponseDto(String brukernavn, String navn, Set<AnsattGruppe> ansattGrupper) { }
}
