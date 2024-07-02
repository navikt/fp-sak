package no.nav.foreldrepenger.tilganger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.foreldrepenger.tilganger.azure.TilgangKlient;
import no.nav.foreldrepenger.web.app.util.LdapUtil;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;

@ApplicationScoped
public class TilgangerTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(TilgangerTjeneste.class);

    private String gruppenavnSaksbehandler;
    private String gruppenavnVeileder;
    private String gruppenavnOverstyrer;
    private String gruppenavnOppgavestyrer;
    private String gruppenavnKode6;

    public TilgangerTjeneste() {
        // CDI
    }

    @Inject
    public TilgangerTjeneste(
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
        var ident = KontekstHolder.getKontekst().getUid();
        var ldapBruker = new LdapBrukeroppslag().hentBrukerinformasjon(ident);
        var ldapBrukerInfo = getInnloggetBruker(ident, ldapBruker);
        sammenlignMedAzureGraphFailSoft(ldapBrukerInfo);
        return ldapBrukerInfo;
    }

    private static void sammenlignMedAzureGraphFailSoft(InnloggetNavAnsattDto ldapBrukerInfo) {
        LOG.info("TILGANGER Azure. Henter fra azure.");
        try {
            var azureBrukerInfo = mapTilDomene(new TilgangKlient().brukerInfo());
            if (!ldapBrukerInfo.equals(azureBrukerInfo)) {
                LOG.info("TILGANGER Azure. tilganger fra ldap og azure er ikke like. Azure: {} != LDAP: {}", azureBrukerInfo, ldapBrukerInfo);
            } else {
                LOG.info("TILGANGER Azure. Azure == LDAP :)");
            }
        } catch (Exception ex) {
            LOG.info("TILGANGER Azure. Klienten feilet med exception: {}", ex.getMessage());
        }
    }

    private static InnloggetNavAnsattDto mapTilDomene(TilgangKlient.BrukerInfoResponseDto brukerInfo) {
        return new InnloggetNavAnsattDto.Builder(brukerInfo.brukernavn(), brukerInfo.navn())
            .kanSaksbehandle(brukerInfo.kanSaksbehandle())
            .kanVeilede(brukerInfo.kanVeilede())
            .kanOverstyre(brukerInfo.kanOverstyre())
            .kanOppgavestyre(brukerInfo.kanOppgavestyre())
            .kanBehandleKode6(brukerInfo.kanBehandleKode6())
            .build();
    }

    InnloggetNavAnsattDto getInnloggetBruker(String ident, LdapBruker ldapBruker) {
        var navn = ldapBruker.displayName();
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
