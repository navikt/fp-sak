package no.nav.foreldrepenger.tilganger;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


class TilgangerTjenesteTest {

    private static final String gruppenavnSaksbehandler = "Saksbehandler";
    private static final String gruppenavnVeileder = "Veileder";
    private static final String gruppenavnOverstyrer = "Overstyrer";
    private static final String gruppenavnOppgavestyrer = "Oppgavestyrer";
    private static final String gruppenavnKode6 = "Kode6";
    private TilgangerTjeneste tilgangerTjeneste;

    @BeforeEach
    public void setUp() {
        tilgangerTjeneste = new TilgangerTjeneste(gruppenavnSaksbehandler, gruppenavnVeileder, gruppenavnOverstyrer,
            gruppenavnOppgavestyrer, gruppenavnKode6);
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
    void skalMappeOverstyrerGruppeTilKanOverstyreRettighet() {
        var brukerUtenforOverstyrerGruppe = getTestBruker();
        var brukerIOverstyrerGruppe = getTestBruker(gruppenavnOverstyrer);

        var innloggetBrukerUtenOverstyrerRettighet = tilgangerTjeneste.getInnloggetBruker(null, brukerUtenforOverstyrerGruppe);
        var innloggetBrukerMedOverstyrerRettighet = tilgangerTjeneste.getInnloggetBruker(null, brukerIOverstyrerGruppe);

        assertThat(innloggetBrukerUtenOverstyrerRettighet.kanOverstyre()).isFalse();
        assertThat(innloggetBrukerMedOverstyrerRettighet.kanOverstyre()).isTrue();
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

    private static LdapBruker getTestBruker(String... grupper) {
        return new LdapBruker("Testbruker", List.of(grupper));
    }

}
