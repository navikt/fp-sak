package no.nav.foreldrepenger.tilganger;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.foreldrepenger.web.app.util.LdapUtil;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;
import no.nav.vedtak.util.LRUCache;

@ApplicationScoped
public class BrukerProfilTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(BrukerProfilTjeneste.class);

    private static final EntraBrukerOppslag ENTRA_BRUKER_OPPSLAG = new EntraBrukerOppslag();
    private static final long CACHE_ELEMENT_LIVE_TIME_MS = TimeUnit.MILLISECONDS.convert(24, TimeUnit.HOURS);

    private static final LRUCache <String, InnloggetNavAnsattDto> AZURE_CACHE = new LRUCache<>(600, CACHE_ELEMENT_LIVE_TIME_MS);

    private String gruppenavnSaksbehandler;
    private String gruppenavnVeileder;
    private String gruppenavnOverstyrer;
    private String gruppenavnOppgavestyrer;
    private String gruppenavnKode6;

    public BrukerProfilTjeneste() {
        // CDI
    }

    @Inject
    public BrukerProfilTjeneste(
        @KonfigVerdi(value = "bruker.gruppenavn.saksbehandler") String gruppenavnSaksbehandler,
        @KonfigVerdi(value = "bruker.gruppenavn.veileder") String gruppenavnVeileder,
        @KonfigVerdi(value = "bruker.gruppenavn.overstyrer") String gruppenavnOverstyrer,
        @KonfigVerdi(value = "bruker.gruppenavn.oppgavestyrer") String gruppenavnOppgavestyrer,
        @KonfigVerdi(value = "bruker.gruppenavn.kode6") String gruppenavnKode6
    ) {
        this.gruppenavnSaksbehandler = gruppenavnSaksbehandler;
        this.gruppenavnVeileder = gruppenavnVeileder;
        this.gruppenavnOverstyrer = gruppenavnOverstyrer;
        this.gruppenavnOppgavestyrer = gruppenavnOppgavestyrer;
        this.gruppenavnKode6 = gruppenavnKode6;
    }

    public InnloggetNavAnsattDto innloggetBruker() {
        var før = System.nanoTime();
        var ident = KontekstHolder.getKontekst().getUid();
        var ldapBruker = new LdapBrukerOppslag().hentBrukerinformasjon(ident);
        var ldapBrukerInfo = getInnloggetBruker(ident, ldapBruker);
        LOG.info("LDAP bruker profil oppslag: {}ms. ", Duration.ofNanos(System.nanoTime() - før).toMillis());
        sammenlignMedAzureGraphFailSoft(ldapBrukerInfo);
        return ldapBrukerInfo;
    }

    private void sammenlignMedAzureGraphFailSoft(InnloggetNavAnsattDto ldapBrukerInfo) {
        // Less intrusive - kall til fptilgang krever OBO-veksling på 200-400ms.
        try {
            if (AZURE_CACHE.get(KontekstHolder.getKontekst().getUid()) == null) {
                LOG.info("PROFIL Azure. Henter fra azure.");
                var før = System.nanoTime();
                var azureBrukerInfo = mapTilDomene(ENTRA_BRUKER_OPPSLAG.brukerInfo());
                LOG.info("Azure bruker profil oppslag: {}ms. ", Duration.ofNanos(System.nanoTime() - før).toMillis());
                AZURE_CACHE.put(KontekstHolder.getKontekst().getUid(), azureBrukerInfo);
                if (!ldapBrukerInfo.equals(azureBrukerInfo)) {
                    LOG.info("PROFIL Azure. tilganger fra ldap og azure er ikke like. Azure: {} != LDAP: {}", azureBrukerInfo, ldapBrukerInfo);
                } else {
                    LOG.info("PROFIL Azure. Azure == LDAP :)");
                }
            }
        } catch (Exception ex) {
            LOG.info("PROFIL Azure. Klienten feilet med exception: {}", ex.getMessage());
        }
    }

    private static InnloggetNavAnsattDto mapTilDomene(EntraBrukerOppslag.BrukerInfoResponseDto brukerInfo) {
        return new InnloggetNavAnsattDto.Builder(brukerInfo.brukernavn(), brukerInfo.fornavnEtternavn())
            .kanSaksbehandle(brukerInfo.kanSaksbehandle())
            .kanVeilede(brukerInfo.kanVeilede())
            .kanOverstyre(brukerInfo.kanOverstyre())
            .kanOppgavestyre(brukerInfo.kanOppgavestyre())
            .kanBehandleKode6(brukerInfo.kanBehandleKode6())
            .build();
    }

    InnloggetNavAnsattDto getInnloggetBruker(String ident, LdapBruker ldapBruker) {
        var navn = ldapBruker.fornavnEtternavn();
        var grupper = LdapUtil.filtrerGrupper(ldapBruker.groups());
        return new InnloggetNavAnsattDto.Builder(ident, navn)
            .kanSaksbehandle(grupper.contains(gruppenavnSaksbehandler))
            .kanVeilede(grupper.contains(gruppenavnVeileder))
            .kanOverstyre(grupper.contains(gruppenavnOverstyrer))
            .kanOppgavestyre(grupper.contains(gruppenavnOppgavestyrer))
            .kanBehandleKode6(grupper.contains(gruppenavnKode6))
            .build();
    }

}
