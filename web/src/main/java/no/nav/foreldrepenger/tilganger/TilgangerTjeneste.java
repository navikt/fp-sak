package no.nav.foreldrepenger.tilganger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.foreldrepenger.tilganger.azure.TilgangKlient;
import no.nav.foreldrepenger.web.app.util.LdapUtil;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class TilgangerTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(TilgangerTjeneste.class);

    private String gruppenavnSaksbehandler;
    private String gruppenavnVeileder;
    private String gruppenavnBeslutter;
    private String gruppenavnOverstyrer;
    private String gruppenavnOppgavestyrer;
    private String gruppenavnEgenAnsatt;
    private String gruppenavnKode6;
    private String gruppenavnKode7;
    private String gruppenavnDrift;

    public TilgangerTjeneste() {
        // CDI
    }

    @Inject
    public TilgangerTjeneste(
        @KonfigVerdi(value = "bruker.gruppenavn.saksbehandler") String gruppenavnSaksbehandler,
        @KonfigVerdi(value = "bruker.gruppenavn.veileder") String gruppenavnVeileder,
        @KonfigVerdi(value = "bruker.gruppenavn.beslutter") String gruppenavnBeslutter,
        @KonfigVerdi(value = "bruker.gruppenavn.overstyrer") String gruppenavnOverstyrer,
        @KonfigVerdi(value = "bruker.gruppenavn.oppgavestyrer") String gruppenavnOppgavestyrer,
        @KonfigVerdi(value = "bruker.gruppenavn.egenansatt") String gruppenavnEgenAnsatt,
        @KonfigVerdi(value = "bruker.gruppenavn.kode6") String gruppenavnKode6,
        @KonfigVerdi(value = "bruker.gruppenavn.kode7") String gruppenavnKode7,
        @KonfigVerdi(value = "bruker.gruppenavn.drift") String gruppenavnDrift
    ) {
        this.gruppenavnSaksbehandler = gruppenavnSaksbehandler;
        this.gruppenavnVeileder = gruppenavnVeileder;
        this.gruppenavnBeslutter = gruppenavnBeslutter;
        this.gruppenavnOverstyrer = gruppenavnOverstyrer;
        this.gruppenavnOppgavestyrer = gruppenavnOppgavestyrer;
        this.gruppenavnEgenAnsatt = gruppenavnEgenAnsatt;
        this.gruppenavnKode6 = gruppenavnKode6;
        this.gruppenavnKode7 = gruppenavnKode7;
        this.gruppenavnDrift = gruppenavnDrift;
    }

    public InnloggetNavAnsattDto innloggetBruker() {
        var ident = KontekstHolder.getKontekst().getUid();
        var ldapBruker = new LdapBrukeroppslag().hentBrukerinformasjon(ident);
        var ldapBrukerInfo = getInnloggetBruker(ident, ldapBruker);
        sammenlignMenAzureGraphFailSoft(ldapBrukerInfo);
        return ldapBrukerInfo;
    }

    private static void sammenlignMenAzureGraphFailSoft(InnloggetNavAnsattDto ldapBrukerInfo) {
        LOG.info("TILGANGER Azure. Henter fra azure.");
        try {
            var azureBrukerInfo = new TilgangKlient().brukerInfo();
            if (!ldapBrukerInfo.equals(azureBrukerInfo)) {
                LOG.info("TILGANGER Azure. tilganger fra ldap og azure er ikke like. Azure: {} != LDAP: {}", azureBrukerInfo, ldapBrukerInfo);
            } else {
                LOG.info("TILGANGER Azure. Azure == LDAP :)");
            }
        } catch (Exception ex) {
            LOG.info("TILGANGER Azure. Klienten feilet med exception: {}", ex.getMessage());
        }
    }

    InnloggetNavAnsattDto getInnloggetBruker(String ident, LdapBruker ldapBruker) {
        var navn = ldapBruker.displayName();
        var grupper = LdapUtil.filtrerGrupper(ldapBruker.groups());
        return new InnloggetNavAnsattDto.Builder(ident, navn)
            .kanSaksbehandle(grupper.contains(gruppenavnSaksbehandler))
            .kanVeilede(grupper.contains(gruppenavnVeileder))
            .kanBeslutte(grupper.contains(gruppenavnBeslutter))
            .kanOverstyre(grupper.contains(gruppenavnOverstyrer))
            .kanOppgavestyre(grupper.contains(gruppenavnOppgavestyrer))
            .kanBehandleKodeEgenAnsatt(grupper.contains(gruppenavnEgenAnsatt))
            .kanBehandleKode6(grupper.contains(gruppenavnKode6))
            .kanBehandleKode7(grupper.contains(gruppenavnKode7))
            .kanDrifte(grupper.contains(gruppenavnDrift))
            .build();
    }

}
