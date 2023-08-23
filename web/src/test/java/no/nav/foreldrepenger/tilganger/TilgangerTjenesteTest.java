package no.nav.foreldrepenger.tilganger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


class TilgangerTjenesteTest {

    private static final String gruppenavnSaksbehandler = "Saksbehandler";
    private static final String gruppenavnVeileder = "Veileder";
    private static final String gruppenavnBeslutter = "Beslutter";
    private static final String gruppenavnOverstyrer = "Overstyrer";
    private static final String gruppenavnOppgavestyrer = "Oppgavestyrer";
    private static final String gruppenavnEgenAnsatt = "EgenAnsatt";
    private static final String gruppenavnKode6 = "Kode6";
    private static final String gruppenavnKode7 = "Kode7";
    private static final Boolean skalViseDetaljerteFeilmeldinger = true;
    private TilgangerTjeneste tilgangerTjeneste;

    @BeforeEach
    public void setUp() {
        tilgangerTjeneste = new TilgangerTjeneste(gruppenavnSaksbehandler, gruppenavnVeileder, gruppenavnBeslutter, gruppenavnOverstyrer,
            gruppenavnOppgavestyrer, gruppenavnEgenAnsatt, gruppenavnKode6, gruppenavnKode7, skalViseDetaljerteFeilmeldinger);
    }

    @Test
    void skalMappeSaksbehandlerGruppeTilKanSaksbehandleRettighet() {
        var brukerUtenforSaksbehandlerGruppe = getTestBruker();
        var brukerISaksbehandlerGruppe = getTestBruker(gruppenavnSaksbehandler);

        var innloggetBrukerUtenSaksbehandlerRettighet = tilgangerTjeneste.getInnloggetBruker(null,
                brukerUtenforSaksbehandlerGruppe);
        var innloggetBrukerMedSaksbehandlerRettighet = tilgangerTjeneste.getInnloggetBruker(null, brukerISaksbehandlerGruppe);

        assertThat(innloggetBrukerUtenSaksbehandlerRettighet.kanSaksbehandle()).isFalse();
        assertThat(innloggetBrukerMedSaksbehandlerRettighet.kanSaksbehandle()).isTrue();
    }

    @Test
    void skalMappeVeilederGruppeTilKanVeiledeRettighet() {
        var brukerUtenforVeilederGruppe = getTestBruker();
        var brukerIVeilederGruppe = getTestBruker(gruppenavnVeileder);

        var innloggetBrukerUtenVeilederRettighet = tilgangerTjeneste.getInnloggetBruker(null, brukerUtenforVeilederGruppe);
        var innloggetBrukerMedVeilederRettighet = tilgangerTjeneste.getInnloggetBruker(null, brukerIVeilederGruppe);

        assertThat(innloggetBrukerUtenVeilederRettighet.kanVeilede()).isFalse();
        assertThat(innloggetBrukerMedVeilederRettighet.kanVeilede()).isTrue();
    }

    @Test
    void skalMappeBeslutterGruppeTilKanBeslutteRettighet() {
        var brukerUtenforBeslutterGruppe = getTestBruker();
        var brukerIBeslutterGruppe = getTestBruker(gruppenavnBeslutter);

        var innloggetBrukerUtenBeslutterRettighet = tilgangerTjeneste.getInnloggetBruker(null, brukerUtenforBeslutterGruppe);
        var innloggetBrukerMedBeslutterRettighet = tilgangerTjeneste.getInnloggetBruker(null, brukerIBeslutterGruppe);

        assertThat(innloggetBrukerUtenBeslutterRettighet.kanBeslutte()).isFalse();
        assertThat(innloggetBrukerMedBeslutterRettighet.kanBeslutte()).isTrue();
    }

    @Test
    void skalMappeOverstyrerGruppeTilKanOverstyreRettighet() {
        var brukerUtenforOverstyrerGruppe = getTestBruker();
        var brukerIOverstyrerGruppe = getTestBruker(gruppenavnOverstyrer);

        var innloggetBrukerUtenOverstyrerRettighet = tilgangerTjeneste.getInnloggetBruker(null, brukerUtenforOverstyrerGruppe);
        var innloggetBrukerMedOverstyrerRettighet = tilgangerTjeneste.getInnloggetBruker(null, brukerIOverstyrerGruppe);

        assertThat(innloggetBrukerUtenOverstyrerRettighet.kanOverstyre()).isFalse();
        assertThat(innloggetBrukerMedOverstyrerRettighet.kanOverstyre()).isTrue();
    }

    @Test
    void skalMappeEgenAnsattGruppeTilKanBehandleEgenAnsattRettighet() {
        var brukerUtenforEgenAnsattGruppe = getTestBruker();
        var brukerIEgenAnsattGruppe = getTestBruker(gruppenavnEgenAnsatt);

        var innloggetBrukerUtenEgenAnsattRettighet = tilgangerTjeneste.getInnloggetBruker(null, brukerUtenforEgenAnsattGruppe);
        var innloggetBrukerMedEgenAnsattRettighet = tilgangerTjeneste.getInnloggetBruker(null, brukerIEgenAnsattGruppe);

        assertThat(innloggetBrukerUtenEgenAnsattRettighet.kanBehandleKodeEgenAnsatt()).isFalse();
        assertThat(innloggetBrukerMedEgenAnsattRettighet.kanBehandleKodeEgenAnsatt()).isTrue();
    }

    @Test
    void skalMappeKode6GruppeTilKanBehandleKode6Rettighet() {
        var brukerUtenforKode6Gruppe = getTestBruker();
        var brukerIKode6Gruppe = getTestBruker(gruppenavnKode6);

        var innloggetBrukerUtenKode6Rettighet = tilgangerTjeneste.getInnloggetBruker(null, brukerUtenforKode6Gruppe);
        var innloggetBrukerMedKode6Rettighet = tilgangerTjeneste.getInnloggetBruker(null, brukerIKode6Gruppe);

        assertThat(innloggetBrukerUtenKode6Rettighet.kanBehandleKode6()).isFalse();
        assertThat(innloggetBrukerMedKode6Rettighet.kanBehandleKode6()).isTrue();
    }

    @Test
    void skalMappeKode7GruppeTilKanBehandleKode7Rettighet() {
        var brukerUtenforKode7Gruppe = getTestBruker();
        var brukerIKode7Gruppe = getTestBruker(gruppenavnKode7);

        var innloggetBrukerUtenKode7Rettighet = tilgangerTjeneste.getInnloggetBruker(null, brukerUtenforKode7Gruppe);
        var innloggetBrukerMedKode7Rettighet = tilgangerTjeneste.getInnloggetBruker(null, brukerIKode7Gruppe);

        assertThat(innloggetBrukerUtenKode7Rettighet.kanBehandleKode7()).isFalse();
        assertThat(innloggetBrukerMedKode7Rettighet.kanBehandleKode7()).isTrue();
    }

    private static LdapBruker getTestBruker(String... grupper) {
        return new LdapBruker("Testbruker", List.of(grupper));
    }

}
