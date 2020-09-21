package no.nav.foreldrepenger.tilganger;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.vedtak.felles.integrasjon.ldap.LdapBruker;

public class TilgangerTjenesteTest {

    private static final String gruppenavnSaksbehandler = "Saksbehandler";
    private static final String gruppenavnVeileder = "Veileder";
    private static final String gruppenavnBeslutter = "Beslutter";
    private static final String gruppenavnOverstyrer = "Overstyrer";
    private static final String gruppenavnEgenAnsatt = "EgenAnsatt";
    private static final String gruppenavnKode6 = "Kode6";
    private static final String gruppenavnKode7 = "Kode7";
    private static final Boolean skalViseDetaljerteFeilmeldinger = true;
    private TilgangerTjeneste tilgangerTjeneste;

    @BeforeEach
    public void setUp() {
        tilgangerTjeneste = new TilgangerTjeneste(gruppenavnSaksbehandler, gruppenavnVeileder, gruppenavnBeslutter, gruppenavnOverstyrer,
                gruppenavnEgenAnsatt, gruppenavnKode6, gruppenavnKode7, skalViseDetaljerteFeilmeldinger);
    }

    @Test
    public void skalMappeSaksbehandlerGruppeTilKanSaksbehandleRettighet() {
        LdapBruker brukerUtenforSaksbehandlerGruppe = getTestBruker();
        LdapBruker brukerISaksbehandlerGruppe = getTestBruker(gruppenavnSaksbehandler);

        InnloggetNavAnsattDto innloggetBrukerUtenSaksbehandlerRettighet = tilgangerTjeneste.getInnloggetBruker(null,
                brukerUtenforSaksbehandlerGruppe);
        InnloggetNavAnsattDto innloggetBrukerMedSaksbehandlerRettighet = tilgangerTjeneste.getInnloggetBruker(null, brukerISaksbehandlerGruppe);

        assertThat(innloggetBrukerUtenSaksbehandlerRettighet.getKanSaksbehandle()).isFalse();
        assertThat(innloggetBrukerMedSaksbehandlerRettighet.getKanSaksbehandle()).isTrue();
    }

    @Test
    public void skalMappeVeilederGruppeTilKanVeiledeRettighet() {
        LdapBruker brukerUtenforVeilederGruppe = getTestBruker();
        LdapBruker brukerIVeilederGruppe = getTestBruker(gruppenavnVeileder);

        InnloggetNavAnsattDto innloggetBrukerUtenVeilederRettighet = tilgangerTjeneste.getInnloggetBruker(null, brukerUtenforVeilederGruppe);
        InnloggetNavAnsattDto innloggetBrukerMedVeilederRettighet = tilgangerTjeneste.getInnloggetBruker(null, brukerIVeilederGruppe);

        assertThat(innloggetBrukerUtenVeilederRettighet.getKanVeilede()).isFalse();
        assertThat(innloggetBrukerMedVeilederRettighet.getKanVeilede()).isTrue();
    }

    @Test
    public void skalMappeBeslutterGruppeTilKanBeslutteRettighet() {
        LdapBruker brukerUtenforBeslutterGruppe = getTestBruker();
        LdapBruker brukerIBeslutterGruppe = getTestBruker(gruppenavnBeslutter);

        InnloggetNavAnsattDto innloggetBrukerUtenBeslutterRettighet = tilgangerTjeneste.getInnloggetBruker(null, brukerUtenforBeslutterGruppe);
        InnloggetNavAnsattDto innloggetBrukerMedBeslutterRettighet = tilgangerTjeneste.getInnloggetBruker(null, brukerIBeslutterGruppe);

        assertThat(innloggetBrukerUtenBeslutterRettighet.getKanBeslutte()).isFalse();
        assertThat(innloggetBrukerMedBeslutterRettighet.getKanBeslutte()).isTrue();
    }

    @Test
    public void skalMappeOverstyrerGruppeTilKanOverstyreRettighet() {
        LdapBruker brukerUtenforOverstyrerGruppe = getTestBruker();
        LdapBruker brukerIOverstyrerGruppe = getTestBruker(gruppenavnOverstyrer);

        InnloggetNavAnsattDto innloggetBrukerUtenOverstyrerRettighet = tilgangerTjeneste.getInnloggetBruker(null, brukerUtenforOverstyrerGruppe);
        InnloggetNavAnsattDto innloggetBrukerMedOverstyrerRettighet = tilgangerTjeneste.getInnloggetBruker(null, brukerIOverstyrerGruppe);

        assertThat(innloggetBrukerUtenOverstyrerRettighet.getKanOverstyre()).isFalse();
        assertThat(innloggetBrukerMedOverstyrerRettighet.getKanOverstyre()).isTrue();
    }

    @Test
    public void skalMappeEgenAnsattGruppeTilKanBehandleEgenAnsattRettighet() {
        LdapBruker brukerUtenforEgenAnsattGruppe = getTestBruker();
        LdapBruker brukerIEgenAnsattGruppe = getTestBruker(gruppenavnEgenAnsatt);

        InnloggetNavAnsattDto innloggetBrukerUtenEgenAnsattRettighet = tilgangerTjeneste.getInnloggetBruker(null, brukerUtenforEgenAnsattGruppe);
        InnloggetNavAnsattDto innloggetBrukerMedEgenAnsattRettighet = tilgangerTjeneste.getInnloggetBruker(null, brukerIEgenAnsattGruppe);

        assertThat(innloggetBrukerUtenEgenAnsattRettighet.getKanBehandleKodeEgenAnsatt()).isFalse();
        assertThat(innloggetBrukerMedEgenAnsattRettighet.getKanBehandleKodeEgenAnsatt()).isTrue();
    }

    @Test
    public void skalMappeKode6GruppeTilKanBehandleKode6Rettighet() {
        LdapBruker brukerUtenforKode6Gruppe = getTestBruker();
        LdapBruker brukerIKode6Gruppe = getTestBruker(gruppenavnKode6);

        InnloggetNavAnsattDto innloggetBrukerUtenKode6Rettighet = tilgangerTjeneste.getInnloggetBruker(null, brukerUtenforKode6Gruppe);
        InnloggetNavAnsattDto innloggetBrukerMedKode6Rettighet = tilgangerTjeneste.getInnloggetBruker(null, brukerIKode6Gruppe);

        assertThat(innloggetBrukerUtenKode6Rettighet.getKanBehandleKode6()).isFalse();
        assertThat(innloggetBrukerMedKode6Rettighet.getKanBehandleKode6()).isTrue();
    }

    @Test
    public void skalMappeKode7GruppeTilKanBehandleKode7Rettighet() {
        LdapBruker brukerUtenforKode7Gruppe = getTestBruker();
        LdapBruker brukerIKode7Gruppe = getTestBruker(gruppenavnKode7);

        InnloggetNavAnsattDto innloggetBrukerUtenKode7Rettighet = tilgangerTjeneste.getInnloggetBruker(null, brukerUtenforKode7Gruppe);
        InnloggetNavAnsattDto innloggetBrukerMedKode7Rettighet = tilgangerTjeneste.getInnloggetBruker(null, brukerIKode7Gruppe);

        assertThat(innloggetBrukerUtenKode7Rettighet.getKanBehandleKode7()).isFalse();
        assertThat(innloggetBrukerMedKode7Rettighet.getKanBehandleKode7()).isTrue();
    }

    private static LdapBruker getTestBruker(String... grupper) {
        return new LdapBruker("Testbruker", List.of(grupper));
    }

}
