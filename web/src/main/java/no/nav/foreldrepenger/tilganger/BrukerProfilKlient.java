package no.nav.foreldrepenger.tilganger;

import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;
import no.nav.vedtak.util.LRUCache;

@ApplicationScoped
public class BrukerProfilTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(BrukerProfilTjeneste.class);

    private static final long CACHE_ELEMENT_LIVE_TIME_MS = TimeUnit.MILLISECONDS.convert(24, TimeUnit.HOURS);

    private static final LRUCache <String, InnloggetNavAnsattDto> AZURE_CACHE = new LRUCache<>(1000, CACHE_ELEMENT_LIVE_TIME_MS);


    private EntraBrukerOppslag entraBrukerOppslag;

    public BrukerProfilTjeneste() {
        // CDI
    }

    @Inject
    public BrukerProfilTjeneste(EntraBrukerOppslag entraBrukerOppslag) {
        this.entraBrukerOppslag = entraBrukerOppslag;
    }

    public InnloggetNavAnsattDto innloggetBruker() {
        var ident = KontekstHolder.getKontekst().getUid();
        // Less intrusive - kall til fptilgang krever OBO-veksling på 200-400ms en gang i timen.
        if (AZURE_CACHE.get(ident) == null) {
            LOG.info("PROFIL Azure. Henter fra azure.");
            var før = System.currentTimeMillis();
            var azureBrukerInfo = mapTilDomene(entraBrukerOppslag.brukerInfo());
            LOG.info("Azure bruker profil oppslag: {}ms. ", System.currentTimeMillis() - før);
            AZURE_CACHE.put(ident, azureBrukerInfo);
        }
        return AZURE_CACHE.get(ident);
    }

    static InnloggetNavAnsattDto mapTilDomene(EntraBrukerOppslag.BrukerInfoResponseDto brukerInfo) {
        return new InnloggetNavAnsattDto.Builder(brukerInfo.brukernavn(), brukerInfo.fornavnEtternavn())
            .kanSaksbehandle(brukerInfo.kanSaksbehandle())
            .kanVeilede(brukerInfo.kanVeilede())
            .kanOverstyre(brukerInfo.kanOverstyre())
            .kanOppgavestyre(brukerInfo.kanOppgavestyre())
            .kanBehandleKode6(brukerInfo.kanBehandleKode6())
            .build();
    }

}
