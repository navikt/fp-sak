package no.nav.foreldrepenger.tilganger;

import java.util.Collection;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.lang3.BooleanUtils;

import no.nav.foreldrepenger.web.app.util.LdapUtil;
import no.nav.vedtak.felles.integrasjon.ldap.LdapBruker;
import no.nav.vedtak.felles.integrasjon.ldap.LdapBrukeroppslag;
import no.nav.vedtak.konfig.KonfigVerdi;
import no.nav.vedtak.sikkerhet.context.SubjectHandler;

@ApplicationScoped
public class TilgangerTjeneste {

    private String gruppenavnSaksbehandler;
    private String gruppenavnVeileder;
    private String gruppenavnBeslutter;
    private String gruppenavnOverstyrer;
    private String gruppenavnEgenAnsatt;
    private String gruppenavnKode6;
    private String gruppenavnKode7;
    private boolean skalViseDetaljerteFeilmeldinger;

    public TilgangerTjeneste() {
        //NOSONAR
    }

    @Inject
    public TilgangerTjeneste(
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

    public InnloggetNavAnsattDto innloggetBruker() {
        String ident = SubjectHandler.getSubjectHandler().getUid();
        LdapBruker ldapBruker = new LdapBrukeroppslag().hentBrukerinformasjon(ident);
        return getInnloggetBruker(ident, ldapBruker);
    }

    InnloggetNavAnsattDto getInnloggetBruker(String ident, LdapBruker ldapBruker) {
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
