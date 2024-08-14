package no.nav.foreldrepenger.tilganger;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;


class BrukerProfilKlientMapperTest {

    @Test
    void skalMappeSaksbehandlerGruppeTilKanSaksbehandleRettighet() {
        var brukerUtenforSaksbehandlerGruppe = getTestBruker(false, true, false, false, false);
        var brukerISaksbehandlerGruppe = getTestSaksbehandler(false, false);

        var innloggetBrukerUtenSaksbehandlerRettighet = BrukerProfilKlient.mapTilDomene(brukerUtenforSaksbehandlerGruppe);
        var innloggetBrukerMedSaksbehandlerRettighet = BrukerProfilKlient.mapTilDomene(brukerISaksbehandlerGruppe);

        assertThat(innloggetBrukerUtenSaksbehandlerRettighet.kanSaksbehandle()).isFalse();
        assertThat(innloggetBrukerMedSaksbehandlerRettighet.kanSaksbehandle()).isTrue();
    }

    @Test
    void skalMappeVeilederGruppeTilKanVeiledeRettighet() {
        var brukerUtenforVeilederGruppe = getTestBruker(false, false, false, false, false);
        var brukerIVeilederGruppe = getTestBruker(false, true, false, false, false);

        var innloggetBrukerUtenVeilederRettighet = BrukerProfilKlient.mapTilDomene(brukerUtenforVeilederGruppe);
        var innloggetBrukerMedVeilederRettighet = BrukerProfilKlient.mapTilDomene(brukerIVeilederGruppe);

        assertThat(innloggetBrukerUtenVeilederRettighet.kanVeilede()).isFalse();
        assertThat(innloggetBrukerMedVeilederRettighet.kanVeilede()).isTrue();
    }

    @Test
    void skalMappeOverstyrerGruppeTilKanOverstyreRettighet() {
        var brukerUtenforOverstyrerGruppe = getTestSaksbehandler(false, false);
        var brukerIOverstyrerGruppe = getTestSaksbehandler(true, false);

        var innloggetBrukerUtenOverstyrerRettighet = BrukerProfilKlient.mapTilDomene(brukerUtenforOverstyrerGruppe);
        var innloggetBrukerMedOverstyrerRettighet = BrukerProfilKlient.mapTilDomene(brukerIOverstyrerGruppe);

        assertThat(innloggetBrukerUtenOverstyrerRettighet.kanOverstyre()).isFalse();
        assertThat(innloggetBrukerMedOverstyrerRettighet.kanOverstyre()).isTrue();
    }

    @Test
    void skalMappeKode6GruppeTilKanBehandleKode6Rettighet() {
        var brukerUtenforKode6Gruppe = getTestSaksbehandler(false, false);
        var brukerIKode6Gruppe = getTestSaksbehandler( false, true);

        var innloggetBrukerUtenKode6Rettighet = BrukerProfilKlient.mapTilDomene(brukerUtenforKode6Gruppe);
        var innloggetBrukerMedKode6Rettighet = BrukerProfilKlient.mapTilDomene( brukerIKode6Gruppe);

        assertThat(innloggetBrukerUtenKode6Rettighet.kanBehandleKode6()).isFalse();
        assertThat(innloggetBrukerMedKode6Rettighet.kanBehandleKode6()).isTrue();
    }

    private static BrukerProfilKlient.BrukerInfoResponseDto getTestSaksbehandler(boolean kanOverstyre, boolean kanBehandleKode6) {
        return getTestBruker(true, true, kanOverstyre, false, kanBehandleKode6);
    }

    private static BrukerProfilKlient.BrukerInfoResponseDto getTestBruker(boolean kanSaksbehandle, boolean kanveilede, boolean kanOverstyre,
                                                                          boolean kanOppgavestyre, boolean kanBehandleKode6) {
        return new BrukerProfilKlient.BrukerInfoResponseDto("T123456", "Test Bruker",
            kanSaksbehandle, kanveilede, kanOverstyre, kanOppgavestyre, kanBehandleKode6, LocalDateTime.now()
        );
    }

}
