package no.nav.foreldrepenger.tilganger;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


class BrukerProfilTjenesteTest {

    private static final String gruppenavnSaksbehandler = "Saksbehandler";
    private static final String gruppenavnVeileder = "Veileder";
    private static final String gruppenavnOverstyrer = "Overstyrer";
    private static final String gruppenavnOppgavestyrer = "Oppgavestyrer";
    private static final String gruppenavnKode6 = "Kode6";
    private BrukerProfilTjeneste brukerProfilTjeneste;

    @BeforeEach
    public void setUp() {
        brukerProfilTjeneste = new BrukerProfilTjeneste(gruppenavnSaksbehandler, gruppenavnVeileder, gruppenavnOverstyrer,
            gruppenavnOppgavestyrer, gruppenavnKode6);
    }

    @Test
    void skalMappeSaksbehandlerGruppeTilKanSaksbehandleRettighet() {
        var brukerUtenforSaksbehandlerGruppe = getTestBruker();
        var brukerISaksbehandlerGruppe = getTestBruker(gruppenavnSaksbehandler);

        var innloggetBrukerUtenSaksbehandlerRettighet = brukerProfilTjeneste.getInnloggetBruker(null,
                brukerUtenforSaksbehandlerGruppe);
        var innloggetBrukerMedSaksbehandlerRettighet = brukerProfilTjeneste.getInnloggetBruker(null, brukerISaksbehandlerGruppe);

        assertThat(innloggetBrukerUtenSaksbehandlerRettighet.kanSaksbehandle()).isFalse();
        assertThat(innloggetBrukerMedSaksbehandlerRettighet.kanSaksbehandle()).isTrue();
    }

    @Test
    void skalMappeVeilederGruppeTilKanVeiledeRettighet() {
        var brukerUtenforVeilederGruppe = getTestBruker();
        var brukerIVeilederGruppe = getTestBruker(gruppenavnVeileder);

        var innloggetBrukerUtenVeilederRettighet = brukerProfilTjeneste.getInnloggetBruker(null, brukerUtenforVeilederGruppe);
        var innloggetBrukerMedVeilederRettighet = brukerProfilTjeneste.getInnloggetBruker(null, brukerIVeilederGruppe);

        assertThat(innloggetBrukerUtenVeilederRettighet.kanVeilede()).isFalse();
        assertThat(innloggetBrukerMedVeilederRettighet.kanVeilede()).isTrue();
    }

    @Test
    void skalMappeOverstyrerGruppeTilKanOverstyreRettighet() {
        var brukerUtenforOverstyrerGruppe = getTestBruker();
        var brukerIOverstyrerGruppe = getTestBruker(gruppenavnOverstyrer);

        var innloggetBrukerUtenOverstyrerRettighet = brukerProfilTjeneste.getInnloggetBruker(null, brukerUtenforOverstyrerGruppe);
        var innloggetBrukerMedOverstyrerRettighet = brukerProfilTjeneste.getInnloggetBruker(null, brukerIOverstyrerGruppe);

        assertThat(innloggetBrukerUtenOverstyrerRettighet.kanOverstyre()).isFalse();
        assertThat(innloggetBrukerMedOverstyrerRettighet.kanOverstyre()).isTrue();
    }

    @Test
    void skalMappeKode6GruppeTilKanBehandleKode6Rettighet() {
        var brukerUtenforKode6Gruppe = getTestBruker();
        var brukerIKode6Gruppe = getTestBruker(gruppenavnKode6);

        var innloggetBrukerUtenKode6Rettighet = brukerProfilTjeneste.getInnloggetBruker(null, brukerUtenforKode6Gruppe);
        var innloggetBrukerMedKode6Rettighet = brukerProfilTjeneste.getInnloggetBruker(null, brukerIKode6Gruppe);

        assertThat(innloggetBrukerUtenKode6Rettighet.kanBehandleKode6()).isFalse();
        assertThat(innloggetBrukerMedKode6Rettighet.kanBehandleKode6()).isTrue();
    }

    private static LdapBruker getTestBruker(String... grupper) {
        return new LdapBruker("Testbruker", "Test Bruker", List.of(grupper));
    }

}
