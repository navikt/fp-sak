package no.nav.foreldrepenger.web.app.tjenester.saksbehandler;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursResourceAttributt.APPLIKASJON;

import java.util.Collection;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.BooleanUtils;

import io.swagger.v3.oas.annotations.Operation;
import no.nav.foreldrepenger.web.app.tjenester.saksbehandler.dto.InnloggetNavAnsattDto;
import no.nav.foreldrepenger.web.app.util.LdapUtil;
import no.nav.vedtak.felles.integrasjon.ldap.LdapBruker;
import no.nav.vedtak.felles.integrasjon.ldap.LdapBrukeroppslag;
import no.nav.vedtak.felles.jpa.Transaction;
import no.nav.vedtak.konfig.KonfigVerdi;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.context.SubjectHandler;

@Path("/nav-ansatt")
@ApplicationScoped
@Transaction
public class NavAnsattRestTjeneste {
    private String gruppenavnSaksbehandler;
    private String gruppenavnVeileder;
    private String gruppenavnBeslutter;
    private String gruppenavnOverstyrer;
    private String gruppenavnEgenAnsatt;
    private String gruppenavnKode6;
    private String gruppenavnKode7;
    private boolean skalViseDetaljerteFeilmeldinger;

    public NavAnsattRestTjeneste() {
        //NOSONAR
    }

    @Inject
    public NavAnsattRestTjeneste(
        @KonfigVerdi(value = "bruker.gruppenavn.saksbehandler") String gruppenavnSaksbehandler,
        @KonfigVerdi(value = "bruker.gruppenavn.veileder") String gruppenavnVeileder,
        @KonfigVerdi(value = "bruker.gruppenavn.beslutter") String gruppenavnBeslutter,
        @KonfigVerdi(value = "bruker.gruppenavn.overstyrer") String gruppenavnOverstyrer,
        @KonfigVerdi(value = "bruker.gruppenavn.egenansatt") String gruppenavnEgenAnsatt,
        @KonfigVerdi(value = "bruker.gruppenavn.kode6") String gruppenavnKode6,
        @KonfigVerdi(value = "bruker.gruppenavn.kode7") String gruppenavnKode7,
        @KonfigVerdi(value = "vise.detaljerte.feilmeldinger", defaultVerdi = "true") Boolean viseDetaljerteFeilmeldinger
    ) {
        this.gruppenavnSaksbehandler = gruppenavnSaksbehandler;
        this.gruppenavnVeileder = gruppenavnVeileder;
        this.gruppenavnBeslutter = gruppenavnBeslutter;
        this.gruppenavnOverstyrer = gruppenavnOverstyrer;
        this.gruppenavnEgenAnsatt = gruppenavnEgenAnsatt;
        this.gruppenavnKode6 = gruppenavnKode6;
        this.gruppenavnKode7 = gruppenavnKode7;
        this.skalViseDetaljerteFeilmeldinger = BooleanUtils.toBoolean(viseDetaljerteFeilmeldinger);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        description = "Returnerer fullt navn for ident",
        tags = "nav-ansatt",
        summary = ("Ident hentes fra sikkerhetskonteksten som er tilgjengelig etter innlogging.")
    )
    @BeskyttetRessurs(action = READ, ressurs = APPLIKASJON, sporingslogg = false)
    public InnloggetNavAnsattDto innloggetBruker() {
        String ident = SubjectHandler.getSubjectHandler().getUid();
        LdapBruker ldapBruker = new LdapBrukeroppslag().hentBrukerinformasjon(ident);
        return getInnloggetBrukerDto(ident, ldapBruker);
    }

    InnloggetNavAnsattDto getInnloggetBrukerDto(String ident, LdapBruker ldapBruker) {
        String navn = ldapBruker.getDisplayName();
        Collection<String> grupper = LdapUtil.filtrerGrupper(ldapBruker.getGroups());
        return InnloggetNavAnsattDto.builder()
            .setBrukernavn(ident)
            .setNavn(navn)
            .setKanSaksbehandle(grupper.contains(gruppenavnSaksbehandler))
            .setKanVeilede(grupper.contains(gruppenavnVeileder))
            .setKanBeslutte(grupper.contains(gruppenavnBeslutter))
            .setKanOverstyre(grupper.contains(gruppenavnOverstyrer))
            .setKanBehandleKodeEgenAnsatt(grupper.contains(gruppenavnEgenAnsatt))
            .setKanBehandleKode6(grupper.contains(gruppenavnKode6))
            .setKanBehandleKode7(grupper.contains(gruppenavnKode7))
            .skalViseDetaljerteFeilmeldinger(this.skalViseDetaljerteFeilmeldinger)
            .create();
    }

}
